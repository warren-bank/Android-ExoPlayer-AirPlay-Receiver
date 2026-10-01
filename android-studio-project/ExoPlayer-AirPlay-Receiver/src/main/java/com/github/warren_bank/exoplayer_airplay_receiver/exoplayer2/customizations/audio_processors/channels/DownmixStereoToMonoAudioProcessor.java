package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.audio_processors.channels;

import androidx.media3.common.audio.ChannelMixingMatrix;

/*
 * references:
 *   https://github.com/androidx/media/blob/1.11.0/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L99
 *   https://github.com/androidx/media/blob/1.11.0/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L236
 *   https://github.com/androidx/media/blob/1.11.0/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L262
 */

public class DownmixStereoToMonoAudioProcessor extends DownmixBaseAudioProcessor {
  public DownmixStereoToMonoAudioProcessor() {
    super();

    putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(2, 1)); // stereo => mono
  }
}
