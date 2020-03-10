package com.github.warren_bank.exoplayer_airplay_receiver.ui.exoplayer2.customizations;

import android.content.Context;
import android.os.Looper;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.text.TextOutput;
import java.util.ArrayList;

public class MyRenderersFactory extends DefaultRenderersFactory {
  private MyTextRenderer textRenderer;

  public MyRenderersFactory(Context context) {
    super(context);
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

  public void setTextOffset(long value) {
    if (textRenderer != null)
      textRenderer.setOffsetPositionUs(value);
  }

  public void addTextOffset(long value) {
    if (textRenderer != null)
      textRenderer.addOffsetPositionUs(value);
  }
}
