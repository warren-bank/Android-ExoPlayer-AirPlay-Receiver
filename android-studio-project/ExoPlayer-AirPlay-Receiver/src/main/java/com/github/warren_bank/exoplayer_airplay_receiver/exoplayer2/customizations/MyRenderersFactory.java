package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

/*
 * references:
 *   https://github.com/androidx/media/blob/1.2.0/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultRenderersFactory.java
 */

import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.ExoPlayerUtils;

import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.text.TextOutput;

import android.content.Context;
import android.os.Looper;

import java.util.ArrayList;

public class MyRenderersFactory extends DefaultRenderersFactory implements TextSynchronizer {
  private MyTextRenderer textRenderer;

  public MyRenderersFactory(Context context, boolean preferExtensionRenderer) {
    super(context);
    setExtensionRendererMode(/* int extensionRendererMode = */ ExoPlayerUtils.getExtensionRendererMode(preferExtensionRenderer));
    textRenderer = null;
  }

  protected void buildTextRenderers(
    Context context,
    TextOutput output,
    Looper outputLooper,
    int extensionRendererMode,
    ArrayList<Renderer> out
  ) {
    textRenderer = new MyTextRenderer(output, outputLooper);
    out.add(textRenderer);
  }

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
}
