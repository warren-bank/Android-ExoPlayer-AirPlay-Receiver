package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

public interface TimeSynchronizer {
  long getTimeOffset();
  void setTimeOffset(long value);
  void addTimeOffset(long value);
}
