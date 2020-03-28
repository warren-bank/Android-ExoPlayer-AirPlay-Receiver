package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

import android.os.Looper;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.text.SubtitleDecoderFactory;
import com.google.android.exoplayer2.text.TextOutput;

public class MyTextRenderer extends NonFinalTextRenderer implements TextSynchronizer {
  private long offsetPositionUs;

  public MyTextRenderer(TextOutput output, @Nullable Looper outputLooper) {
    super(output, outputLooper);
    this.offsetPositionUs = 0l;
  }

  public MyTextRenderer(TextOutput output, @Nullable Looper outputLooper, SubtitleDecoderFactory decoderFactory) {
    super(output, outputLooper, decoderFactory);
    this.offsetPositionUs = 0l;
  }

  @Override
  public long getTextOffset() {
    return offsetPositionUs;
  }

  @Override
  public void setTextOffset(long value) {
    offsetPositionUs = value;
  }

  @Override
  public void addTextOffset(long value) {
    offsetPositionUs += value;
  }

  @Override
  public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
    positionUs        += offsetPositionUs;
    elapsedRealtimeUs += offsetPositionUs;

    super.render(positionUs, elapsedRealtimeUs);
  }
}
