package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.MyLoadErrorHandlingPolicy;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.MyRenderersFactory;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.TextSynchronizer;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExoPlayerUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaSourceUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ResourceUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.SystemUtils;

import android.content.Context;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.DiscontinuityReason;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.Player.TimelineChangeReason;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsCollector;
import com.google.android.exoplayer2.audio.AudioListener;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.ArrayList;
import java.util.Arrays;

/** Manages ExoPlayer and an internal media queue */
public final class PlayerManager implements EventListener {

  private static final String TAG = "PlayerManager";

  private final class MyArrayList<E> extends ArrayList<E> {
    public void retainLast(int count) {
      int last = this.size() - count;

      if (last > 0)
        removeRange(0, last);
    }
  }

  private PlayerView playerView;
  private MyArrayList<VideoSource> mediaQueue;
  private ConcatenatingMediaSource concatenatingMediaSource;
  private SimpleExoPlayer exoPlayer;
  private float audioVolume;
  private int   audioVolumeMaxDbBoost;
  private AudioListener audioListener;
  private LoudnessEnhancer loudnessEnhancer;
  private DefaultHttpDataSourceFactory httpDataSourceFactory;
  private DefaultDataSourceFactory rawDataSourceFactory;
  private MyLoadErrorHandlingPolicy loadErrorHandlingPolicy;
  private int currentItemIndex;
  private Handler handler;

  public DefaultTrackSelector trackSelector;
  public TextSynchronizer textSynchronizer;

  /**
   * @param context A {@link Context}.
   */
  public static PlayerManager createPlayerManager(Context context) {
    PlayerManager playerManager = new PlayerManager(context);
    playerManager.init();
    return playerManager;
  }

  private PlayerManager(Context context) {
    this.playerView = null;
    this.mediaQueue = new MyArrayList<>();
    this.concatenatingMediaSource = new ConcatenatingMediaSource();
    this.trackSelector = new DefaultTrackSelector(context);
    MyRenderersFactory renderersFactory = new MyRenderersFactory(context);
    this.textSynchronizer = (TextSynchronizer) renderersFactory;
    DefaultLoadControl loadControl = getLoadControl(context);
    EventLogger exoLogger = new EventLogger(trackSelector);
    AnalyticsCollector analyticsCollector = new AnalyticsCollector(Clock.DEFAULT);
    analyticsCollector.addListener(exoLogger);
    this.exoPlayer = new SimpleExoPlayer.Builder(
      context,
      (RenderersFactory) renderersFactory,
      trackSelector,
      new DefaultMediaSourceFactory(context, new DefaultExtractorsFactory()),
      loadControl,
      DefaultBandwidthMeter.getSingletonInstance(context),
      analyticsCollector
    ).build();
    this.exoPlayer.addListener(this);
    this.exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);

    this.audioVolume           = 1.0f;
    this.audioVolumeMaxDbBoost = ResourceUtils.getInteger(context, R.integer.AUDIO_VOLUME_MAX_DB_BOOST);

    if (Build.VERSION.SDK_INT >= 19) {
      this.audioListener = new AudioListener() {
        @Override
        public void onAudioSessionIdChanged(int audioSessionId) {
          if (loudnessEnhancer != null) {
            loudnessEnhancer.setEnabled(false);
            loudnessEnhancer.release();
            loudnessEnhancer = null;
          }

          if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            try {
              loudnessEnhancer = new LoudnessEnhancer(audioSessionId);

              AirPlay_volume(audioVolume);
            }
            catch (Exception e) {}
          }
        }
      };

      this.exoPlayer.addAudioListener(this.audioListener);

