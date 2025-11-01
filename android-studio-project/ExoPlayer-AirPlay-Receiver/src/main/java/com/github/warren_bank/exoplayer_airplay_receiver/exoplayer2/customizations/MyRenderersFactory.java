package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

/*
 * references:
 *   https://github.com/androidx/media/blob/1.8.0/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultRenderersFactory.java
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
  private MyTextRenderer textRenderer;
  public MyAudioProcessorChain audioProcessorChain;

  public MyRenderersFactory(Context context, boolean preferExtensionRenderer) {
    super(context);
    setExtensionRendererMode(/* int extensionRendererMode = */ ExoPlayerUtils.getExtensionRendererMode(preferExtensionRenderer));

    textRenderer = null;
    audioProcessorChain = MyAudioProcessorChain.getInstance();
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
  protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioTrackPlaybackParams) {
    return new DefaultAudioSink.Builder(context)
      .setEnableFloatOutput(enableFloatOutput)
      .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
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
