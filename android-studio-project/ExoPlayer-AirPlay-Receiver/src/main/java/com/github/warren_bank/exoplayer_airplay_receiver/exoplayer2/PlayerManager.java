package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.MyLoadErrorHandlingPolicy;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.MyRenderersFactory;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.TextSynchronizer;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaSourceUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.PreferencesMgr;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.SystemUtils;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Clock;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.RawResourceDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.rtmp.RtmpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector;
import androidx.media3.exoplayer.source.ConcatenatingMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MergingMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.source.SingleSampleMediaSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;
import androidx.media3.extractor.ts.TsExtractor;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;

import android.content.Context;
import android.media.audiofx.LoudnessEnhancer;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Manages ExoPlayer and an internal media queue */
public final class PlayerManager implements Player.Listener, PreferencesMgr.OnPreferenceChangeListener {

  private static final String TAG = "PlayerManager";

  private final class MyArrayList<E> extends ArrayList<E> {
    public void retainLast(int count) {
      int last = this.size() - count;

      if (last > 0)
        removeRange(0, last);
    }
  }

  private Context context;
  private PlayerView playerView;
  private MyArrayList<VideoSource> mediaQueue;
  private ConcatenatingMediaSource concatenatingMediaSource;
  private MyRenderersFactory renderersFactory;
  private DefaultExtractorsFactory extractorsFactory;
  private DefaultTrackSelector trackSelector;
  private DefaultHttpDataSource.Factory httpDataSourceFactory;
  private DataSource.Factory defaultDataSourceFactory;
  private CacheDataSource.Factory cacheDataSourceFactory;
  private RtmpDataSource.Factory rtmpDataSourceFactory;
  private DownloadTracker downloadTracker;
  private float audioVolume;
  private boolean captionsDisabled;
  private MyLoadErrorHandlingPolicy loadErrorHandlingPolicy;
  private int currentItemIndex;
  private Handler handler;
  private LoudnessEnhancer loudnessEnhancer;

  public TextSynchronizer textSynchronizer;
  public ExoPlayer exoPlayer;

  /**
   * @param context A {@link Context}.
   */
  public static PlayerManager createPlayerManager(Context context) {
    PlayerManager playerManager = new PlayerManager(context);
    playerManager.init();
    return playerManager;
  }

  private PlayerManager(Context context) {
    this.context                  = context;
    this.playerView               = null;
    this.mediaQueue               = new MyArrayList<>();
    this.concatenatingMediaSource = new ConcatenatingMediaSource();
    this.renderersFactory         = new MyRenderersFactory(context, PreferencesMgr.get_prefer_extension_renderer());
    this.extractorsFactory        = new DefaultExtractorsFactory();
    this.trackSelector            = new DefaultTrackSelector(context);
    this.textSynchronizer         = (TextSynchronizer) renderersFactory;

    extractorsFactory.setTsExtractorTimestampSearchBytes(
      (int) (PreferencesMgr.get_ts_extractor_timestamp_search_bytes_factor() * TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES)
    );

    extractorsFactory.setTsExtractorFlags(
      PreferencesMgr.get_enable_hdmv_dts_audio_streams()
        ? DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS
        : 0
    );

    trackSelector.setParameters(
      trackSelector.buildUponParameters()
        .setTunnelingEnabled(
          PreferencesMgr.get_enable_tunneled_video_playback()
        )
    );

    DefaultLoadControl loadControl = getLoadControl(context);

    EventLogger exoLogger = new EventLogger(trackSelector);
    DefaultAnalyticsCollector analyticsCollector = new DefaultAnalyticsCollector(Clock.DEFAULT);
    analyticsCollector.addListener(exoLogger);

    String userAgent               = PreferencesMgr.get_default_user_agent();
    VideoSource.DEFAULT_USER_AGENT = userAgent;
    ExoPlayerUtils.setUserAgent(userAgent);

    this.httpDataSourceFactory     = ExoPlayerUtils.getHttpDataSourceFactory(context);
    this.defaultDataSourceFactory  = ExoPlayerUtils.getDefaultDataSourceFactory(context);
    this.cacheDataSourceFactory    = ExoPlayerUtils.getCacheDataSourceFactory(context);
    this.rtmpDataSourceFactory     = ExoPlayerUtils.getRtmpDataSourceFactory();
    this.downloadTracker           = ExoPlayerUtils.getDownloadTracker(context);

    ExoPlayer.Builder builder = new ExoPlayer.Builder(
      context,
      (RenderersFactory) renderersFactory,
      new DefaultMediaSourceFactory(cacheDataSourceFactory, extractorsFactory),
      trackSelector,
      loadControl,
      DefaultBandwidthMeter.getSingletonInstance(context),
      analyticsCollector
    );

    builder
      .setSeekBackIncrementMs(
        PreferencesMgr.get_seek_back_ms_increment()
      )
      .setSeekForwardIncrementMs(
        PreferencesMgr.get_seek_forward_ms_increment()
      )
      .setUsePlatformDiagnostics(false)
    ;

    this.exoPlayer = builder.build();
    this.exoPlayer.addListener(this);
    this.exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
    this.exoPlayer.setHandleAudioBecomingNoisy(
      PreferencesMgr.get_pause_on_change_to_audio_output_device()
    );

    this.audioVolume             = 1.0f;
    this.captionsDisabled        = false;
    this.loadErrorHandlingPolicy = new MyLoadErrorHandlingPolicy();
    this.currentItemIndex        = C.INDEX_UNSET;
    this.handler                 = new Handler(Looper.getMainLooper());

    this.onAudioSessionIdChanged( this.exoPlayer.getAudioSessionId() );
    this.downloadTracker.startDownloadService();

    PreferencesMgr.addOnPreferenceChangedListener(this);
  }

