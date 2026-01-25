package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

/*
 * references:
 *   https://github.com/androidx/media/blob/1.9.0/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultRenderersFactory.java
 *   https://github.com/androidx/media/blob/1.9.0/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/DefaultAudioSink.java
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

public class MyRenderersFactory extends DefaultRenderersFactory implements TextSynchronizer, TextFilter {
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

  // ---------------------------------------------------------------------------
  // implement: TextSynchronizer
  // ---------------------------------------------------------------------------

  @Override
  public long getTextOffset() {
    return (textRenderer != null)
      ? textRenderer.getTextOffset()
      : 0l;
  }

  @Override
  public void setTextOffset(long value) {
    if (textRenderer != null)
      textRenderer.setTextOffset(value);
  }

  @Override
  public void addTextOffset(long value) {
    if (textRenderer != null)
      textRenderer.addTextOffset(value);
  }

  // ---------------------------------------------------------------------------
  // implement: TextFilter
  // ---------------------------------------------------------------------------

  @Override
  public void setTextFilters(String[] textFilters) {
    if (textRenderer != null)
      textRenderer.setTextFilters(textFilters);
  }

  @Override
  public void addTextFilters(String[] textFilters) {
    if (textRenderer != null)
      textRenderer.addTextFilters(textFilters);
  }

}
