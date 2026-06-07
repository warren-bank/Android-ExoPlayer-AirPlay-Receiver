package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

/*
 * references:
 *   https://github.com/androidx/media/blob/1.10.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/DefaultAudioSink.java#L175-L223
 *   https://github.com/androidx/media/blob/1.10.1/libraries/common/src/main/java/androidx/media3/common/audio/BaseAudioProcessor.java#L62
 */

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.ChannelMixingMatrix;
import androidx.media3.common.audio.NonFinalChannelMixingAudioProcessor;
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain;

public class MyAudioProcessorChain extends DefaultAudioProcessorChain {
  public static MyAudioProcessorChain getInstance() {
    AudioProcessor[] audioProcessors = new AudioProcessor[] {
      new DownmixSurroundToStereoAudioProcessor(),
      new DownmixStereoToMonoAudioProcessor()
    };

    return new MyAudioProcessorChain(audioProcessors);
  }

  private DownmixSurroundToStereoAudioProcessor downmix_surround_to_stereo;
  private DownmixStereoToMonoAudioProcessor     downmix_stereo_to_mono;

  private MyAudioProcessorChain(AudioProcessor[] audioProcessors) {
    super(audioProcessors);

    downmix_surround_to_stereo = (DownmixSurroundToStereoAudioProcessor) audioProcessors[0];
    downmix_stereo_to_mono     = (DownmixStereoToMonoAudioProcessor)     audioProcessors[1];

    enable_downmix_surround_to_stereo(false);
    enable_downmix_stereo_to_mono(false);
  }

  public void enable_downmix_surround_to_stereo(boolean enabled) {
    downmix_surround_to_stereo.enable(enabled);
  }

  public void enable_downmix_stereo_to_mono(boolean enabled) {
    downmix_stereo_to_mono.enable(enabled);
  }

  // ---------------------------------------------------------------------------
  // implement: DownmixBaseAudioProcessor
  // ---------------------------------------------------------------------------

  /*
   * references:
   *   https://github.com/androidx/media/blob/1.10.1/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L99
   *   https://github.com/androidx/media/blob/1.10.1/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L236
   *   https://github.com/androidx/media/blob/1.10.1/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L262
   *   https://github.com/androidx/media/blob/1.10.1/libraries/common/src/main/java/androidx/media3/common/audio/ChannelMixingMatrix.java#L297
   */

  public static class DownmixBaseAudioProcessor extends NonFinalChannelMixingAudioProcessor {
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

  // ---------------------------------------------------------------------------
  // implement: DownmixSurroundToStereoAudioProcessor
  // ---------------------------------------------------------------------------

  public static class DownmixSurroundToStereoAudioProcessor extends DownmixBaseAudioProcessor {
    public DownmixSurroundToStereoAudioProcessor() {
      super();

      putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(6, 2)); // 6 => stereo
      putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(5, 2)); // 5 => stereo
      putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(4, 2)); // 4 => stereo
      putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(3, 2)); // 3 => stereo
    }
  }

  // ---------------------------------------------------------------------------
  // implement: DownmixStereoToMonoAudioProcessor
  // ---------------------------------------------------------------------------

  public static class DownmixStereoToMonoAudioProcessor extends DownmixBaseAudioProcessor {
    public DownmixStereoToMonoAudioProcessor() {
      super();

      putChannelMixingMatrix(ChannelMixingMatrix.createForConstantPower(2, 1)); // stereo => mono
    }
  }
}
