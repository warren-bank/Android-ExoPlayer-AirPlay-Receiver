package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ExternalStorageUtils {

  private static Pattern file_uri_regex = Pattern.compile("^(?:/|file:/)");

  public static boolean isFileUri(String uri) {
    if (uri == null) return false;

    Matcher matcher = ExternalStorageUtils.file_uri_regex.matcher(uri.toLowerCase());
    return matcher.find();
  }

  public static String normalizeFileUri(String uri) {
    if (uri == null)   return null;
    if (uri.isEmpty()) return null;

    if (uri.charAt(0) == '/')
      uri = (new File(uri)).toURI().toString();

    return uri;
  }

  public static boolean has_permission(Activity activity) {
    if (Build.VERSION.SDK_INT < 23) {
      return true;
    } else {
      String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

      return (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }
  }

  private static final int PERMISSIONS_REQUEST_CODE = 0;

  public static void request_permission(Activity activity) {
    String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

    activity.requestPermissions(new String[]{permission}, ExternalStorageUtils.PERMISSIONS_REQUEST_CODE);
  }

  public static boolean is_permission_granted(int requestCode, int[] grantResults) {
    return (
         (requestCode == ExternalStorageUtils.PERMISSIONS_REQUEST_CODE)
      && (grantResults.length == 1)
      && (grantResults[0] == PackageManager.PERMISSION_GRANTED)
    );
  }

}
