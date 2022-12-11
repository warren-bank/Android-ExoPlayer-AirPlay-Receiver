package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.net.Uri;

import java.net.URI;

public class UriUtils {

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
        throw new Exception("protocol is required");
      sVal = sVal.toLowerCase();
      builder.append(sVal);
      builder.append("://");

      if (!isFile) {
        sVal = uri.getEncodedUserInfo();
        if (!StringUtils.isEmpty(sVal)) {
          sVal = Uri.encode(sVal, "%:");
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
      sVal = Uri.encode(sVal, "%/");
      builder.append(sVal);

      if (!isFile) {
        sVal = uri.getEncodedQuery();
        if (!StringUtils.isEmpty(sVal)) {
          sVal = Uri.encode(sVal, "%=&[]");
          builder.append("?");
          builder.append(sVal);
        }

        sVal = uri.getEncodedFragment();
        if (!StringUtils.isEmpty(sVal)) {
          sVal = Uri.encode(sVal, "%/");
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
