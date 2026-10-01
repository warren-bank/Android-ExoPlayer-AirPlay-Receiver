package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

/*
 * references:
 *   https://github.com/androidx/media/blob/1.11.0/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/DefaultAudioSink.java#L177-L225
 *   https://github.com/androidx/media/blob/1.11.0/libraries/common/src/main/java/androidx/media3/common/audio/BaseAudioProcessor.java#L62
 */

import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.audio_processors.channels.DownmixStereoToMonoAudioProcessor;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.audio_processors.channels.DownmixSurroundToStereoAudioProcessor;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.audio_processors.sync.TimeOffsetAudioProcessor;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain;

public class MyAudioProcessorChain extends DefaultAudioProcessorChain {
  public static MyAudioProcessorChain getInstance() {
    AudioProcessor[] audioProcessors = new AudioProcessor[] {
      new DownmixSurroundToStereoAudioProcessor(),
      new DownmixStereoToMonoAudioProcessor(),
      new TimeOffsetAudioProcessor()
    };

    return new MyAudioProcessorChain(audioProcessors);
  }

  private DownmixSurroundToStereoAudioProcessor downmix_surround_to_stereo;
  private DownmixStereoToMonoAudioProcessor     downmix_stereo_to_mono;
  private TimeOffsetAudioProcessor              time_offset;

  private MyAudioProcessorChain(AudioProcessor[] audioProcessors) {
    super(audioProcessors);

    downmix_surround_to_stereo = (DownmixSurroundToStereoAudioProcessor) audioProcessors[0];
    downmix_stereo_to_mono     = (DownmixStereoToMonoAudioProcessor)     audioProcessors[1];
    time_offset                = (TimeOffsetAudioProcessor)              audioProcessors[2];

    enable_downmix_surround_to_stereo(false);
    enable_downmix_stereo_to_mono(false);
  }

  public void enable_downmix_surround_to_stereo(boolean enabled) {
    downmix_surround_to_stereo.enable(enabled);
  }

  public void enable_downmix_stereo_to_mono(boolean enabled) {
    downmix_stereo_to_mono.enable(enabled);
  }

  public void setTimeOffset(long value) {
    time_offset.setTimeOffset(value);
  }

  public void addTimeOffset(long value) {
    time_offset.addTimeOffset(value);
  }
}
