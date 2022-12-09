package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

public class UrlUtils {

  public static String decodeURL(String strUrl) {
    try {
      return URLDecoder.decode(strUrl, "UTF-8");
    }
    catch(Exception e) {
      return strUrl;
    }
  }

  public static String encodeURL(String strUrl) {
    try {
      URL url = new URL(strUrl);

      return UrlUtils.encodeURL(url);
    }
    catch(Exception e) {
      return strUrl;
    }
  }

  public static String encodeURL(URL url) {
    try {
      URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());

      return uri.toASCIIString();
    }
    catch(Exception e) {
      return url.toExternalForm();
    }
  }

}
