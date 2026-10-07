package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

/*
 * references:
 *   https://github.com/androidx/media/blob/1.11.0/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultRenderersFactory.java
 *   https://github.com/androidx/media/blob/1.11.0/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/DefaultAudioSink.java
 */

import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.ExoPlayerUtils;

import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.text.TextOutput;

import android.content.Context;
import android.os.Looper;

import java.util.ArrayList;

public class MyRenderersFactory extends DefaultRenderersFactory {
  private boolean useDefaultAudioCapabilities;
  private MyTextRenderer textRenderer;
  public MyAudioProcessorChain audioProcessorChain;

  public MyRenderersFactory(Context context, boolean preferExtensionRenderer, boolean useDefaultAudioCapabilities) {
    super(context);

    setEnableAudioFloatOutput(false);
    setEnableAudioOutputPlaybackParameters(false);
    setEnableDecoderFallback(true);
    setExtensionRendererMode(/* int extensionRendererMode = */ ExoPlayerUtils.getExtensionRendererMode(preferExtensionRenderer));

    this.useDefaultAudioCapabilities = useDefaultAudioCapabilities;
    this.textRenderer = null;
    this.audioProcessorChain = MyAudioProcessorChain.getInstance();
  }

  @Override
  protected void buildTextRenderers(
    Context context,
    TextOutput output,
    Looper outputLooper,
    int extensionRendererMode,
    ArrayList<Renderer> out
  ) {
    textRenderer = new MyTextRenderer(output, outputLooper);
    textRenderer.experimentalSetLegacyDecodingEnabled(true);
    out.add(textRenderer);
  }

  @Override
  protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioOutputPlaybackParams) {
    DefaultAudioSink.Builder builder = useDefaultAudioCapabilities
      ? new DefaultAudioSink.Builder()
      : new DefaultAudioSink.Builder(context);

    return builder
      .setEnableFloatOutput(enableFloatOutput)
      .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
      .setAudioProcessorChain(audioProcessorChain)
      .build();
  }

  public AudioSynchronizer getAudioSynchronizer() {
    return (AudioSynchronizer) audioProcessorChain;
  }

  public TextSynchronizer getTextSynchronizer() {
    return (TextSynchronizer) textRenderer;
  }

  public TextFilter getTextFilter() {
    return (TextFilter) textRenderer;
  }

}
