package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import java.net.URI;

public class UriUtils {

  public static URI parseURI(String strUrl) {
    try {
      return new URI(StringUtils.encodeURL(strUrl));
    }
    catch(Exception e) {
      return null;
    }
  }

}
