package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

public interface TextSynchronizer {
  long getTextOffset();
  void setTextOffset(long value);
  void addTextOffset(long value);
}
