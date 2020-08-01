package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.upstream.HttpDataSource.HttpDataSourceException;

public class ExoPlayerUtils {

  private static boolean isInstanceOfType(ExoPlaybackException error, Class<?> assertion) {
    int type = ExoPlaybackException.TYPE_SOURCE;
    return isInstanceOfType(error, type, assertion);
  }

  private static boolean isInstanceOfType(ExoPlaybackException error, int type, Class<?> assertion) {
    if (error == null)
      return false;
    if (error.type != type)
      return false;

    Throwable cause = error.getSourceException();
    return isInstanceOfClass(cause, assertion);
  }

  private static boolean isInstanceOfClass(Throwable cause, Class<?> assertion) {
    if (cause == null)
      return false;
    if (assertion == null)
      return false;

    while (cause != null) {
      if (assertion.isInstance(cause))
        return true;

      cause = cause.getCause();
    }
    return false;
  }

  public static boolean isBehindLiveWindow(ExoPlaybackException error) {
    return isInstanceOfType(error, ExoPlaybackException.TYPE_SOURCE, BehindLiveWindowException.class);
  }

  public static boolean isHttpDataSource(ExoPlaybackException error) {
    return isInstanceOfType(error, ExoPlaybackException.TYPE_SOURCE, HttpDataSourceException.class);
  }

}
