package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.VideoSource;

public class MediaTypeUtils {

  public static boolean isVideoFileUrl(String uri) {
    return VideoSource.isVideoFileUrl(uri);
  }

  public static boolean isCaptionFileUrl(String uri) {
    return VideoSource.isCaptionFileUrl(uri);
  }

  public static boolean isAudioFileUrl(String uri) {
    return VideoSource.isAudioFileUrl(uri);
  }

}
