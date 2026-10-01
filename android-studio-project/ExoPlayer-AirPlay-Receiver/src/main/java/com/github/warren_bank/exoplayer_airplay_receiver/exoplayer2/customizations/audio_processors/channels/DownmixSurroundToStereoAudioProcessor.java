package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.audio_processors.channels;

import androidx.media3.common.audio.ChannelMixingMatrix;

/*
 * references:
 *   https://github.com/androidx/media/blob/1.11.0/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L99
 *   https://github.com/androidx/media/blob/1.11.0/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L236
 *   https://github.com/androidx/media/blob/1.11.0/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L297
 */

public class DownmixSurroundToStereoAudioProcessor extends DownmixBaseAudioProcessor {
  public DownmixSurroundToStereoAudioProcessor() {
    super();

    putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(6, 2)); // 6 => stereo
    putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(5, 2)); // 5 => stereo
    putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(4, 2)); // 4 => stereo
    putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(3, 2)); // 3 => stereo
  }
}
