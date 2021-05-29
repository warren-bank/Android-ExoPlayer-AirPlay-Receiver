package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class VideoSource {

  public final String uri;
  public final String caption;
  public final String uri_mimeType;
  public final String caption_mimeType;
  public final String referer;
  public final float startPosition;

  // static factory

  public static VideoSource createVideoSource(
    String uri,
    String caption,
    String referer,
    float startPosition
  ) {
    VideoSource videoSource = new VideoSource(uri, caption, referer, startPosition);
    return videoSource;
  }

  private VideoSource(
    String uri,
    String caption,
    String referer,
    float startPosition
  ) {
    this.uri              = uri;
    this.caption          = caption;
    this.uri_mimeType     = get_video_mimeType(uri);
    this.caption_mimeType = get_caption_mimeType(caption);
    this.referer          = referer;
    this.startPosition    = startPosition;
  }

  // Public methods.

  @Override
  public String toString() {
    return uri;
  }

  // Static helpers.

  // ===========================================================================
  // generics

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

  private static String get_fileExtension(String uri, Pattern mediatype_regex) {
    if (uri == null) return null;

    Matcher matcher = mediatype_regex.matcher(uri.toLowerCase());
    String file_ext = matcher.find()
      ? matcher.group(1)
      : null;

    return file_ext;
  }

  // ===========================================================================
  // video mime-type

  public static Pattern video_regex = Pattern.compile("\\.(mp4|mp4v|mpv|m1v|m4v|mpg|mpg2|mpeg|xvid|webm|3gp|avi|mov|mkv|ogg|ogv|ogm|m3u8|mpd|ism(?:[vc]|/manifest)?)(?:[\\?#]|$)");

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

  // ===========================================================================
  // captions mime-type

  public static Pattern caption_regex = Pattern.compile("\\.(srt|ttml|vtt|webvtt|ssa|ass)(?:[\\?#]|$)");

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

  // ===========================================================================
  // audio file-extension

  public static Pattern audio_regex = Pattern.compile("\\.(mp3|m4a|ogg|wav|flac)(?:[\\?#]|$)");

  public static String get_audio_fileExtension(String uri) {
    return get_fileExtension(uri, audio_regex);
  }

  public static boolean isAudioFileUrl(String uri) {
    String file_ext = get_audio_fileExtension(uri);
    return (file_ext != null);
  }

  // ===========================================================================
}
