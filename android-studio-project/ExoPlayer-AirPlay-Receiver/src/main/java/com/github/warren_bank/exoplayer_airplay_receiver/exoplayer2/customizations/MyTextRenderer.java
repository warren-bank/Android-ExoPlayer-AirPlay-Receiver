package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

import androidx.annotation.Nullable;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.exoplayer.text.NonFinalTextRenderer;
import androidx.media3.exoplayer.text.SubtitleDecoderFactory;
import androidx.media3.exoplayer.text.TextOutput;

import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MyTextRenderer extends NonFinalTextRenderer implements TextSynchronizer, TextFilter {
  private long offsetPositionUs;
  private List<Pattern> regexFilters;

  public MyTextRenderer(TextOutput output, @Nullable Looper outputLooper) {
    super(output, outputLooper);
    this.offsetPositionUs = 0l;
    this.regexFilters = new ArrayList<Pattern>();
  }

  public MyTextRenderer(TextOutput output, @Nullable Looper outputLooper, SubtitleDecoderFactory decoderFactory) {
    super(output, outputLooper, decoderFactory);
    this.offsetPositionUs = 0l;
    this.regexFilters = new ArrayList<Pattern>();
  }

  // ---------------------------------------------------------------------------
  // implement: TextSynchronizer
  // ---------------------------------------------------------------------------

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

  // ---------------------------------------------------------------------------
  // implement: TextFilter
  // ---------------------------------------------------------------------------

  @Override
  public void setTextFilters(String[] textFilters) {
    regexFilters.clear();
    addTextFilters(textFilters);
  }

  @Override
  public void addTextFilters(String[] textFilters) {
    if (textFilters == null) return;
    if (textFilters.length == 0) return;

    for (String textFilter : textFilters) {
      try {
        Pattern regexFilter = Pattern.compile(textFilter);
        regexFilters.add(regexFilter);
      }
      catch(Exception e) {}
    }
  }

  // ---------------------------------------------------------------------------
  // override: NonFinalTextRenderer
  // ---------------------------------------------------------------------------

  @Override
  public void render(long positionUs, long elapsedRealtimeUs) {
    positionUs        += offsetPositionUs;
    elapsedRealtimeUs += offsetPositionUs;

    super.render(positionUs, elapsedRealtimeUs);
  }

  @Override
  protected void invokeUpdateOutputInternal(CueGroup oldCueGroup) {
    CueGroup newCueGroup = oldCueGroup;

    if (!regexFilters.isEmpty()) {
      List<Cue> newCues = new ArrayList<Cue>();

      for (Cue oldCue : oldCueGroup.cues) {
        if (oldCue.text == null) {
          newCues.add(oldCue);
        }
        else {
          String newText = oldCue.text.toString().trim();

          for (Pattern pattern : regexFilters) {
            newText = pattern.matcher(newText).replaceAll("").trim();
          }

          if (!newText.isEmpty()) {
            Cue newCue = oldCue.buildUpon().setText(newText).build();
            newCues.add(newCue);
          }
        }
      }

      newCueGroup = new CueGroup(
        newCues,
        oldCueGroup.presentationTimeUs
      );
    }

    super.invokeUpdateOutputInternal(newCueGroup);
  }

}
