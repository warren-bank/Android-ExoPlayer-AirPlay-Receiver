package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.source.ClippingMediaSource;
import androidx.media3.exoplayer.source.MediaSource;

public class MediaSourceUtils {

  public static MediaSource applyClippingProperties(MediaSource mediaSource, MediaItem mediaItem) {
    MediaItem.ClippingProperties props = mediaItem.clippingProperties;

    return ((props.startPositionMs == 0) && (props.endPositionMs == C.TIME_END_OF_SOURCE))
      ? mediaSource
      : new ClippingMediaSource(
          /* mediaSource=                 */ mediaSource,
          /* startPositionUs=             */ (props.startPositionMs * 1000),
          /* endPositionUs=               */ (props.endPositionMs == C.TIME_END_OF_SOURCE) ? C.TIME_END_OF_SOURCE : (props.endPositionMs * 1000),
          /* enableInitialDiscontinuity=  */ true,
          /* allowDynamicClippingUpdates= */ false,
          /* relativeToDefaultPosition=   */ props.relativeToDefaultPosition
        )
    ;
  }

}