      this.audioListener.onAudioSessionIdChanged( this.exoPlayer.getAudioSessionId() );
    }

    String userAgent = context.getResources().getString(R.string.user_agent);
    this.httpDataSourceFactory   = new DefaultHttpDataSourceFactory(userAgent);
    this.rawDataSourceFactory    = new DefaultDataSourceFactory(context, userAgent);
    this.loadErrorHandlingPolicy = new MyLoadErrorHandlingPolicy();

    this.currentItemIndex = C.INDEX_UNSET;
    this.handler = new Handler(Looper.getMainLooper());
  }

  private DefaultLoadControl getLoadControl(Context context) {
    long thresholdMemorySizeInBytes = 1610612736l; // 1.5 GiB
    long memorySizeInBytes          = SystemUtils.getMemorySizeInBytes(context);
    float factor                    = (memorySizeInBytes <= thresholdMemorySizeInBytes) ? 1.0f : 1.5f;

    Log.d(TAG, "memory=" + (float)(((int)((memorySizeInBytes*100)/(1024*1024*1024)))/100f) + "GB, buffer factor=" + (int)(factor*100) + "%");

    DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();

    if (Math.abs(factor - 1.0f) > 0.00001f) { // if (factor != 1.0f)
      builder
        .setBufferDurationsMs(
          /* int minBufferMs= minBufferAudioMs= minBufferVideoMs= */ (int) (factor * DefaultLoadControl.DEFAULT_MIN_BUFFER_MS),
          /* int maxBufferMs=                                     */ (int) (factor * DefaultLoadControl.DEFAULT_MAX_BUFFER_MS),
          /* int bufferForPlaybackMs=                             */ (int) (factor * DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS),
          /* int bufferForPlaybackAfterRebufferMs=                */ (int) (factor * DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
        )
        .setTargetBufferBytes(
          /* int targetBufferBytes=                               */ (int) (factor * DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES)
        )
      ;
    }

    return builder.build();
  }

  /**
   * @param newPlayerView A {@link PlayerView}.
   *
   * Attach or detach Player to a video surface.
   */
  public void setPlayerView(@Nullable PlayerView newPlayerView) {
    if (playerView != null) {
      playerView.setPlayer(null);
    }

    playerView = newPlayerView;

    if (playerView != null) {
      playerView.setPlayer(exoPlayer);
    }
  }

  /**
   * @param holder Instance of any class that implements the {@link SetPlayer} Interface.
   *
   * Attach or detach Player to the specified class.
   */
  public void setPlayer(SetPlayer holder) {
    holder.setPlayer(exoPlayer);
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

  public void addItem(
    String uri,
    String caption,
    String referer,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server
  ) {
    addItem(uri, caption, referer, startPosition, stopPosition, drm_scheme, drm_license_server, /* remove_previous_items= */ false);
  }

  /**
   * Appends {@link VideoSource} to the media queue.
   *
   * @param uri                   The URL to a video file or stream.
   * @param caption               The URL to a file containing text captions (srt or vtt).
   * @param referer               The URL to include in the 'Referer' HTTP header of requests to retrieve the video file or stream.
   * @param startPosition         The position at which to start playback within the video file or (non-live) stream. When value < 1.0 and stopPosition < value, it is interpreted to mean a percentage of the total video length. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   * @param stopPosition          The position at which to stop playback within the video file or (non-live) stream. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   * @param drm_scheme            The DRM scheme; value in: ["widevine","playready","clearkey"]
   * @param drm_license_server    The URL to obtain DRM license keys.
   * @param remove_previous_items A boolean flag to indicate whether all previous items in queue should be removed after new items have been appended.
   */
  public void addItem(
    String uri,
    String caption,
    String referer,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    boolean remove_previous_items
  ) {
    VideoSource sample = VideoSource.createVideoSource(uri, caption, referer, startPosition, stopPosition, drm_scheme, drm_license_server);
    addItem(sample, remove_previous_items);
  }

  public void addItem(VideoSource sample) {
    addItem(sample, /* remove_previous_items= */ false);
  }

  /**
   * Appends {@code sample} to the media queue.
   *
   * @param sample The {@link VideoSource} to append.
   * @param remove_previous_items A boolean flag to indicate whether all previous items in queue should be removed after new items have been appended.
   */
  public void addItem(
    VideoSource sample,
    boolean remove_previous_items
  ) {
    MediaSource mediaSource = buildMediaSource(sample);
    addItem(sample, mediaSource, remove_previous_items);
  }

  private void addItem(
    VideoSource sample,
    MediaSource mediaSource,
    boolean remove_previous_items
  ) {
    VideoSource[] samples      = new VideoSource[]{sample};
    MediaSource[] mediaSources = new MediaSource[]{mediaSource};
    addItems(samples, mediaSources, remove_previous_items);
  }

  public void addItems(
    String[] uris,
    String caption,
    String referer,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server
  ) {
    addItems(uris, caption, referer, startPosition, stopPosition, drm_scheme, drm_license_server, /* remove_previous_items= */ false);
  }

  /**
   * Appends {@link VideoSource} to the media queue.
   *
   * @param uris                  Array of URLs to video files or streams.
   * @param caption               The URL to a file containing text captions (srt or vtt).
   * @param referer               The URL to include in the 'Referer' HTTP header of requests to retrieve the video file or stream.
   * @param startPosition         The position at which to start playback within the video file or (non-live) stream. When value < 1.0 and stopPosition < value, it is interpreted to mean a percentage of the total video length. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   * @param stopPosition          The position at which to stop playback within the video file or (non-live) stream. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   * @param drm_scheme            The DRM scheme; value in: ["widevine","playready","clearkey"]
   * @param drm_license_server    The URL to obtain DRM license keys.
   * @param remove_previous_items A boolean flag to indicate whether all previous items in queue should be removed after new items have been appended.
   */
  public void addItems(
    String[] uris,
    String caption,
    String referer,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    boolean remove_previous_items
  ) {
    VideoSource[] samples = new VideoSource[uris.length];
    String uri;
    VideoSource sample;

    for (int i=0; i < uris.length; i++) {
      uri        = uris[i];
      sample     = (i == 0)
                     ? VideoSource.createVideoSource(uri, caption, referer, startPosition, stopPosition, drm_scheme, drm_license_server)
                     : VideoSource.createVideoSource(uri, null,    referer, -1f,           -1f,          drm_scheme, drm_license_server)
                   ;
      samples[i] = sample;
    }

    addItems(samples, remove_previous_items);
  }

  public void addItems(VideoSource[] samples) {
    addItems(samples, /* remove_previous_items= */ false);
  }

  /**
   * Appends {@code samples} to the media queue.
   *
   * @param samples Array of {@link VideoSource} to append.
   * @param remove_previous_items A boolean flag to indicate whether all previous items in queue should be removed after new items have been appended.
   */
  public void addItems(
    VideoSource[] samples,
    boolean remove_previous_items
  ) {
    MediaSource[] mediaSources = new MediaSource[samples.length];
    VideoSource sample;
    MediaSource mediaSource;

    for (int i=0; i < samples.length; i++) {
      sample          = samples[i];
      mediaSource     = buildMediaSource(sample);
      mediaSources[i] = mediaSource;
    }

    addItems(samples, mediaSources, remove_previous_items);
  }

  private void addItems(
    VideoSource[] samples,
    MediaSource[] mediaSources,
    boolean remove_previous_items
  ) {
    if ((mediaQueue == null) || (concatenatingMediaSource == null))
      return;
    if ((samples == null) || (mediaSources == null))
      return;
    if (samples.length != mediaSources.length)
      return;

    boolean isEnded = (exoPlayer != null) && !exoPlayer.isPlaying() && exoPlayer.getPlayWhenReady();

    if (isEnded || remove_previous_items) {
      truncateQueue(0);
      currentItemIndex = C.INDEX_UNSET;
      exoPlayer.setPlayWhenReady(false);
      exoPlayer.retry();
    }

    mediaQueue.addAll(
      Arrays.asList(samples)
    );

    if (isEnded || remove_previous_items) {
      Runnable runCompletionAction = new Runnable() {
        @Override
        public void run() {
          selectQueueItem(0);
        }
      };

      concatenatingMediaSource.addMediaSources(
        Arrays.asList(mediaSources),
        handler,
        runCompletionAction
      );
    }
    else {
      concatenatingMediaSource.addMediaSources(
        Arrays.asList(mediaSources)
      );
    }
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
    return ((position >= 0) && (getMediaQueueSize() > position))
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
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server
  ) {
    addItem(uri, caption, referer, startPosition, stopPosition, drm_scheme, drm_license_server, /* remove_previous_items= */ true);
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
   * Seek within the current video by a relative offset.
   *
   * @param offsetMs The position as a relative offset in milliseconds.
   */
  public void AirPlay_add_scrub_offset(long offsetMs) {
    if (exoPlayer == null) return;

    if (exoPlayer.isCurrentWindowSeekable()) {
      long positionMs = exoPlayer.getCurrentPosition();
      exoPlayer.seekTo(currentItemIndex, positionMs + offsetMs);
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

  public void AirPlay_stop() {
    AirPlay_stop(/* play_animation= */ VideoActivity.isVisible);
  }

  /**
   * Clears the media queue and (optionally) plays a short animation.
   *
   * The animation serves two purposes.
   *  (1) it adds to the user experience.
   *  (2) it solves the problem that when the media queue
   *      is cleared during playback, a video frame
   *      is left on the surface as a still image;
   *      this would detract from the user experience.
   */
  public void AirPlay_stop(boolean play_animation) {
    if (play_animation) {
      addRawVideoItem(R.raw.airplay, /* remove_previous_items= */ true);
    }
    else {
      AirPlay_rate(0f);
      truncateQueue(0);
    }
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
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server
  ) {
    addItem(uri, caption, referer, startPosition, stopPosition, drm_scheme, drm_license_server);
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
   * @param audioVolume New audio volume level.
   *                    The range of acceptable values is 0.0 to (AUDIO_VOLUME_MAX_DB_BOOST + 1.0).
   *                    The value 0.0 is   0% input volume (equivalent to 'mute').
   *                    The value 1.0 is 100% input volume (unity gain).
   *                    The value 6.5 is 100% input volume and amplified by 5.5 dB.
   */
  public void AirPlay_volume(float audioVolume) {
    if (audioVolume < 0.0f) {
      audioVolume = 0.0f;
    }
    if (audioVolume > (audioVolumeMaxDbBoost + 1.0f)) {
      audioVolume = (float) audioVolumeMaxDbBoost + 1.0f;
    }

    this.audioVolume = audioVolume;

    if (exoPlayer == null) return;

    if (audioVolume <= 1.0f) {
      exoPlayer.setVolume(audioVolume); // range of values: 0.0 (mute) - 1.0 (unity gain)

      if (loudnessEnhancer != null) {
        try {
          loudnessEnhancer.setTargetGain(0);
          loudnessEnhancer.setEnabled(false);
        }
        catch (Exception e) {}
      }
    }
    else {
      exoPlayer.setVolume(1.0f);

      if (loudnessEnhancer != null) {
        try {
          float gain_dB = audioVolume - 1.0f;
          int   gain_mB = (int) (gain_dB * 100.0f);

          loudnessEnhancer.setTargetGain(gain_mB);
          loudnessEnhancer.setEnabled(true);
        }
        catch (Exception e) {}
      }
    }
  }

  /**
   * Change visibility of text captions.
   *
   * @param showCaptions
   */
  public void AirPlay_show_captions(boolean showCaptions) {
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

  /**
   * Set the time offset for text captions.
   *
   * @param offset Measured in microseconds
   */
  public void AirPlay_set_captions_offset(long offset) {
    textSynchronizer.setTextOffset(offset);
  }

  /**
   * Add to the current time offset for text captions.
   *
   * @param offset Measured in microseconds
   */
  public void AirPlay_add_captions_offset(long offset) {
    textSynchronizer.addTextOffset(offset);
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
            isHandled = true;
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

      mediaQueue               = null;
      concatenatingMediaSource = null;
      httpDataSourceFactory    = null;
      rawDataSourceFactory     = null;
      loadErrorHandlingPolicy  = null;
      currentItemIndex         = C.INDEX_UNSET;
    }
    catch (Exception e){}
  }

  /**
   * Releases the instance of ExoPlayer.
   */
  private void release_exoPlayer() {
    setPlayerView(null);

    if (exoPlayer != null) {
      try {
        exoPlayer.stop(true);

        if (audioListener != null) {
          exoPlayer.removeAudioListener(audioListener);
          audioListener = null;
        }

        exoPlayer.removeListener(this);
        exoPlayer.release();
      }
      catch (Exception e) {}
      finally {
        exoPlayer = null;
      }
    }

    if (loudnessEnhancer != null) {
      loudnessEnhancer.setEnabled(false);
      loudnessEnhancer.release();
      loudnessEnhancer = null;
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
    if (exoPlayer == null)
      return;
    if (error.type != ExoPlaybackException.TYPE_SOURCE)
      return;

    if (ExoPlayerUtils.isBehindLiveWindow(error) || ExoPlayerUtils.isHttpDataSource(error))
      retry();
    else
      exoPlayer.next();
  }

  // Internal methods.

  private void init() {
    if (exoPlayer == null) return;

    // Media queue management.
    exoPlayer.prepare(concatenatingMediaSource);
  }

  private void retry() {
    if (exoPlayer == null) return;

    exoPlayer.retry();
    exoPlayer.seekToDefaultPosition();
  }

  private void truncateQueue(int count) {
    if ((mediaQueue == null) || (concatenatingMediaSource == null))
      return;

    mediaQueue.retainLast(count);

    int last = concatenatingMediaSource.getSize() - count;
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
    if (ExternalStorageUtils.isFileUri(sample.uri)) return;

    if (sample.referer != null) {
      Uri referer   = Uri.parse(sample.referer);
      String origin = referer.getScheme() + "://" + referer.getAuthority();

      setHttpRequestHeader("origin",  origin);
      setHttpRequestHeader("referer", sample.referer);
    }
    else {
      setHttpRequestHeader("origin",  null);
      setHttpRequestHeader("referer", null);
    }

    switch (sample.uri_mimeType) {
      case "video/mp4":
      case "video/mpeg":
      case "video/x-mkv":
      case "video/x-msvideo":
        setHttpRequestHeader("range", "bytes=0-");
        break;
      default:
        setHttpRequestHeader("range", null);
    }
  }

  private void setHttpRequestHeader(String name, String value) {
    if (httpDataSourceFactory == null) return;

    if (value == null)
      httpDataSourceFactory.getDefaultRequestProperties().remove(name);
    else
      httpDataSourceFactory.getDefaultRequestProperties().set(name, value);
  }

  private MediaSource buildMediaSource(VideoSource sample) {
    MediaSource            video    = buildUriMediaSource(sample);
    ArrayList<MediaSource> captions = buildCaptionMediaSources(sample);
    MediaSource[] mediaSources;

    if ((captions == null) || captions.isEmpty()) {
      mediaSources = new MediaSource[1];
      mediaSources[0] = video;
    }
    else {
      // prepend
      captions.add(0, video);

      mediaSources = new MediaSource[captions.size()];
      mediaSources = captions.toArray(mediaSources);
    }

    MediaSource mergedMediaSource = (mediaSources.length == 1)
      ? mediaSources[0]
      : new MergingMediaSource(mediaSources);

    MediaSource clippedMediaSource = applyClippingProperties(mergedMediaSource, sample);

    return clippedMediaSource;
  }

  private MediaSource buildUriMediaSource(VideoSource sample) {
    DataSource.Factory factory = ExternalStorageUtils.isFileUri(sample.uri)
      ? rawDataSourceFactory
      : httpDataSourceFactory;

    if (factory == null)
      return null;

    MediaItem mediaItem = sample.getMediaItem();

    switch (sample.uri_mimeType) {
      case MimeTypes.APPLICATION_M3U8:
        return new HlsMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem);
      case MimeTypes.APPLICATION_MPD:
        return new DashMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem);
      case MimeTypes.APPLICATION_SS:
        return new SsMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem);
      case MimeTypes.APPLICATION_RTSP:
        return new RtspMediaSource.Factory().createMediaSource(mediaItem);
      default:
        return new ProgressiveMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem);
    }
  }

  private ArrayList<MediaSource> buildCaptionMediaSources(VideoSource sample) {
    ArrayList<MediaSource> captions = new ArrayList<MediaSource>();
    DataSource.Factory factory;
    Uri uri;
    MediaItem.Subtitle mediaItem;

    if (!TextUtils.isEmpty(sample.caption) && !TextUtils.isEmpty(sample.caption_mimeType)) {
      factory = ExternalStorageUtils.isFileUri(sample.caption)
        ? rawDataSourceFactory
        : httpDataSourceFactory;

      if (factory == null)
        return null;

      uri       = Uri.parse(sample.caption);
      mediaItem = new MediaItem.Subtitle(uri, sample.caption_mimeType, /* language= */ null, /* selectionFlags= */ C.SELECTION_FLAG_DEFAULT);

      captions.add(
        new SingleSampleMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem, C.TIME_UNSET)
      );
    }
    else if (ExternalStorageUtils.isFileUri(sample.uri)) {
      // loading media from external storage without any captions file explicitly specified.
      // search within same directory as media file for external captions in a supported format.
      // file naming convention: "${video_filename}.*.${supported_caption_extension}"

      factory = rawDataSourceFactory;

      if (factory == null)
        return null;

      ArrayList<String> uriCaptions = ExternalStorageUtils.findMatchingSubtitles(sample.uri);

      if ((uriCaptions == null) || uriCaptions.isEmpty())
        return null;

      for (String caption : uriCaptions) {
        uri       = Uri.parse(caption);
        mediaItem = new MediaItem.Subtitle(uri, MediaTypeUtils.get_caption_mimeType(caption), /* language= */ null, /* selectionFlags= */ C.SELECTION_FLAG_DEFAULT);

        captions.add(
          new SingleSampleMediaSource.Factory(factory).createMediaSource(mediaItem, C.TIME_UNSET)
        );
      }
    }

    // normalize that non-null return value must include matches
    if ((captions == null) || captions.isEmpty())
      captions = null;

    return captions;
  }

  private MediaSource applyClippingProperties(MediaSource mediaSource, VideoSource sample) {
    MediaItem mediaItem = sample.getMediaItem();

    return MediaSourceUtils.applyClippingProperties(mediaSource, mediaItem);
  }

  private MediaSource buildRawVideoMediaSource(int rawResourceId) {
    if (rawDataSourceFactory == null)
      return null;

    Uri uri = RawResourceDataSource.buildRawResourceUri(rawResourceId);
    MediaItem mediaItem = MediaItem.fromUri(uri);

    return new ProgressiveMediaSource.Factory(rawDataSourceFactory).createMediaSource(mediaItem);
  }

  private void addRawVideoItem(int rawResourceId) {
    addRawVideoItem(rawResourceId, /* remove_previous_items= */ false);
  }

  private void addRawVideoItem(
    int rawResourceId,
    boolean remove_previous_items
  ) {
    VideoSource sample      = VideoSource.createVideoSource();
    MediaSource mediaSource = buildRawVideoMediaSource(rawResourceId);
    addItem(sample, mediaSource, remove_previous_items);
  }

}
