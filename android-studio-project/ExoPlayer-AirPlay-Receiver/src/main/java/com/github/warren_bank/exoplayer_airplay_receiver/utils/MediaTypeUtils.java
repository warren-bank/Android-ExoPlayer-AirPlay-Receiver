package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/*
 * reference:
 *   https://github.com/androidx/media/blob/1.2.0/libraries/common/src/main/java/androidx/media3/common/MimeTypes.java
 */

public class MediaTypeUtils {

  // ===========================================================================
  // URI protocol

  public static boolean is_protocol(String uri, String[] protocols) {
    if ((uri == null) || uri.isEmpty() || (protocols == null) || (protocols.length == 0))
      return false;

    uri = uri.toLowerCase();

    for (String protocol : protocols) {
      if (protocol == null)
        continue;
      if (uri.startsWith(protocol.toLowerCase() + ":"))
        return true;
    }
    return false;
  }

  public static boolean is_protocol(String uri, String protocol) {
    return is_protocol(uri, new String[]{protocol});
  }

  public static boolean is_protocol_rtmp(String uri) {
    return is_protocol(uri, "rtmp");
  }

  public static boolean is_protocol_rtsp(String uri) {
    return is_protocol(uri, "rtsp");
  }

  public static boolean is_protocol_file(String uri) {
    return ExternalStorageUtils.isFileUri(uri);
  }

  public static boolean is_protocol_supported(String uri) {
    return is_protocol(uri, new String[]{"http","https","rtmp","rtsp","file","content"}) || is_protocol_file(uri);
  }

  // ===========================================================================
  // URI path file extension

  private static String get_fileExtension(String uri, Pattern mediatype_regex) {
    return get_fileExtension(uri, mediatype_regex, /* capture_group_index= */ 1);
  }

  private static String get_fileExtension(String uri, Pattern mediatype_regex, int capture_group_index) {
    if (uri == null) return null;

    Matcher matcher = mediatype_regex.matcher(uri.toLowerCase());
    String file_ext = matcher.find()
      ? matcher.group(capture_group_index)
      : null;

    return file_ext;
  }

  // ===================================
  // video

  private static Pattern video_regex = Pattern.compile("\\.(mp4|mp4v|m4v|f4v|mpeg|mpg[2]?|m1v|webm|og[gvm]|mkv|mov|flv|xvid|avi|mp4[23]|mjp[e]?g|mpv|ts[v]?|m2t[s]?|m4s|3gp[p]?|m3u8|mpd|ism(?:[vc]|/manifest)?)(?:[\\?#]|$)");



  public static String get_video_fileExtension(String uri) {
    return get_fileExtension(uri, video_regex);
  }

  public static boolean isVideoFileUrl(String uri) {
    if (!is_protocol_supported(uri)) return false;

    String file_ext = get_video_fileExtension(uri);
    return (
        (file_ext != null)
     || is_protocol_rtmp(uri)
     || is_protocol_rtsp(uri)
    );
  }

  public static String get_video_mimeType(String uri) {
    String mimeType = "";
    String file_ext = null;

    // define a non-standard mime-type for the purpose of unique identification
    if (mimeType.isEmpty()) {
      if (is_protocol_rtmp(uri))
        mimeType = "application/x-rtmp";
    }

    // define a non-standard mime-type for the purpose of unique identification
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
          case "f4v":
            mimeType = "video/mp4";
            break;
          case "mpeg":
          case "mpg":
          case "mpg2":
          case "m1v":
            mimeType = "video/mpeg";
            break;
          case "webm":
            mimeType = "video/webm";
            break;
          case "ogg":
          case "ogv":
          case "ogm":
            mimeType = "video/ogg";
            break;
          case "mkv":
            mimeType = "video/x-mkv";
            break;
          case "mov":
            mimeType = "video/quicktime";
            break;
          case "flv":
            mimeType = "video/x-flv";
            break;
          case "xvid":
            mimeType = "video/x-xvid";
            break;
          case "avi":
            mimeType = "video/x-msvideo";
            break;
          case "mp42":
            mimeType = "video/mp42";
            break;
          case "mp43":
            mimeType = "video/mp43";
            break;
          case "mjpg":
          case "mjpeg":
            mimeType = "video/mjpeg";
            break;
          case "mpv":
            mimeType = "video/MPV";
            break;
          case "ts":
          case "tsv":
          case "m2t":
          case "m2ts":
            mimeType = "video/mp2t";
            break;
          case "m4s":
            mimeType = "video/iso.segment";
            break;
          case "3gp":
          case "3gpp":
            mimeType = "video/3gpp";
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

  private static Pattern audio_regex = Pattern.compile("\\.(mp3|ogg|wav|flac|aac|m4[ab]|f4[ab]|tsa|amr|3ga)(?:[\\?#]|$)");

  public static String get_audio_fileExtension(String uri) {
    return get_fileExtension(uri, audio_regex);
  }

  public static boolean isAudioFileUrl(String uri) {
    if (!is_protocol_supported(uri)) return false;

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
        case "ogg":
          mimeType = "application/ogg";
          break;
        case "wav":
          mimeType = "audio/wav";
          break;
        case "flac":
          mimeType = "audio/flac";
          break;
        case "aac":
        case "m4a":
        case "m4b":
        case "f4a":
        case "f4b":
          mimeType = "audio/mp4a-latm";
          break;
        case "tsa":
          mimeType = "audio/mp2t";
          break;
        case "amr":
          mimeType = "audio/amr";
          break;
        case "3ga":
          mimeType = "audio/3gpp";
          break;
      }
    }
    return mimeType;
  }

  // ===================================
  // captions

  private static Pattern caption_regex = Pattern.compile("(?:\\.([^\\./]+))?\\.(srt|ttml[12]?|dfxp|vtt|webvtt|ssa|ass)(?:[\\?#]|$)");

  public static String get_caption_label(String uri) {
    return get_fileExtension(uri, caption_regex, /* capture_group_index= */ 1);
  }

  public static String get_caption_fileExtension(String uri) {
    return get_fileExtension(uri, caption_regex, /* capture_group_index= */ 2);
  }

  public static boolean isCaptionFileUrl(String uri) {
    if (!is_protocol_supported(uri)) return false;

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
        case "ttml1":
        case "ttml2":
        case "dfxp":
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
  // playlists

  private static Pattern playlist_htm_regex = Pattern.compile("(?:\\/|\\.(?:s?html?))(?:[\\?#]|$)");
  private static Pattern playlist_m3u_regex = Pattern.compile("\\.(?:m3u)(?:[\\?#]|$)");

  public static boolean isPlaylistFileUrl(String uri) {
    return isPlaylistHtmlUrl(uri) || isPlaylistM3uUrl(uri);
  }

  public static boolean isPlaylistHtmlUrl(String uri) {
    if (!is_protocol_supported(uri)) return false;

    Matcher matcher = playlist_htm_regex.matcher(uri);
    return matcher.find();
  }

  public static boolean isPlaylistM3uUrl(String uri) {
    if (!is_protocol_supported(uri)) return false;

    Matcher matcher = playlist_m3u_regex.matcher(uri);
    return matcher.find();
  }

  // ===================================
  // generic media

  public static String get_media_mimeType(String uri) {
    return get_media_mimeType(uri, /* include_captions= */ false);
  }

  public static String get_media_mimeType(String uri, boolean include_captions) {
    if (!is_protocol_supported(uri)) return "";

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
