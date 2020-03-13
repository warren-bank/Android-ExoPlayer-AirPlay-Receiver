package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

public class StringUtils {

  public static String getValue(String textBlock, String prefix, String suffix) {
    String value = "";

    if ((prefix == null) || prefix.isEmpty())
      return value;

    int indexStart, indexEnd;

    indexStart = textBlock.indexOf(prefix);
    if (indexStart < 0)
      return value;
    indexStart += prefix.length();

    indexEnd = ((suffix == null) || suffix.isEmpty())
      ? -1
      : textBlock.indexOf(suffix, indexStart);

    value = (indexEnd < 0)
      ? textBlock.substring(indexStart)
      : textBlock.substring(indexStart, indexEnd);
    value = value.trim();

    return value;
  }

  public static String getQueryStringValue(String url, String prefix) {
    String suffix = "&";
    return StringUtils.getValue(url, prefix, suffix);
  }

  public static String getRequestBodyValue(String requestBody, String prefix) {
    String suffix = "\n";
    return StringUtils.getValue(requestBody, prefix, suffix);
  }

  public static String convertEscapedLinefeeds(String requestBody) {
    return requestBody.replaceAll("\\\\n", "\n");
  }

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

      return StringUtils.encodeURL(url);
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
