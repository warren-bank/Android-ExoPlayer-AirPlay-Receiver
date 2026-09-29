package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;

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

  private static class VideoFileFilter implements FileFilter {
    private String  video_filename;
    private Pattern filename_regex;

    public VideoFileFilter(String video_filename, String filename_regex_pattern) {
      int pos = video_filename.lastIndexOf('.');

      this.video_filename = (pos >= 0)
        ? video_filename.substring(0, pos + 1)
        : video_filename + ".";

      this.filename_regex = Pattern.compile(filename_regex_pattern.toLowerCase());
    }

    @Override
    public boolean accept(File subject_file) {
      String subject_filename = subject_file.getName();

      if (subject_filename.indexOf(video_filename) != 0)
        return false;

      Matcher matcher = filename_regex.matcher(subject_filename.toLowerCase());
      return matcher.find();
    }
  }

  private static class CaptionsFileFilter extends VideoFileFilter {
    public CaptionsFileFilter(String video_filename) {
      super(video_filename, "\\.(?:srt|ttml[12]?|dfxp|vtt|webvtt|ssa|ass)$");
    }
  }

  private static class AudioFileFilter extends VideoFileFilter {
    public AudioFileFilter(String video_filename) {
      super(video_filename, "\\.(?:mp3|ogg|wav|flac|aac|m4[ab]|f4[ab]|tsa|amr|3ga)$");
    }
  }

  public static boolean isContentUri(String uri) {
    if (uri == null) return false;

    return uri.toLowerCase().startsWith("content:");
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
    return findMatchingFiles(uriVideo, /* filter_captions */ true, /* filter_audio */ false);
  }

  public static ArrayList<String> findMatchingAudioFiles(String uriVideo) {
    return findMatchingFiles(uriVideo, /* filter_captions */ false, /* filter_audio */ true);
  }

  private static ArrayList<String> findMatchingFiles(String uriVideo, boolean filter_captions, boolean filter_audio) {
    File file = getFile(uriVideo);
    if (file == null)
      return null;
    if (!file.isFile())
      return null;
    if (!filter_captions && !filter_audio)
      return null;

    String video_filename = file.getName();

    file = file.getParentFile();
    if (file == null)
      return null;
    if (!file.isDirectory())
      return null;

    try {
      FileFilter filter = null;

      if (filter_captions)
        filter = new CaptionsFileFilter(video_filename);
      else if (filter_audio)
        filter = new AudioFileFilter(video_filename);

      File[] matchingFiles = file.listFiles(filter);

      if (matchingFiles == null)
        return null;
      if (matchingFiles.length == 0)
        return null;

      ArrayList<String> matchingURIs = new ArrayList<String>();
      String uri;

      for (File matchingFile : matchingFiles) {
        uri = matchingFile.toURI().toString();
        matchingURIs.add(uri);
      }
      return matchingURIs;
    }
    catch(Exception e) {
      return null;
    }
  }

  // convenience method
  public static boolean has_permission(Context context) {
    return RuntimePermissionUtils.hasAllPermissions(context, Constant.PermissionRequestCode.READ_EXTERNAL_STORAGE) && RuntimePermissionUtils.hasFilePermissions();
  }

}
