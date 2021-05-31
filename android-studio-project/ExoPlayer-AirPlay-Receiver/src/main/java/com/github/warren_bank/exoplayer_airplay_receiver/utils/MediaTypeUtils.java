package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MediaTypeUtils {

  // ===========================================================================
  // URI protocol

  public static boolean is_protocol(String uri, String protocol) {
    return ((uri == null) || (protocol == null))
      ? false
      : uri.toLowerCase().startsWith(
          protocol.toLowerCase() + "://"
        )
    ;
  }

  public static boolean is_protocol_rtsp(String uri) {
    return is_protocol(uri, "rtsp");
  }

  public static boolean is_protocol_file(String uri) {
    return ExternalStorageUtils.isFileUri(uri);
  }

  // ===========================================================================
  // URI path file extension

  private static String get_fileExtension(String uri, Pattern mediatype_regex) {
    if (uri == null) return null;

    Matcher matcher = mediatype_regex.matcher(uri.toLowerCase());
    String file_ext = matcher.find()
      ? matcher.group(1)
      : null;

    return file_ext;
  }

  // ===================================
  // video

  private static Pattern video_regex = Pattern.compile("\\.(mp4|mp4v|mpv|m1v|m4v|mpg|mpg2|mpeg|xvid|webm|3gp|avi|mov|mkv|ogg|ogv|ogm|m3u8|mpd|ism(?:[vc]|/manifest)?)(?:[\\?#]|$)");

  public static String get_video_fileExtension(String uri) {
    return get_fileExtension(uri, video_regex);
  }

  public static boolean isVideoFileUrl(String uri) {
    String file_ext = get_video_fileExtension(uri);
    return (file_ext != null);
  }

  public static String get_video_mimeType(String uri) {
    String mimeType = "";
    String file_ext = null;

    if (mimeType.isEmpty()) {
      if (is_protocol_rtsp(uri))
        mimeType = "application/x-rtsp";
    }

    if (mimeType.isEmpty()) {
      file_ext = get_video_fileExtension(uri);

      if (file_ext != null) {
        switch (file_ext) {
          case "mp4":
          case "mp4v":
          case "m4v":
            mimeType = "video/mp4";
            break;
          case "mpv":
            mimeType = "video/MPV";
            break;
          case "m1v":
          case "mpg":
          case "mpg2":
          case "mpeg":
            mimeType = "video/mpeg";
            break;
          case "xvid":
            mimeType = "video/x-xvid";
            break;
          case "webm":
            mimeType = "video/webm";
            break;
          case "3gp":
            mimeType = "video/3gpp";
            break;
          case "avi":
            mimeType = "video/x-msvideo";
            break;
          case "mov":
            mimeType = "video/quicktime";
            break;
          case "mkv":
            mimeType = "video/x-mkv";
            break;
          case "ogg":
          case "ogv":
          case "ogm":
            mimeType = "video/ogg";
            break;
          case "m3u8":
            mimeType = "application/x-mpegURL";
            break;
          case "mpd":
            mimeType = "application/dash+xml";
            break;
          case "ism":
          case "ism/manifest":
          case "ismv":
          case "ismc":
            mimeType = "application/vnd.ms-sstr+xml";
            break;
        }
      }
    }

    return mimeType;
  }

  // ===================================
  // audio

  private static Pattern audio_regex = Pattern.compile("\\.(mp3|m4a|ogg|wav|flac)(?:[\\?#]|$)");

  public static String get_audio_fileExtension(String uri) {
    return get_fileExtension(uri, audio_regex);
  }

  public static boolean isAudioFileUrl(String uri) {
    String file_ext = get_audio_fileExtension(uri);
    return (file_ext != null);
  }

  public static String get_audio_mimeType(String uri) {
    String file_ext = get_audio_fileExtension(uri);
    String mimeType = "";

    if (file_ext != null) {
      switch (file_ext) {
        case "mp3":
          mimeType = "audio/mpeg";
          break;
        case "m4a":
          mimeType = "audio/m4a";
          break;
        case "ogg":
          mimeType = "application/ogg";
          break;
        case "wav":
          mimeType = "audio/wav";
          break;
        case "flac":
          mimeType = "audio/flac";
          break;
      }
    }
    return mimeType;
  }

  // ===================================
  // captions

  private static Pattern caption_regex = Pattern.compile("\\.(srt|ttml|vtt|webvtt|ssa|ass)(?:[\\?#]|$)");

  public static String get_caption_fileExtension(String uri) {
    return get_fileExtension(uri, caption_regex);
  }

  public static boolean isCaptionFileUrl(String uri) {
    String file_ext = get_caption_fileExtension(uri);
    return (file_ext != null);
  }

  public static String get_caption_mimeType(String uri) {
    String file_ext = get_caption_fileExtension(uri);
    String mimeType = "";

    if (file_ext != null) {
      switch (file_ext) {
        case "srt":
          mimeType = "application/x-subrip";
          break;
        case "ttml":
          mimeType = "application/ttml+xml";
          break;
        case "vtt":
        case "webvtt":
          mimeType = "text/vtt";
          break;
        case "ssa":
        case "ass":
          mimeType = "text/x-ssa";
          break;
      }
    }
    return mimeType;
  }

  // ===================================
  // generic media

  public static String get_media_mimeType(String uri) {
    return get_media_mimeType(uri, /* include_captions= */ false);
  }

  public static String get_media_mimeType(String uri, boolean include_captions) {
    if (isVideoFileUrl(uri))
      return get_video_mimeType(uri);
    if (isAudioFileUrl(uri))
      return get_audio_mimeType(uri);
    if (include_captions && isCaptionFileUrl(uri))
      return get_caption_mimeType(uri);
    return "";
  }

  // ===========================================================================
}
