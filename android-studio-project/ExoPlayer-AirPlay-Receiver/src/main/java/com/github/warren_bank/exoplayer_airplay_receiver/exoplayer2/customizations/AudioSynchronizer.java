package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

public interface AudioSynchronizer extends TimeSynchronizer {
  long getAudioOffset();
  void setAudioOffset(long value);
  void addAudioOffset(long value);

  @Override
  default long getTimeOffset() {
    return getAudioOffset();
  }

  @Override
  default void setTimeOffset(long value) {
    setAudioOffset(value);
  }

  @Override
  default void addTimeOffset(long value) {
    addAudioOffset(value);
  }
}
