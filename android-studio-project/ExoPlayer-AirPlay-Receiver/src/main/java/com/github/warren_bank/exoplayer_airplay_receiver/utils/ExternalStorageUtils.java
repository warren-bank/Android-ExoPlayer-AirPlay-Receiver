package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.R;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ExternalStorageUtils {

  private static class CaptionsFileFilter implements FileFilter {
    private String  video_filename;
    private Pattern caption_regex;

    public CaptionsFileFilter(String video_filename) {
      int pos = video_filename.lastIndexOf('.');

      this.video_filename = (pos >= 0)
        ? video_filename.substring(0, pos + 1)
        : video_filename + ".";

      this.caption_regex = Pattern.compile("\\.(?:srt|ttml|vtt|webvtt|ssa|ass)$");
    }

    @Override
    public boolean accept(File file) {
      String caption_filename = file.getName();

      if (caption_filename.indexOf(video_filename) != 0)
        return false;

      Matcher matcher = caption_regex.matcher(caption_filename.toLowerCase());
      return matcher.find();
    }
  }

  private static Pattern file_uri_regex = Pattern.compile("^(?:/|file:/)");

  public static boolean isFileUri(String uri) {
    if (uri == null) return false;

    Matcher matcher = file_uri_regex.matcher(uri.toLowerCase());
    return matcher.find();
  }

  public static String normalizeFileUri(String uri) {
    if (uri == null)   return null;
    if (uri.isEmpty()) return null;

    if (uri.charAt(0) == '/')
      uri = (new File(uri)).toURI().toString();

    return uri;
  }

  public static File getFile(String uri) {
    uri = normalizeFileUri(uri);
    if (uri == null) return null;

    try {
      URI  u = new URI(uri);
      File f = new File(u);
      return f;
    }
    catch(Exception e) {
      return null;
    }
  }

  public static String joinFilePaths(File context, String path) {
    String basedir = context.getParent();

    if ((basedir == null) || (basedir.equals("/")))
      basedir = "";

    return basedir + "/" + path;
  }

  public static ArrayList<String> findMatchingSubtitles(String uriVideo) {
    File file = getFile(uriVideo);
    if (file == null)
      return null;
    if (!file.isFile())
      return null;

    String video_filename = file.getName();

    file = file.getParentFile();
    if (file == null)
      return null;
    if (!file.isDirectory())
      return null;

    try {
      FileFilter filter = new CaptionsFileFilter(video_filename);
      File[]   captions = file.listFiles(filter);

      if (captions == null)
        return null;
      if (captions.length == 0)
        return null;

      ArrayList<String> uriCaptions = new ArrayList<String>();
      String uri;

      for (File caption : captions) {
        uri = caption.toURI().toString();
        uriCaptions.add(uri);
      }
      return uriCaptions;
    }
    catch(Exception e) {
      return null;
    }
  }

  public static boolean has_permission(Context context) {
    if (Build.VERSION.SDK_INT < 23) {
      return true;
    } else {
      String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

      return (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }
  }

  private static int get_request_code(Context context) {
    return context.getResources().getInteger(R.integer.PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE);
  }

  public static void request_permission(Activity activity) {
    String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

    activity.requestPermissions(new String[]{permission}, get_request_code(activity));
  }

  public static boolean is_permission_granted(Activity activity, int requestCode, int[] grantResults) {
    return (
         (requestCode == get_request_code(activity))
      && (grantResults.length == 1)
      && (grantResults[0] == PackageManager.PERMISSION_GRANTED)
    );
  }

}
