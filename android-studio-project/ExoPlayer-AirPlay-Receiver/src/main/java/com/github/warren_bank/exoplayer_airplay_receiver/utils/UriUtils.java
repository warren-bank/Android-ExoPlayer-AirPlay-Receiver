package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.net.Uri;

import java.net.URI;

public class UriUtils {

  public static String encodeURI(String strUri) {
    try {
      if ((strUri == null) || strUri.isEmpty())
        throw new Exception("uri is empty");

      StringBuilder builder = new StringBuilder();
      Uri uri = Uri.parse(strUri);
      String sVal;
      int iVal;

      sVal = uri.getScheme();
      if (sVal == null)
        throw new Exception("protocol is required");
      builder.append(sVal);
      builder.append("://");

      sVal = uri.getEncodedUserInfo();
      if (sVal != null) {
        sVal = Uri.encode(sVal, "%:");
        builder.append(sVal);
        builder.append("@");
      }

      sVal = uri.getHost();
      if (sVal == null)
        throw new Exception("hostname is required");
      builder.append(sVal);

      iVal = uri.getPort();
      if (iVal > 0) {
        builder.append(":");
        builder.append(iVal);
      }

      sVal = uri.getEncodedPath();
      if (sVal == null)
        throw new Exception("path is required");
      sVal = Uri.encode(sVal, "%/");
      builder.append(sVal);

      sVal = uri.getEncodedQuery();
      if (sVal != null) {
        sVal = Uri.encode(sVal, "%=&[]");
        builder.append("?");
        builder.append(sVal);
      }

      sVal = uri.getEncodedFragment();
      if (sVal != null) {
        sVal = Uri.encode(sVal, "%/");
        builder.append("#");
        builder.append(sVal);
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
