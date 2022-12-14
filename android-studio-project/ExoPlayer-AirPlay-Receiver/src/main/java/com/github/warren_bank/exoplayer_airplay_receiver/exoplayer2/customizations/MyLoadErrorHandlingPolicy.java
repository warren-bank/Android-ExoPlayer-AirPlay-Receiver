package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

import android.util.Log;

import androidx.media3.common.C;
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;

import java.io.IOException;

public class MyLoadErrorHandlingPolicy extends DefaultLoadErrorHandlingPolicy {
  private static final String TAG = "LoadErrorHandlingPolicy";

  private static final int  DEFAULT_RETRY_COUNT            = 3;
  private static final long DEFAULT_OFFLINE_RETRY_DELAY_MS = 6000l;

  private boolean isOffline;
  private int     retryCount;
  private long    offlineRetryDelayMs;

  public MyLoadErrorHandlingPolicy() {
    this(DEFAULT_RETRY_COUNT, DEFAULT_OFFLINE_RETRY_DELAY_MS);
  }

  public MyLoadErrorHandlingPolicy(int retryCount) {
    this(retryCount, DEFAULT_OFFLINE_RETRY_DELAY_MS);
  }

  public MyLoadErrorHandlingPolicy(long offlineRetryDelayMs) {
    this(DEFAULT_RETRY_COUNT, offlineRetryDelayMs);
  }

  public MyLoadErrorHandlingPolicy(int retryCount, long offlineRetryDelayMs) {
    super();
    this.isOffline           = false;
    this.retryCount          = retryCount;
    this.offlineRetryDelayMs = offlineRetryDelayMs;
  }

  // ===========================================================================
  // https://github.com/androidx/media/blob/1.0.0-beta03/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/upstream/DefaultLoadErrorHandlingPolicy.java#L114
  // https://github.com/androidx/media/blob/1.0.0-beta03/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/upstream/LoadErrorHandlingPolicy.java#L75
  // https://github.com/androidx/media/blob/1.0.0-beta03/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/source/LoadEventInfo.java
  // https://github.com/androidx/media/blob/1.0.0-beta03/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/source/MediaLoadData.java
  // ===========================================================================

  @Override
  public long getRetryDelayMsFor(LoadErrorHandlingPolicy.LoadErrorInfo loadErrorInfo) {
    int         dataType       = loadErrorInfo.mediaLoadData.dataType;
    long        loadDurationMs = loadErrorInfo.loadEventInfo.loadDurationMs;
    IOException exception      = loadErrorInfo.exception;
    int         errorCount     = loadErrorInfo.errorCount;

    Log.e(TAG, "getRetryDelayMsFor [" + errorCount + "@" + loadDurationMs + "]", exception);

    isOffline = (exception instanceof HttpDataSourceException);

    return (isOffline)
      ? offlineRetryDelayMs
      : super.getRetryDelayMsFor(loadErrorInfo)
    ;
  }

  @Override
  public int getMinimumLoadableRetryCount(int dataType) {
    if (isOffline)
      return Integer.MAX_VALUE;

    if (dataType == C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE)
      return Integer.MAX_VALUE;

    return retryCount;
  }
}
