package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.ExoPlayerUtils;

import android.content.Context;
import android.os.Looper;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.text.TextOutput;
import java.util.ArrayList;

public class MyRenderersFactory extends DefaultRenderersFactory implements TextSynchronizer {
  private MyTextRenderer textRenderer;

  public MyRenderersFactory(Context context, boolean preferExtensionRenderer) {
    super(
      context,
      /* int extensionRendererMode = */ ExoPlayerUtils.getExtensionRendererMode(preferExtensionRenderer)
    );
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
