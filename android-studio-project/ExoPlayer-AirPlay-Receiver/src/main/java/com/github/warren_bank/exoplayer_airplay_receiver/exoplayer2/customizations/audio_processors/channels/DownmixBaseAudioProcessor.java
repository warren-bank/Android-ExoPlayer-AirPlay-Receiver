package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.audio_processors.channels;

import androidx.media3.common.audio.NonFinalChannelMixingAudioProcessor;

public class DownmixBaseAudioProcessor extends NonFinalChannelMixingAudioProcessor {
  private boolean enabled;

  public DownmixBaseAudioProcessor() {
    super();
    this.enabled = false;
  }

  public void enable(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean isActive() {
    return this.enabled && super.isActive();
  }

  @Override
  protected AudioFormat onConfigure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
    try {
      return super.onConfigure(inputAudioFormat);
    }
    catch(UnhandledAudioFormatException e) {
      return AudioFormat.NOT_SET;
    }
  }
}
