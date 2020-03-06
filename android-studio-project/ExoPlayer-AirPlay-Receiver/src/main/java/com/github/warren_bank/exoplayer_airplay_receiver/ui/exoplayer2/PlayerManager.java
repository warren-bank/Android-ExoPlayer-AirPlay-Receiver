package com.github.warren_bank.exoplayer_airplay_receiver.ui.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.KeyEvent;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.DiscontinuityReason;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.Player.TimelineChangeReason;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;

import java.util.ArrayList;

/** Manages ExoPlayer and an internal media queue */
public final class PlayerManager implements EventListener {

  private final class MyArrayList<E> extends ArrayList<E> {
    public void retainLast() {
      int last = this.size() - 1;

      if (last > 0)
        removeRange(0, last);
    }
  }

  private PlayerView playerView;
  private MyArrayList<VideoSource> mediaQueue;
  private ConcatenatingMediaSource concatenatingMediaSource;
  private SimpleExoPlayer exoPlayer;
  private DefaultHttpDataSourceFactory httpDataSourceFactory;
  private DefaultDataSourceFactory rawDataSourceFactory;
  private int currentItemIndex;
  private Handler handler;
  private Runnable retainLast;

  public DefaultTrackSelector trackSelector;

  /**
   * @param context A {@link Context}.
   * @param playerView The {@link PlayerView} for local playback.
   */
  public static PlayerManager createPlayerManager(
      Context context,
      PlayerView playerView
    ) {
    PlayerManager playerManager = new PlayerManager(context, playerView);
    playerManager.init();
    return playerManager;
  }

  private PlayerManager(
      Context context,
      PlayerView playerView
    ) {
    this.playerView = playerView;
    this.mediaQueue = new MyArrayList<>();
    this.concatenatingMediaSource = new ConcatenatingMediaSource();
    this.trackSelector = new DefaultTrackSelector(context);
    RenderersFactory renderersFactory = new DefaultRenderersFactory(context);
    this.exoPlayer = ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector);
    this.exoPlayer.addListener(this);
    this.playerView.setKeepContentOnPlayerReset(false);
    this.playerView.setPlayer(this.exoPlayer);

    ExoPlayerEventLogger exoLogger = new ExoPlayerEventLogger(trackSelector);
    this.exoPlayer.addListener(exoLogger);

    String userAgent = context.getResources().getString(R.string.user_agent);
    this.httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
    this.rawDataSourceFactory  = new DefaultDataSourceFactory(context, userAgent);

    this.currentItemIndex = C.INDEX_UNSET;

