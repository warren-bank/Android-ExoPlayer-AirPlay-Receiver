package com.github.warren_bank.exoplayer_airplay_receiver.httpcore;

public abstract class PlaybackStatusMonitor {
  protected static volatile boolean playbackFinished = true;
  protected static volatile long    duration    = 0l;
  protected static volatile long    curPosition = 0l;

  public static boolean isPlaybackFinished() {
    return playbackFinished;
  }

  public static long getDuration() {
    return duration;
  }

  public static long getCurrentPosition() {
    return curPosition;
  }

  public PlaybackStatusMonitor() {
  }

  public abstract void start();
  public abstract void stop();
}