  private DefaultLoadControl getLoadControl(Context context) {
    long thresholdMemorySizeInBytes = 1610612736l; // 1.5 GiB
    long memorySizeInBytes          = SystemUtils.getMemorySizeInBytes(context);
    float factor                    = (memorySizeInBytes <= thresholdMemorySizeInBytes) ? 1.0f : 1.5f;

    Log.d(TAG, "memory=" + (float)(((int)((memorySizeInBytes*100)/(1024*1024*1024)))/100f) + "GB, buffer factor=" + (int)(factor*100) + "%");

    DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();

    if (Float.compare(factor, 1.0f) != 0) { // if (factor != 1.0f)
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
   * @return The position of the current video in milliseconds.
   */
  public long getCurrentVideoPosition() {
    if (exoPlayer == null)
      return 0l;

    long positionMs = exoPlayer.getCurrentPosition();
    return positionMs;
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

  public String getCurrentItemJson() {
    return getCurrentItemJson(/* indentSpaces= */ 0);
  }

  public String getCurrentItemJson(int indentSpaces) {
    try {
      JSONObject json = getCurrentItemJSONObject();
      return (json == null)
        ? null
        : (indentSpaces <= 0)
          ? json.toString()
          : json.toString(indentSpaces);
    }
    catch(Exception e) {
      return null;
    }
  }

  public JSONObject getCurrentItemJSONObject() {
    try {
      VideoSource sample = getCurrentItem();

      JSONObject json        = new JSONObject();
      JSONObject req_headers = new JSONObject();
      JSONObject drm_headers = new JSONObject();

      if ((sample != null) && (sample.reqHeadersMap != null) && !sample.reqHeadersMap.isEmpty()) {
        for (Map.Entry<String,String> header : sample.reqHeadersMap.entrySet()) {
          req_headers.put(header.getKey(), header.getValue());
        }
      }

      if ((sample != null) && (sample.drmHeadersMap != null) && !sample.drmHeadersMap.isEmpty()) {
        for (Map.Entry<String,String> header : sample.drmHeadersMap.entrySet()) {
          drm_headers.put(header.getKey(), header.getValue());
        }
      }

      json.put(Constant.MediaItemInfo.IS_PLAYER_READY,        isPlayerReady());
      json.put(Constant.MediaItemInfo.IS_PLAYER_PAUSED,       isPlayerPaused());
      json.put(Constant.MediaItemInfo.MEDIA_URL,              (sample == null) ? null : sample.uri);
      json.put(Constant.MediaItemInfo.MEDIA_TYPE,             (sample == null) ? null : sample.uri_mimeType);
      json.put(Constant.MediaItemInfo.CAPTION_URL,            (sample == null) ? null : sample.caption);
      json.put(Constant.MediaItemInfo.REFERER_URL,            (sample == null) ? null : sample.referer);
      json.put(Constant.MediaItemInfo.REQUEST_HEADERS,        req_headers);
      json.put(Constant.MediaItemInfo.USE_OFFLINE_CACHE,      (sample == null) ? null : sample.useCache);
      json.put(Constant.MediaItemInfo.START_POSITION,         (sample == null) ? null : sample.startPosition);
      json.put(Constant.MediaItemInfo.STOP_POSITION,          (sample == null) ? null : sample.stopPosition);
      json.put(Constant.MediaItemInfo.CURRENT_POSITION,       (sample == null) ? null : getCurrentVideoPosition());
      json.put(Constant.MediaItemInfo.DURATION,               (sample == null) ? null : getCurrentVideoDuration());
      json.put(Constant.MediaItemInfo.DRM_SCHEME,             (sample == null) ? null : sample.drm_scheme);
      json.put(Constant.MediaItemInfo.DRM_LICENSE_SERVER_URL, (sample == null) ? null : sample.drm_license_server);
      json.put(Constant.MediaItemInfo.DRM_REQUEST_HEADERS,    drm_headers);

      return json;
    }
    catch(Exception e) {
      return null;
    }
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
    HashMap<String, String> reqHeadersMap,
    boolean useCache,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    HashMap<String, String> drmHeadersMap
  ) {
    addItem(uri, caption, referer, reqHeadersMap, useCache, startPosition, stopPosition, drm_scheme, drm_license_server, drmHeadersMap, /* remove_previous_items= */ false);
  }

  /**
   * Appends {@link VideoSource} to the media queue.
   *
   * @param uri                   The URL to a video file or stream.
   * @param caption               The URL to a file containing text captions (srt or vtt).
   * @param referer               The URL to include in the 'Referer' HTTP header of requests to retrieve the video file or stream.
   * @param reqHeadersMap         Map of HTTP headers to include in requests to retrieve the video file or stream.
   * @param useCache              Boolean flag to indicate whether to prefetch this item to a local cache.
   * @param startPosition         The position at which to start playback within the video file or (non-live) stream. When value < 1.0 and stopPosition < value, it is interpreted to mean a percentage of the total video length. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   * @param stopPosition          The position at which to stop playback within the video file or (non-live) stream. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   * @param drm_scheme            The DRM scheme; value in: ["widevine","playready","clearkey"]
   * @param drm_license_server    The URL to obtain DRM license keys.
   * @param drmHeadersMap         Map of HTTP headers to include in requests to retrieve the DRM license keys.
   * @param remove_previous_items A boolean flag to indicate whether all previous items in queue should be removed after new items have been appended.
   */
  public void addItem(
    String uri,
    String caption,
    String referer,
    HashMap<String, String> reqHeadersMap,
    boolean useCache,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    HashMap<String, String> drmHeadersMap,
    boolean remove_previous_items
  ) {
    VideoSource sample = VideoSource.createVideoSource(uri, caption, referer, reqHeadersMap, useCache, startPosition, stopPosition, drm_scheme, drm_license_server, drmHeadersMap);
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
    HashMap<String, String> reqHeadersMap,
    boolean useCache,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    HashMap<String, String> drmHeadersMap
  ) {
    addItems(uris, caption, referer, reqHeadersMap, useCache, startPosition, stopPosition, drm_scheme, drm_license_server, drmHeadersMap, /* remove_previous_items= */ false);
  }

  /**
   * Appends {@link VideoSource} to the media queue.
   *
   * @param uris                  Array of URLs to video files or streams.
   * @param caption               The URL to a file containing text captions (srt or vtt).
   * @param referer               The URL to include in the 'Referer' HTTP header of requests to retrieve the video file or stream.
   * @param reqHeadersMap         Map of HTTP headers to include in requests to retrieve the video file or stream.
   * @param useCache              Boolean flag to indicate whether to prefetch this item to a local cache.
   * @param startPosition         The position at which to start playback within the video file or (non-live) stream. When value < 1.0 and stopPosition < value, it is interpreted to mean a percentage of the total video length. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   * @param stopPosition          The position at which to stop playback within the video file or (non-live) stream. When value >= 1.0, it is interpreted to mean a fixed offset in seconds.
   * @param drm_scheme            The DRM scheme; value in: ["widevine","playready","clearkey"]
   * @param drm_license_server    The URL to obtain DRM license keys.
   * @param drmHeadersMap         Map of HTTP headers to include in requests to retrieve the DRM license keys.
   * @param remove_previous_items A boolean flag to indicate whether all previous items in queue should be removed after new items have been appended.
   */
  public void addItems(
    String[] uris,
    String caption,
    String referer,
    HashMap<String, String> reqHeadersMap,
    boolean useCache,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    HashMap<String, String> drmHeadersMap,
    boolean remove_previous_items
  ) {
    VideoSource[] samples = new VideoSource[uris.length];
    String uri;
    VideoSource sample;

    for (int i=0; i < uris.length; i++) {
      uri        = uris[i];
      sample     = (i == 0)
                     ? VideoSource.createVideoSource(uri, caption, referer, reqHeadersMap, useCache, startPosition, stopPosition, drm_scheme, drm_license_server, drmHeadersMap)
                     : VideoSource.createVideoSource(uri, null,    referer, reqHeadersMap, useCache, -1f,           -1f,          drm_scheme, drm_license_server, drmHeadersMap)
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

    for (VideoSource sample : samples) {
      if (
           !sample.useCache
        && !TextUtils.isEmpty(sample.uri)
        && downloadTracker.isDownloaded(sample.uri)
      ) {
        sample.updateUseCache(true);
      }
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
   * Update the {@code caption} for the current media queue {@link VideoSource} and corresponding playlist {@link MediaSource}.
   *
   * @param caption The URL to a file containing text captions (srt or vtt).
   */
  public void loadCaptions(String caption) {
    if (exoPlayer == null)
      return;
    if ((mediaQueue == null) || (concatenatingMediaSource == null))
      return;
    if (currentItemIndex == C.INDEX_UNSET)
      return;
    if (TextUtils.isEmpty(caption))
      return;

    int sample_index = currentItemIndex;
    long positionMs  = exoPlayer.getCurrentPosition();

    VideoSource sample = getItem(sample_index);
    if (sample == null)
      return;

    if (caption.equals(sample.caption))
      return;

    currentItemIndex = C.INDEX_UNSET;
    exoPlayer.setPlayWhenReady(false);
    exoPlayer.retry();

    sample.updateCaption(caption);
    MediaSource mediaSource = buildMediaSource(sample);

    Runnable addCompletionAction = new Runnable() {
      @Override
      public void run() {
        selectQueueItem(sample_index);
        exoPlayer.seekTo(sample_index, positionMs);
      }
    };

    Runnable removeCompletionAction = new Runnable() {
      @Override
      public void run() {
        concatenatingMediaSource.addMediaSource(sample_index, mediaSource, handler, addCompletionAction);
      }
    };

    concatenatingMediaSource.removeMediaSource(sample_index, handler, removeCompletionAction);
  }

  /**
   * @return The size of the media queue.
   */
  public int getMediaQueueSize() {
    if (mediaQueue == null)
      return 0;

    return mediaQueue.size();
  }

  private MediaItem getCurrentMediaItem() {
    VideoSource sample = getCurrentItem();
    return getMediaItem(sample);
  }

  private MediaItem getMediaItem(VideoSource sample) {
    return getMediaItem(sample, /* forOfflineDownload= */ false);
  }

  private MediaItem getMediaItem(VideoSource sample, boolean forOfflineDownload) {
    return (sample != null)
      ? sample.getMediaItem(forOfflineDownload)
      : null;
  }

  public VideoSource getCurrentItem() {
    return getItem(getCurrentItemIndex());
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

  // Offline cache manipulation methods.

  private boolean isCurrentItemDownloaded() {
    MediaItem mediaItem = getCurrentMediaItem();
    return ((mediaItem != null) && downloadTracker.isDownloaded(mediaItem));
  }

  public boolean doesCurrentItemUseCache() {
    VideoSource sample = getCurrentItem();
    return ((sample != null) && sample.useCache);
  }

  public void toggleCurrentItemUseCache() {
    VideoSource sample = getCurrentItem();
    if (sample == null) return;

    MediaItem mediaItem = getMediaItem(sample, /* forOfflineDownload= */ true);
    if (mediaItem == null) return;

    sample.updateUseCache(!sample.useCache);
    downloadTracker.toggleDownload(
      mediaItem,
      renderersFactory,
      ((exoPlayer == null) ? null : exoPlayer.getTrackSelectionParameters())
    );
  }

  // AirPlay functionality (exposed by HTTP endpoints)
  //   http://nto.github.io/AirPlay.html#video

  /**
   * Clears the media queue and adds the specified {@link VideoSource}.
   */
  public void AirPlay_play(
    String uri,
    String caption,
    String referer,
    HashMap<String, String> reqHeadersMap,
    boolean useCache,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    HashMap<String, String> drmHeadersMap
  ) {
    addItem(uri, caption, referer, reqHeadersMap, useCache, startPosition, stopPosition, drm_scheme, drm_license_server, drmHeadersMap, /* remove_previous_items= */ true);
  }

  /**
   * Seek within the current video.
   *
   * @param positionSec The position as a fixed offset in seconds.
   */
  public void AirPlay_scrub(float positionSec) {
    if (exoPlayer == null) return;

    if (exoPlayer.isCurrentMediaItemSeekable()) {
      long positionMs = (long) (positionSec * 1000.0f);
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

    if (exoPlayer.isCurrentMediaItemSeekable()) {
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

    if (Float.compare(rate, 0.0f) == 0) { // if (rate == 0.0f)
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
   */
  public void AirPlay_queue(
    String uri,
    String caption,
    String referer,
    HashMap<String, String> reqHeadersMap,
    boolean useCache,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    HashMap<String, String> drmHeadersMap
  ) {
    addItem(uri, caption, referer, reqHeadersMap, useCache, startPosition, stopPosition, drm_scheme, drm_license_server, drmHeadersMap);
  }

  /**
   * Skip forward to the next {@link VideoSource} in the media queue.
   */
  public void AirPlay_next() {
    if (exoPlayer == null) return;

    if (exoPlayer.hasNextMediaItem()) {
      exoPlayer.seekToNextMediaItem();
    }
  }

  /**
   * Skip backward to the previous {@link VideoSource} in the media queue.
   */
  public void AirPlay_previous() {
    if (exoPlayer == null) return;

    if (exoPlayer.hasPreviousMediaItem()) {
      exoPlayer.seekToPreviousMediaItem();
    }
  }

  /**
   * Change audio volume level.
   *
   * @param newAudioVolume New audio volume level.
   *                       The range of acceptable values is 0.0 to (AUDIO_VOLUME_MAX_DB_BOOST + 1.0).
   *                       The value 0.0 is   0% input volume (equivalent to 'mute').
   *                       The value 1.0 is 100% input volume (unity gain).
   *                       The value 6.5 is 100% input volume and amplified by 5.5 dB.
   */
  public void AirPlay_volume(float newAudioVolume) {
    if (exoPlayer == null)
      return;

    float maxVolume = (loudnessEnhancer != null)
      ? (float) (PreferencesMgr.get_max_audio_volume_boost_db() + 1.0f)
      : 1.0f;

    if (Float.compare(newAudioVolume, 0.0f) < 0) // if (newAudioVolume < 0.0f)
      newAudioVolume = 0.0f;

    if (Float.compare(newAudioVolume, maxVolume) > 0) // if (newAudioVolume > maxVolume)
      newAudioVolume = maxVolume;

    if (Float.compare(this.audioVolume, newAudioVolume) == 0) // if (this.audioVolume == newAudioVolume)
      return;

    this.audioVolume = newAudioVolume;

    if (Float.compare(newAudioVolume, 1.0f) <= 0) { // if (newAudioVolume <= 1.0f)
      exoPlayer.setVolume(newAudioVolume); // range of values: 0.0 (mute) ... 1.0 (unity gain)

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
          float gain_dB = newAudioVolume - 1.0f;
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

    captionsDisabled = !showCaptions;

    DefaultTrackSelector.Parameters.Builder builder = trackSelector.getParameters().buildUpon();

    MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
    if (info == null) return;

    int renderer_count = info.getRendererCount();
    int modified_count = 0;
    for (int i = 0; i < renderer_count; i++) {
      if (exoPlayer.getRendererType(i) == C.TRACK_TYPE_TEXT) {
        builder.clearSelectionOverrides(/* rendererIndex= */ i);
        builder.setRendererDisabled(/* rendererIndex= */ i, captionsDisabled);
        modified_count++;
      }
    }

    if (modified_count > 0)
      trackSelector.setParameters(builder.build());
  }

  public void AirPlay_toggle_captions() {
    AirPlay_show_captions(/* showCaptions= */ captionsDisabled);
  }

  /**
   * Set font style and size for text captions.
   *
   * @param fontSize      Measured in "sp" units
   * @param applyEmbedded Apply styles and sizes embedded in the text captions?
   */
  public void AirPlay_set_captions_style(Integer fontSize, Boolean applyEmbedded) {
    if (
      (playerView == null) ||
      ((fontSize == null) && (applyEmbedded == null))
    ) return;

    SubtitleView subtitleView = playerView.getSubtitleView();
    if (subtitleView == null) return;

    boolean isCustomFontSize = false;

    if (fontSize != null) {
      int valFontSize = fontSize.intValue();

      if (valFontSize == 0) {
        subtitleView.setFractionalTextSize​(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION);
        subtitleView.setUserDefaultTextSize();
      }
      else if ((valFontSize > 0) && (valFontSize <= Constant.MAX_FONT_SIZE_SP)) {
        isCustomFontSize = true;
        subtitleView.setFixedTextSize​(android.util.TypedValue.COMPLEX_UNIT_SP, fontSize.floatValue());
      }
    }

    if (applyEmbedded != null) {
      boolean valApplyEmbedded = applyEmbedded.booleanValue();

      subtitleView.setApplyEmbeddedStyles(   valApplyEmbedded);
      subtitleView.setApplyEmbeddedFontSizes(valApplyEmbedded && !isCustomFontSize);
    }
    else if (isCustomFontSize) {
      subtitleView.setApplyEmbeddedFontSizes(false);
    }
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

  /**
   * Set the repeat mode of the ExoPlayer Player.
   *
   * @param repeatMode Integer constant:
   *                   https://developer.android.com/reference/androidx/media3/common/Player.RepeatMode
   */
  public void AirPlay_repeat_mode(int repeatMode) {
    if (exoPlayer == null) return;

    exoPlayer.setRepeatMode(repeatMode);
  }

  /**
   * Set the resize mode of the ExoPlayer PlayerView.
   *
   * @param resizeMode Integer constant:
   *                   https://developer.android.com/reference/androidx/media3/ui/AspectRatioFrameLayout.ResizeMode
   */
  public void AirPlay_resize_mode(int resizeMode) {
    if (playerView == null) return;

    playerView.setResizeMode(resizeMode);
  }

  public void AirPlay_toggle_resize_mode() {
    if (playerView == null) return;

    int resizeMode = playerView.getResizeMode();

    // 0=fit,1=width,2=height,3=fill,4=zoom
    resizeMode = (resizeMode + 1) % 5;

    playerView.setResizeMode(resizeMode);
  }

  /**
   * Delete files on internal storage used to temporarily cache video data.
   * Cancel all active and pending cache download operations.
   */
  public void AirPlay_delete_cache() {
    if (downloadTracker != null)
      downloadTracker.removeAllDownloads();

    if (mediaQueue != null) {
      for (int i = 0; i < mediaQueue.size(); i++) {
        VideoSource sample = mediaQueue.get(i);

        if (sample.useCache)
          sample.updateUseCache(false);
      }
    }
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

        case KeyEvent.KEYCODE_MEDIA_PLAY : {
          if (isPlayerReady() && !exoPlayer.getPlayWhenReady()) {
            exoPlayer.setPlayWhenReady(true);
            isHandled = true;
          }
          break;
        }

        case KeyEvent.KEYCODE_MEDIA_PAUSE : {
          if (isPlayerReady() && exoPlayer.getPlayWhenReady()) {
            exoPlayer.setPlayWhenReady(false);
            isHandled = true;
          }
          break;
        }

        case KeyEvent.KEYCODE_SPACE :
        case KeyEvent.KEYCODE_HEADSETHOOK :
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE : {
          if (isPlayerReady()) {
            // toggle pause
            exoPlayer.setPlayWhenReady( !exoPlayer.getPlayWhenReady() );
            isHandled = true;
          }
          break;
        }

        case KeyEvent.KEYCODE_MEDIA_STOP : {
          AirPlay_stop();
          isHandled = true;
          break;
        }

        case KeyEvent.KEYCODE_MEDIA_PREVIOUS : {
          AirPlay_previous();
          isHandled = true;
          break;
        }

        case KeyEvent.KEYCODE_MEDIA_NEXT : {
          AirPlay_next();
          isHandled = true;
          break;
        }

        case KeyEvent.KEYCODE_MEDIA_REWIND : {
          long offsetMs = -5000l; // -5 seconds
          AirPlay_add_scrub_offset(offsetMs);
          isHandled = true;
          break;
        }

        case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD : {
          long offsetMs = 15000l; // +15 seconds
          AirPlay_add_scrub_offset(offsetMs);
          isHandled = true;
          break;
        }

        case KeyEvent.KEYCODE_CAPTIONS : {
          AirPlay_toggle_captions();
          isHandled = true;
          break;
        }

        case KeyEvent.KEYCODE_TV_ZOOM_MODE : {
          AirPlay_toggle_resize_mode();
          isHandled = true;
          break;
        }

        case KeyEvent.KEYCODE_VOLUME_DOWN : {
          if (Float.compare(audioVolume, 1.0f) > 0) { // if (audioVolume > 1.0f)
            // decrease amplification
            float newAudioVolume = audioVolume - PreferencesMgr.get_audio_volume_boost_db_increment();
            if (Float.compare(newAudioVolume, 1.0f) < 0) // if (newAudioVolume < 1.0f)
              newAudioVolume = 1.0f;
            AirPlay_volume(newAudioVolume);
            isHandled = true;
          }
          break;
        }

        case KeyEvent.KEYCODE_VOLUME_UP : {
          if (Float.compare(audioVolume, 1.0f) < 0) { // if (audioVolume < 1.0f)
            // increase volume
            float newAudioVolume = audioVolume + PreferencesMgr.get_audio_volume_percent_increment();
            if (Float.compare(newAudioVolume, 1.0f) > 0) // if (newAudioVolume > 1.0f)
              newAudioVolume = 1.0f;
            AirPlay_volume(newAudioVolume);
            isHandled = true;
          }
          else {
            AudioManager systemAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int systemVolume            = systemAudioMgr.getStreamVolume(AudioManager.STREAM_MUSIC);
            int systemVolumeMax         = systemAudioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            if (systemVolume == systemVolumeMax) {
              float maxVolume = (float) (PreferencesMgr.get_max_audio_volume_boost_db() + 1.0f);

              if (Float.compare(audioVolume, maxVolume) < 0) { // if (audioVolume < maxVolume)
                // increase amplification
                AirPlay_volume(audioVolume + PreferencesMgr.get_audio_volume_boost_db_increment());
                isHandled = true;
              }
            }
          }
          break;
        }
      }
    }

    return isHandled;
  }

  public void release() {
    release(false, 0l);
  }

  /**
   * Releases the manager and instance of ExoPlayer that it holds.
   */
  public void release(boolean shutdown, long delay_ms) {
    try {
      PreferencesMgr.removeOnPreferenceChangedListener(this);

      release_exoPlayer();

      if (mediaQueue != null)
        mediaQueue.clear();

      if (concatenatingMediaSource != null)
        concatenatingMediaSource.clear();

      if (downloadTracker != null)
        downloadTracker.removeAllDownloads();
    }
    catch (Exception e) {}
    finally {
      if (shutdown) {
        shutdownAfterAllDownloadsRemoved(downloadTracker, (delay_ms > 0l) ? delay_ms : 0l);
      }

      mediaQueue               = null;
      concatenatingMediaSource = null;
      renderersFactory         = null;
      extractorsFactory        = null;
      httpDataSourceFactory    = null;
      defaultDataSourceFactory = null;
      cacheDataSourceFactory   = null;
      downloadTracker          = null;
      loadErrorHandlingPolicy  = null;
      currentItemIndex         = C.INDEX_UNSET;
    }
  }

  private void shutdownAfterAllDownloadsRemoved(final DownloadTracker downloadTracker, final long delay_ms) {
    if (downloadTracker != null) {
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          downloadTracker.addAllDownloadsRemovedCallback(new DownloadTracker.AllDownloadsRemovedCallback() {
            @Override
            public void onAllDownloadsRemoved() {
              Process.killProcess(Process.myPid());
            }
          });
        }
      }, delay_ms);
    }
    else {
      Process.killProcess(Process.myPid());
    }
  }

  /**
   * Releases the instance of ExoPlayer.
   */
  private void release_exoPlayer() {
    setPlayerView(null);

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

    if (loudnessEnhancer != null) {
      loudnessEnhancer.setEnabled(false);
      loudnessEnhancer.release();
      loudnessEnhancer = null;
    }
  }

  // ===========================================================================
  // https://github.com/androidx/media/blob/1.0.0-beta03/libraries/common/src/main/java/androidx/media3/common/Player.java#L625-L1073
  // ===========================================================================
  // Player.Listener implementation.
  // ===========================================================================

  @Override
  public void onPlaybackStateChanged(@Player.State int playbackState) {
    updateCurrentItemIndex();
  }

  @Override
  public void onPlayWhenReadyChanged(boolean playWhenReady, @Player.PlayWhenReadyChangeReason int reason) {
    updateCurrentItemIndex();
  }

  @Override
  public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, @Player.DiscontinuityReason int reason) {
    updateCurrentItemIndex();
  }

  @Override
  public void onTimelineChanged(Timeline timeline, @Player.TimelineChangeReason int reason) {
    updateCurrentItemIndex();
  }

  @Override
  public void onAudioSessionIdChanged(int audioSessionId) {
    if (Build.VERSION.SDK_INT >= 19) {
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
  }

  @Override
  public void onPlayerError(PlaybackException error) {
    if ((error == null) || (exoPlayer == null))
      return;

    switch(error.errorCode) {
      case PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW :
      case PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE :
      case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED :
      case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT :
      case PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR :
      case PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED :
      case PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED : {
        retry();
        break;
      }
      case PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE :
      case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS :
      case PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND :
      case PlaybackException.ERROR_CODE_IO_NO_PERMISSION :
      case PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED :
      case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED :
      case PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED :
      case PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED :
      case PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED :
      case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED :
      case PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED :
      case PlaybackException.ERROR_CODE_DECODING_FAILED :
      case PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES :
      case PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED :
      case PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED :
      case PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED :
      case PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED :
      case PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR :
      case PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED : {
        exoPlayer.seekToNextMediaItem();
        break;
      }
    }
  }

  // ===========================================================================
  // PreferencesMgr.OnPreferenceChangeListener implementation.
  // ===========================================================================

  @Override
  public void onPreferenceChange(int pref_key_id) {
    switch(pref_key_id) {
      case R.string.prefkey_default_user_agent : {
        String userAgent               = PreferencesMgr.get_default_user_agent();
        VideoSource.DEFAULT_USER_AGENT = userAgent;
        ExoPlayerUtils.setUserAgent(userAgent);
        break;
      }

      case R.string.prefkey_ts_extractor_timestamp_search_bytes_factor : {
        extractorsFactory.setTsExtractorTimestampSearchBytes(
          (int) (PreferencesMgr.get_ts_extractor_timestamp_search_bytes_factor() * TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES)
        );
        break;
      }

      case R.string.prefkey_enable_hdmv_dts_audio_streams : {
        extractorsFactory.setTsExtractorFlags(
          PreferencesMgr.get_enable_hdmv_dts_audio_streams()
            ? DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS
            : 0
        );
        break;
      }

      case R.string.prefkey_enable_tunneled_video_playback : {
        trackSelector.setParameters(
          trackSelector.buildUponParameters()
            .setTunnelingEnabled(
              PreferencesMgr.get_enable_tunneled_video_playback()
            )
        );
        break;
      }

      case R.string.prefkey_pause_on_change_to_audio_output_device : {
        exoPlayer.setHandleAudioBecomingNoisy(
          PreferencesMgr.get_pause_on_change_to_audio_output_device()
        );
        break;
      }

      case R.string.prefkey_max_audio_volume_boost_db       :
      case R.string.prefkey_audio_volume_percent_increment  :
      case R.string.prefkey_audio_volume_boost_db_increment : {
        // nothing to do: value is requested when needed
        break;
      }

      case R.string.prefkey_max_parallel_downloads    :
      case R.string.prefkey_seek_back_ms_increment    :
      case R.string.prefkey_seek_forward_ms_increment :
      case R.string.prefkey_prefer_extension_renderer : {
        // nothing to do: value will take effect when app is restarted
        break;
      }
    }
  }

  // Internal methods.

  private void init() {
    if (exoPlayer == null) return;

    // Media queue management.
    exoPlayer.prepare(concatenatingMediaSource);
  }

  private void retry() {
    if (exoPlayer == null) return;

    exoPlayer.seekToDefaultPosition();
    exoPlayer.retry();
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
      ? exoPlayer.getCurrentMediaItemIndex()
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
        downloadToCache(currentItemIndex);
      }
    }
  }

  private void seekToStartPosition(int currentItemIndex) {
    if (exoPlayer == null) return;

    VideoSource sample = getItem(currentItemIndex);
    if (sample == null) return;

    float position = sample.startPosition;

    if ((Float.compare(position, 0.0f) > 0) && (Float.compare(position, 1.0f) < 0)) { // if ((position > 0.0f) && (position < 1.0f))
      // percentage
      long duration = exoPlayer.getDuration(); // ms
      if (duration != C.TIME_UNSET) {
        long positionMs = (long) (duration * position);
        exoPlayer.seekTo(currentItemIndex, positionMs);
      }
    }
    else if (Float.compare(position, 1.0f) >= 0) { // if (position >= 1.0f)
      // fixed offset in seconds
      long positionMs = (long) (position * 1000.0f);
      exoPlayer.seekTo(currentItemIndex, positionMs);
    }
    else {
      exoPlayer.seekToDefaultPosition(currentItemIndex);
    }
  }

  private void setHttpRequestHeaders(int currentItemIndex) {
    if (httpDataSourceFactory == null) return;

    VideoSource sample = getItem(currentItemIndex);
    if (sample == null) return;
    if (ExternalStorageUtils.isFileUri(sample.uri)) return;

    if (sample.reqHeadersMap != null) {
      httpDataSourceFactory.setDefaultRequestProperties(sample.reqHeadersMap);
    }
  }

  private void downloadToCache(int currentItemIndex) {
    if (downloadTracker == null) return;

    VideoSource sample = getItem(currentItemIndex);
    if (sample == null) return;

    if (sample.useCache) {
      downloadTracker.startDownload(sample.getMediaItem(/* forOfflineDownload= */ true), renderersFactory);
    }
  }

  private MediaSource buildMediaSource(VideoSource sample) {
    MediaSource            video    = buildUriMediaSource(sample);
    ArrayList<MediaSource> captions = buildCaptionMediaSources(video);
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
    DataSource.Factory factory = (ExternalStorageUtils.isFileUri(sample.uri) || ExternalStorageUtils.isContentUri(sample.uri))
      ? defaultDataSourceFactory
      : cacheDataSourceFactory;

    if (factory == null)
      return null;

    MediaItem mediaItem = sample.getMediaItem();

    switch (sample.uri_mimeType) {
      case "application/x-mpegURL":
      case "application/x-mpegurl":
        return new HlsMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem);
      case "application/dash+xml":
        return new DashMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem);
      case "application/vnd.ms-sstr+xml":
        return new SsMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem);
      case "application/x-rtsp":
        return new RtspMediaSource.Factory().createMediaSource(mediaItem);
      case "application/x-rtmp":
        factory = rtmpDataSourceFactory;
        return new ProgressiveMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem);
      default:
        return new ProgressiveMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(mediaItem);
    }
  }

  private ArrayList<MediaSource> buildCaptionMediaSources(MediaSource video) {
    ArrayList<MediaSource> captions = new ArrayList<MediaSource>();
    DataSource.Factory factory;

    try {
      for (MediaItem.SubtitleConfiguration subtitleConfiguration : video.getMediaItem().localConfiguration.subtitleConfigurations) {
        factory = ExternalStorageUtils.isFileUri(subtitleConfiguration.uri.toString())
          ? defaultDataSourceFactory
          : httpDataSourceFactory;

        captions.add(
          new SingleSampleMediaSource.Factory(factory).setLoadErrorHandlingPolicy(loadErrorHandlingPolicy).createMediaSource(subtitleConfiguration, C.TIME_UNSET)
        );
      }
    }
    catch (Exception e) {}

    // normalize that non-null return value must include matches
    if (captions.isEmpty())
      captions = null;

    return captions;
  }

  private MediaSource applyClippingProperties(MediaSource mediaSource, VideoSource sample) {
    MediaItem mediaItem = sample.getMediaItem();

    return MediaSourceUtils.applyClippingProperties(mediaSource, mediaItem);
  }

  private MediaSource buildRawVideoMediaSource(int rawResourceId) {
    if (defaultDataSourceFactory == null)
      return null;

    Uri uri = RawResourceDataSource.buildRawResourceUri(rawResourceId);
    MediaItem mediaItem = MediaItem.fromUri(uri);

    return new ProgressiveMediaSource.Factory(defaultDataSourceFactory).createMediaSource(mediaItem);
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