    this.handler    = new Handler();
    this.retainLast = new Runnable() {
      @Override
      public void run() {
        // this is a callback function. executed after a new video has been appended to the queue. only used when the new video should replace all previous items in queue.
        truncateQueue();

        // probably not necessary..
        currentItemIndex = C.INDEX_UNSET;
        selectQueueItem(0);
      }
    };
  }

  // Query state of ExoPlayer.

  /**
   * @return Is the instance of ExoPlayer able to immediately play from its current position.
   */
  public boolean isPlayerReady() {
    if (exoPlayer == null)
      return false;

    int state = exoPlayer.getPlaybackState();
    return (state == Player.STATE_READY);
  }

  /**
   * @return Is the instance of ExoPlayer paused, but ready to resume playback.
   */
  public boolean isPlayerPaused() {
    return isPlayerReady()
      ? !exoPlayer.getPlayWhenReady()
      : false;
  }

  /**
   * @return The duration of the current video in milliseconds, or -1 if the duration is not known.
   */
  public long getCurrentVideoDuration() {
    if (exoPlayer == null)
      return 0l;

    long durationMs = exoPlayer.getDuration();
    if (durationMs == C.TIME_UNSET) durationMs = -1l;
    return durationMs;
  }

  /**
   * @return The position of the current video in milliseconds.
   */
  public long getCurrentVideoPosition() {
    if (exoPlayer == null)
      return 0l;

    long positionMs = exoPlayer.getCurrentPosition();
    return positionMs;
  }

  public String getCurrentVideoMimeType() {
    if (exoPlayer == null)
      return null;

    Format format = exoPlayer.getVideoFormat();
    if (format == null)
      return null;

    return format.sampleMimeType;
  }

  public String getCurrentAudioMimeType() {
    if (exoPlayer == null)
      return null;

    Format format = exoPlayer.getAudioFormat();
    if (format == null)
      return null;

    return format.sampleMimeType;
  }

  // Queue manipulation methods.

  /**
   * Plays a specified queue item in ExoPlayer.
   *
   * @param itemIndex The index of the item to play.
   */
  public void selectQueueItem(int itemIndex) {
    setCurrentItem(itemIndex, true);
  }

  /**
   * @return The index of the currently played item.
   */
  public int getCurrentItemIndex() {
    return currentItemIndex;
  }

  /**
   * Appends {@link VideoSource} to the media queue.
   *
   * @param uri The URL to a video file or stream.
   * @param caption The URL to a file containing text captions (srt or vtt).
   * @param referer The URL to include in the 'Referer' HTTP header of requests to retrieve the video file or stream.
   * @param startPosition The position at which to start playback within the video file or (non-live) stream. When value < 1.0, it is interpreted to mean a percentage of the total video length. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   */
  public void addItem(
    String uri,
    String caption,
    String referer,
    float startPosition
  ) {
    addItem(uri, caption, referer, startPosition, handler, (Runnable) null);
  }

  /**
   * Appends {@link VideoSource} to the media queue.
   *
   * @param uri The URL to a video file or stream.
   * @param caption The URL to a file containing text captions (srt or vtt).
   * @param referer The URL to include in the 'Referer' HTTP header of requests to retrieve the video file or stream.
   * @param startPosition The position at which to start playback within the video file or (non-live) stream. When value < 1.0, it is interpreted to mean a percentage of the total video length. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   * @param handler
   * @param onCompletionAction
   */
  public void addItem(
    String uri,
    String caption,
    String referer,
    float startPosition,
    Handler handler,
    Runnable onCompletionAction
  ) {
    VideoSource sample = VideoSource.createVideoSource(uri, caption, referer, startPosition);
    addItem(sample, handler, onCompletionAction);
  }

  /**
   * Appends {@code sample} to the media queue.
   *
   * @param sample The {@link VideoSource} to append.
   */
  public void addItem(VideoSource sample) {
    addItem(sample, handler, (Runnable) null);
  }

  /**
   * Appends {@code sample} to the media queue.
   *
   * @param sample The {@link VideoSource} to append.
   * @param handler
   * @param onCompletionAction
   */
  public void addItem(
    VideoSource sample,
    Handler handler,
    Runnable onCompletionAction
  ) {
    // this shouldn't happen: VideoActivity.handleIntent() has code to detect when playerManager has been released
    if ((mediaQueue == null) || (concatenatingMediaSource == null))
      return;

    // this shouldn't happen: Intents without a uri are ignored
    if (sample.uri == null) {
      if ((handler != null) && (onCompletionAction != null))
        handler.post(onCompletionAction);
      return;
    }

    boolean isEnded = (exoPlayer != null) && !exoPlayer.isPlaying() && exoPlayer.getPlayWhenReady();

    if (isEnded) {
      exoPlayer.setPlayWhenReady(false);
      exoPlayer.retry();
    }

    Runnable runCompletionAction = new Runnable() {
      @Override
      public void run() {
        if (isEnded)
          exoPlayer.setPlayWhenReady(true);

        if ((handler != null) && (onCompletionAction != null))
          handler.post(onCompletionAction);
      }
    };

    mediaQueue.add(sample);
    concatenatingMediaSource.addMediaSource(
      buildMediaSource(sample),
      handler,
      runCompletionAction
    );
  }

  /**
   * @return The size of the media queue.
   */
  public int getMediaQueueSize() {
    if (mediaQueue == null)
      return 0;

    return mediaQueue.size();
  }

  /**
   * Returns the item at the given index in the media queue.
   *
   * @param position The index of the item.
   * @return The item at the given index in the media queue.
   */
  public VideoSource getItem(int position) {
    return (getMediaQueueSize() > position)
      ? mediaQueue.get(position)
      : null;
  }

  /**
   * Removes the item at the given index from the media queue.
   *
   * @param itemIndex The index of the item to remove.
   * @return Whether the removal was successful.
   */
  public boolean removeItem(int itemIndex) {
    if ((mediaQueue == null) || (concatenatingMediaSource == null))
      return false;

    concatenatingMediaSource.removeMediaSource(itemIndex);
    mediaQueue.remove(itemIndex);
    if ((itemIndex == currentItemIndex) && (itemIndex == mediaQueue.size())) {
      maybeSetCurrentItemAndNotify(C.INDEX_UNSET);
    } else if (itemIndex < currentItemIndex) {
      maybeSetCurrentItemAndNotify(currentItemIndex - 1);
    }
    return true;
  }

  /**
   * Moves an item within the queue.
   *
   * @param fromIndex The index of the item to move.
   * @param toIndex The target index of the item in the queue.
   * @return Whether the item move was successful.
   */
  public boolean moveItem(int fromIndex, int toIndex) {
    if ((mediaQueue == null) || (concatenatingMediaSource == null))
      return false;

    // Player update.
    concatenatingMediaSource.moveMediaSource(fromIndex, toIndex);

    mediaQueue.add(toIndex, mediaQueue.remove(fromIndex));

    // Index update.
    if (fromIndex == currentItemIndex) {
      maybeSetCurrentItemAndNotify(toIndex);
    } else if (fromIndex < currentItemIndex && toIndex >= currentItemIndex) {
      maybeSetCurrentItemAndNotify(currentItemIndex - 1);
    } else if (fromIndex > currentItemIndex && toIndex <= currentItemIndex) {
      maybeSetCurrentItemAndNotify(currentItemIndex + 1);
    }

    return true;
  }

  // AirPlay functionality (exposed by HTTP endpoints)
  //   http://nto.github.io/AirPlay.html#video

  /**
   * Clears the media queue and adds the specified {@link VideoSource}.
   *
   * @param uri The URL to a video file or stream.
   * @param startPosition The position at which to start playback within the video file or (non-live) stream. When value < 1.0, it is interpreted to mean a percentage of the total video length. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   */
  public void AirPlay_play(
    String uri,
    String caption,
    String referer,
    float startPosition
  ) {
    addItem(uri, caption, referer, startPosition, handler, retainLast);
  }

  /**
   * Seek within the current video.
   *
   * @param positionSec The position as a fixed offset in seconds.
   */
  public void AirPlay_scrub(float positionSec) {
    if (exoPlayer == null) return;

    if (exoPlayer.isCurrentWindowSeekable()) {
      long positionMs = ((long) positionSec) * 1000;
      exoPlayer.seekTo(currentItemIndex, positionMs);
    }
  }

  /**
   * Change rate of speed for video playback.
   *
   * @param rate New rate of speed for video playback. The value 0.0 is equivalent to 'pause'.
   */
  public void AirPlay_rate(float rate) {
    if (exoPlayer == null) return;

    if (rate == 0f) {
      // pause playback
      if (exoPlayer.getPlayWhenReady())
        exoPlayer.setPlayWhenReady(false);
    }
    else {
      // update playback speed
      exoPlayer.setPlaybackParameters(
        new PlaybackParameters(rate)
      );

      // resume playback if paused
      if (!exoPlayer.getPlayWhenReady())
        exoPlayer.setPlayWhenReady(true);
    }
  }

  /**
   * Clears the media queue and plays a short animation.
   *
   * The animation serves two purposes.
   *  (1) it adds to the user experience.
   *  (2) it solves the problem that when the media queue
   *      is cleared during playback, a video frame
   *      is left on the surface as a still image;
   *      this would detract from the user experience.
   */
  public void AirPlay_stop() {
    addRawVideoItem(R.raw.airplay, handler, retainLast);
  }

  // extra non-standard functionality (exposed by HTTP endpoints)

  /**
   * Appends {@link VideoSource} to the media queue.
   *
   * @param uri The URL to a video file or stream.
   * @param referer The URL to include in the 'Referer' HTTP header of requests to retrieve the video file or stream.
   * @param startPosition The position at which to start playback within the video file or (non-live) stream. When value < 1.0, it is interpreted to mean a percentage of the total video length. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   */
  public void AirPlay_queue(
    String uri,
    String caption,
    String referer,
    float startPosition
  ) {
    addItem(uri, caption, referer, startPosition);
  }

  /**
   * Skip forward to the next {@link VideoSource} in the media queue.
   */
  public void AirPlay_next() {
    if (exoPlayer == null) return;

    if (exoPlayer.hasNext()) {
      exoPlayer.next();
    }
  }

  /**
   * Skip backward to the previous {@link VideoSource} in the media queue.
   */
  public void AirPlay_previous() {
    if (exoPlayer == null) return;

    if (exoPlayer.hasPrevious()) {
      exoPlayer.previous();
    }
  }

  /**
   * Change audio volume level.
   *
   * @param audioVolume New rate audio volume level. The range of acceptable values is 0.0 to 1.0. The value 0.0 is equivalent to 'mute'. The value 1.0 is unity gain.
   */
  public void AirPlay_volume(float audioVolume) {
    if (exoPlayer == null) return;

    exoPlayer.setVolume(audioVolume); // range of values: 0.0 (mute) - 1.0 (unity gain)
  }

  /**
   * Change visibility of text captions.
   *
   * @param showCaptions
   */
  public void AirPlay_captions(boolean showCaptions) {
    if (exoPlayer == null) return;

    boolean isDisabled = !showCaptions;

    DefaultTrackSelector.ParametersBuilder builder = trackSelector.getParameters().buildUpon();

    MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
    if (info == null) return;

    int renderer_count = info.getRendererCount();
    int modified_count = 0;
    for (int i = 0; i < renderer_count; i++) {
      if (exoPlayer.getRendererType(i) == C.TRACK_TYPE_TEXT) {
        builder.clearSelectionOverrides(/* rendererIndex= */ i);
        builder.setRendererDisabled(/* rendererIndex= */ i, isDisabled);
        modified_count++;
      }
    }

    if (modified_count > 0)
      trackSelector.setParameters(builder.build());
  }

  // Miscellaneous methods.

  /**
   * Dispatches a given {@link KeyEvent} to the ExoPlayer PlayerView.
   *
   * @param event The {@link KeyEvent}.
   * @return Whether the event was handled by the target view.
   */
  public boolean dispatchKeyEvent(KeyEvent event) {
    boolean isHandled = (playerView != null) && playerView.dispatchKeyEvent(event);

    if (!isHandled && (exoPlayer != null) && (event.getRepeatCount() == 0) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
      // apply custom handler(s)

      switch(event.getKeyCode()) {
        case KeyEvent.KEYCODE_SPACE :
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE :
          if (isPlayerReady()) {
            // toggle pause
            exoPlayer.setPlayWhenReady( !exoPlayer.getPlayWhenReady() );
          }
          break;
      }
    }

    return isHandled;
  }

  /**
   * Releases the manager and instance of ExoPlayer that it holds.
   */
  public void release() {
    try {
      release_exoPlayer();

      mediaQueue.clear();
      concatenatingMediaSource.clear();

      mediaQueue = null;
      concatenatingMediaSource = null;
      httpDataSourceFactory = null;
      rawDataSourceFactory  = null;
      currentItemIndex = C.INDEX_UNSET;
    }
    catch (Exception e){}
  }

  /**
   * Releases the instance of ExoPlayer.
   */
  private void release_exoPlayer() {
    if (playerView != null) {
      try {
        playerView.setPlayer(null);
      }
      catch (Exception e) {}
      finally {
        playerView = null;
      }
    }

    if (exoPlayer != null) {
      try {
        exoPlayer.stop(true);
        exoPlayer.removeListener(this);
        exoPlayer.release();
      }
      catch (Exception e) {}
      finally {
        exoPlayer = null;
      }
    }
  }

  // Player.EventListener implementation.

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    updateCurrentItemIndex();
  }

  @Override
  public void onPositionDiscontinuity(@DiscontinuityReason int reason) {
    updateCurrentItemIndex();
  }

  @Override
  public void onTimelineChanged(
      Timeline timeline, @Nullable Object manifest, @TimelineChangeReason int reason
  ){
    updateCurrentItemIndex();
  }

  @Override
  public void onPlayerError(ExoPlaybackException error) {
    if (exoPlayer == null) return;

    if (error.type == ExoPlaybackException.TYPE_SOURCE) {
      exoPlayer.next();
    }
  }

  // Internal methods.

  private void init() {
    if (exoPlayer == null) return;

    // Media queue management.
    exoPlayer.prepare(concatenatingMediaSource);
  }

  private void truncateQueue() {
    if ((mediaQueue == null) || (concatenatingMediaSource == null))
      return;

    mediaQueue.retainLast();

    int last = concatenatingMediaSource.getSize() - 1;
    if (last > 0)
      concatenatingMediaSource.removeMediaSourceRange(0, last);
  }

  private void updateCurrentItemIndex() {
    if (exoPlayer == null) return;

    int playbackState = exoPlayer.getPlaybackState();

    int currentItemIndex = ((playbackState != Player.STATE_IDLE) && (playbackState != Player.STATE_ENDED))
      ? exoPlayer.getCurrentWindowIndex()
      : C.INDEX_UNSET;

    maybeSetCurrentItemAndNotify(currentItemIndex);
  }

  /**
   * Starts playback of the item at the given position.
   *
   * @param itemIndex The index of the item to play.
   * @param playWhenReady Whether ExoPlayer should begin playback when item is ready.
   */
  private void setCurrentItem(int itemIndex, boolean playWhenReady) {
    if (exoPlayer == null) return;

    maybeSetCurrentItemAndNotify(itemIndex);
    exoPlayer.setPlayWhenReady(playWhenReady);
  }

  private void maybeSetCurrentItemAndNotify(int currentItemIndex) {
    if (this.currentItemIndex != currentItemIndex) {
      this.currentItemIndex = currentItemIndex;

      if (currentItemIndex != C.INDEX_UNSET) {
        seekToStartPosition(currentItemIndex);
        setHttpRequestHeaders(currentItemIndex);
      }
    }
  }

  private void seekToStartPosition(int currentItemIndex) {
    if (exoPlayer == null) return;

    VideoSource sample = getItem(currentItemIndex);
    if (sample == null) return;

    float position = sample.startPosition;

    if ((position > 0f) && (position < 1f)) {
      // percentage
      long duration = exoPlayer.getDuration(); // ms
      if (duration != C.TIME_UNSET) {
        long positionMs = (long) (duration * position);
        exoPlayer.seekTo(currentItemIndex, positionMs);
      }
    }
    else if (position >= 1f) {
      // fixed offset in seconds
      long positionMs = ((long) position) * 1000;
      exoPlayer.seekTo(currentItemIndex, positionMs);
    }
    else {
      exoPlayer.seekToDefaultPosition(currentItemIndex);
    }
  }

  private void setHttpRequestHeaders(int currentItemIndex) {
    VideoSource sample = getItem(currentItemIndex);
    if (sample == null) return;

    if (sample.referer != null) {
      Uri referer   = Uri.parse(sample.referer);
      String origin = referer.getScheme() + "://" + referer.getAuthority();

      setHttpRequestHeader("origin",  origin);
      setHttpRequestHeader("referer", sample.referer);
    }
  }

  private void setHttpRequestHeader(String name, String value) {
    if (httpDataSourceFactory == null) return;

    httpDataSourceFactory.getDefaultRequestProperties().set(name, value);
  }

  private MediaSource buildMediaSource(VideoSource sample) {
    MediaSource video   = buildUriMediaSource(sample);
    MediaSource caption = buildCaptionMediaSource(sample);

    return (caption == null)
      ? video
      : new MergingMediaSource(video, caption);
  }

  private MediaSource buildUriMediaSource(VideoSource sample) {
    if (httpDataSourceFactory == null)
      return null;

    Uri uri = Uri.parse(sample.uri);

    switch (sample.uri_mimeType) {
      case MimeTypes.APPLICATION_M3U8:
        return new HlsMediaSource.Factory(httpDataSourceFactory).createMediaSource(uri);
      case MimeTypes.APPLICATION_MPD:
        return new DashMediaSource.Factory(httpDataSourceFactory).createMediaSource(uri);
      case MimeTypes.APPLICATION_SS:
        return new SsMediaSource.Factory(httpDataSourceFactory).createMediaSource(uri);
      default:
        return new ExtractorMediaSource.Factory(httpDataSourceFactory).createMediaSource(uri);
    }
  }

  private MediaSource buildCaptionMediaSource(VideoSource sample) {
    if (httpDataSourceFactory == null)
      return null;

    if ((sample.caption == null) || (sample.caption_mimeType == null))
      return null;

    Uri uri       = Uri.parse(sample.caption);
    Format format = Format.createTextSampleFormat(/* id= */ null, sample.caption_mimeType, /* selectionFlags= */ C.SELECTION_FLAG_DEFAULT, /* language= */ "en");

    return new SingleSampleMediaSource.Factory(httpDataSourceFactory).createMediaSource(uri, format, C.TIME_UNSET);
  }

  private MediaSource buildRawVideoMediaSource(int rawResourceId) {
    if (rawDataSourceFactory == null)
      return null;

    Uri uri = RawResourceDataSource.buildRawResourceUri(rawResourceId);

    return new ExtractorMediaSource.Factory(rawDataSourceFactory).createMediaSource(uri);
  }

  private void addRawVideoItem(int rawResourceId) {
    addRawVideoItem(rawResourceId, handler, (Runnable) null);
  }

  private void addRawVideoItem(
    int rawResourceId,
    Handler handler,
    Runnable onCompletionAction
  ) {
    if ((mediaQueue == null) || (concatenatingMediaSource == null))
      return;

    VideoSource sample = VideoSource.createVideoSource("raw", /* caption= */ (String) null, /* referer= */ (String) null, /* startPosition= */ 0f);

    boolean isEnded = (exoPlayer != null) && !exoPlayer.isPlaying() && exoPlayer.getPlayWhenReady();

    if (isEnded) {
      exoPlayer.setPlayWhenReady(false);
      exoPlayer.retry();
    }

    Runnable runCompletionAction = new Runnable() {
      @Override
      public void run() {
        if (isEnded)
          exoPlayer.setPlayWhenReady(true);

        if ((handler != null) && (onCompletionAction != null))
          handler.post(onCompletionAction);
      }
    };

    mediaQueue.add(sample);
    concatenatingMediaSource.addMediaSource(
      buildRawVideoMediaSource(rawResourceId),
      handler,
      runCompletionAction
    );
  }

}
