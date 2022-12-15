package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.net.Uri;

import java.net.URI;

public class UriUtils {

  /* =============================================
   * https://datatracker.ietf.org/doc/html/rfc3986#section-2.1
   *   pct-encoded = [%][a-fA-F0-9]{2}
   * https://datatracker.ietf.org/doc/html/rfc3986#section-2.2
   *   sub-delims  = ['()*!$&+,;=]
   * https://datatracker.ietf.org/doc/html/rfc3986#section-2.3
   *   unreserved  = [a-zA-Z0-9-._~]
   * https://datatracker.ietf.org/doc/html/rfc3986#section-3.2.1
   *   auth_chars  = unreserved + pct-encoded + sub-delims + [:]
   * https://datatracker.ietf.org/doc/html/rfc3986#section-3.3
   *   path_chars  = unreserved + pct-encoded + sub-delims + [:@]
   * https://datatracker.ietf.org/doc/html/rfc3986#section-3.4
   *   query_chars = unreserved + pct-encoded + sub-delims + [:@/?]
   * https://datatracker.ietf.org/doc/html/rfc3986#section-3.5
   *   hash_chars  = unreserved + pct-encoded + sub-delims + [:@/?]
   *
   * https://developer.android.com/reference/android/net/Uri#encode(java.lang.String,%20java.lang.String)
   *   chars allowed by default = unreserved + ['()*]
   * =============================================
   */
  private static String PCT_ENCODE  = "%";
  private static String SUB_DELIMS  = "!$&+,;=";
  private static String AUTH_CHARS  = PCT_ENCODE + SUB_DELIMS + ":";
  private static String PATH_CHARS  = PCT_ENCODE + SUB_DELIMS + ":@";
  private static String QUERY_CHARS = PATH_CHARS + "/?";
  private static String HASH_CHARS  = PATH_CHARS + "/?";

  public static String encodeURI(String strUri) {
    try {
      if (StringUtils.isEmpty(strUri))
        throw new Exception("uri is empty");

      if (!MediaTypeUtils.is_protocol_supported(strUri))
        throw new Exception("uri has an unsupported scheme");

      boolean isFile = MediaTypeUtils.is_protocol_file(strUri);

      StringBuilder builder = new StringBuilder();
      Uri uri = Uri.parse(strUri);

      String sVal;
      int iVal;

      sVal = uri.getScheme();
      if (StringUtils.isEmpty(sVal))
        throw new Exception("scheme is required");
      sVal = sVal.toLowerCase();
      builder.append(sVal);
      builder.append("://");

      if (!isFile) {
        sVal = uri.getEncodedUserInfo();
        if (!StringUtils.isEmpty(sVal)) {
          sVal = Uri.encode(sVal, AUTH_CHARS);
          builder.append(sVal);
          builder.append("@");
        }

        sVal = uri.getHost();
        if (StringUtils.isEmpty(sVal))
          throw new Exception("hostname is required");
        builder.append(sVal);

        iVal = uri.getPort();
        if (iVal > 0) {
          builder.append(":");
          builder.append(iVal);
        }
      }

      sVal = uri.getEncodedPath();
      if (StringUtils.isEmpty(sVal))
        throw new Exception("path is required");
      sVal = Uri.encode(sVal, "/" + PATH_CHARS);
      builder.append(sVal);

      if (!isFile) {
        sVal = uri.getEncodedQuery();
        if (!StringUtils.isEmpty(sVal)) {
          sVal = Uri.encode(sVal, QUERY_CHARS);
          builder.append("?");
          builder.append(sVal);
        }

        sVal = uri.getEncodedFragment();
        if (!StringUtils.isEmpty(sVal)) {
          sVal = Uri.encode(sVal, HASH_CHARS);
          builder.append("#");
          builder.append(sVal);
        }
      }

      strUri = builder.toString();

      if (strUri.isEmpty())
        throw new Exception("uri is empty");

      return strUri;
    }
    catch(Exception e) {
      return null;
    }
  }

  public static URI parseURI(String strUri) {
    try {
      strUri = UriUtils.encodeURI(strUri);

      if (strUri == null)
        throw new Exception("uri is empty");

      return new URI(strUri);
    }
    catch(Exception e) {
      return null;
    }
  }

}
