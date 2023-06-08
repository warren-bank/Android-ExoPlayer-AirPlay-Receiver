package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.UriUtils;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.Util;

import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;

public final class VideoSource {

  public static String DEFAULT_USER_AGENT = null;

  public final String uri;
  public final String uri_mimeType;
  public       String caption;
  public final String referer;
  public final HashMap<String, String> reqHeadersMap;
  public       boolean useCache;
  public final float startPosition;
  public final float stopPosition;
  public final String drm_scheme;
  public final String drm_license_server;
  public final HashMap<String, String> drmHeadersMap;

  // static factory

  public static VideoSource createVideoSource() {
    return VideoSource.createVideoSource(
      (String) null  /* uri */
    );
  }

  public static VideoSource createVideoSource(String uri) {
    return VideoSource.createVideoSource(
      uri,
      (String)  null, /* caption            */
      (String)  null, /* referer            */
      (HashMap) null, /* reqHeadersMap      */
      false,          /* useCache           */
      -1f,            /* startPosition      */
      -1f,            /* stopPosition       */
      (String)  null, /* drm_scheme         */
      (String)  null, /* drm_license_server */
      (HashMap) null  /* drmHeadersMap      */
    );
  }

  public static VideoSource createVideoSource(
    String uri,
    String caption,
    String referer,
    HashMap<String, String> reqHeadersMap,
    boolean useCache,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    HashMap<String, String> drmHeadersMap
  ) {
    return new VideoSource(uri, caption, referer, reqHeadersMap, useCache, startPosition, stopPosition, drm_scheme, drm_license_server, drmHeadersMap);
  }

  private VideoSource(
    String uri,
    String caption,
    String referer,
    HashMap<String, String> reqHeadersMap,
    boolean useCache,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    HashMap<String, String> drmHeadersMap
  ) {
    // enforce that URLs are encoded and RFC 2396-compliant
    if (!TextUtils.isEmpty(uri))
      uri = UriUtils.encodeURI(uri);
    if (!TextUtils.isEmpty(caption))
      caption = UriUtils.encodeURI(caption);
    if (!TextUtils.isEmpty(referer))
      referer = UriUtils.encodeURI(referer);

    if (uri == null)
      uri = "";

    if (useCache) {
      String uri_lc = ((uri.length() > 8) ? uri.substring(0, 8) : uri).toLowerCase();

      useCache = uri_lc.startsWith("http://") || uri_lc.startsWith("https://");
    }

    if ((stopPosition >= 1f) && (stopPosition <= startPosition))
      stopPosition = -1f;

    String uri_mimeType = MediaTypeUtils.get_media_mimeType(uri);

    if (reqHeadersMap == null)
      reqHeadersMap = new HashMap<String, String>();
    if (!TextUtils.isEmpty(referer))
      reqHeadersMap.put("referer", referer);
    if (reqHeadersMap.containsKey("referer")) {
      referer = (String) reqHeadersMap.get("referer");

      if (!reqHeadersMap.containsKey("origin")) {
        Uri refererUri = Uri.parse(referer);
        String origin  = refererUri.getScheme() + "://" + refererUri.getAuthority();
        reqHeadersMap.put("origin", origin);
      }
    }
    if (!reqHeadersMap.containsKey("range")) {
      switch (uri_mimeType) {
        case "video/mp4":
        case "video/mpeg":
        case "video/x-mkv":
        case "video/x-msvideo":
          reqHeadersMap.put("range", "bytes=0-");
          break;
      }
    }
    if (!reqHeadersMap.containsKey("user-agent") && (DEFAULT_USER_AGENT != null)) {
      reqHeadersMap.put("user-agent", DEFAULT_USER_AGENT);
    }

    this.uri                = uri;
    this.uri_mimeType       = uri_mimeType;
    this.caption            = caption;
    this.referer            = referer;
    this.reqHeadersMap      = reqHeadersMap;
    this.useCache           = useCache;
    this.startPosition      = startPosition;
    this.stopPosition       = stopPosition;
    this.drm_scheme         = drm_scheme;
    this.drm_license_server = drm_license_server;
    this.drmHeadersMap      = drmHeadersMap;
  }

  // Public methods.

  @Override
  public String toString() {
    return uri;
  }

  public void updateCaption(String caption) {
    this.caption = caption;
  }

  public void updateUseCache(boolean useCache) {
    this.useCache = useCache;
  }

  public MediaItem getMediaItem() {
    return getMediaItem(/* forOfflineDownload= */ false);
  }

  public MediaItem getMediaItem(boolean forOfflineDownload) {
    MediaItem.Builder builder = new MediaItem.Builder();

    builder.setUri(uri);

    // ignore undefined and non-standard mime-types
    if (
        !TextUtils.isEmpty(uri_mimeType)
     && !uri_mimeType.equals("application/x-rtmp")
     && !uri_mimeType.equals("application/x-rtsp")
    ) {
      builder.setMimeType(uri_mimeType);
    }

    if (!forOfflineDownload) {
      // only  clip the trailing end, when there is a stop position
      // never clip the leading  end, always seek to the start position
      if (stopPosition >= 1f) {
        builder
          .setClipEndPositionMs(
            (long) (stopPosition * 1000)
          )
          .setClipRelativeToDefaultPosition(true)
        ;
      }
    }

    if (!TextUtils.isEmpty(drm_scheme) && !TextUtils.isEmpty(drm_license_server)) {
      builder
        .setDrmUuid(Util.getDrmUuid(drm_scheme))
        .setDrmLicenseUri(drm_license_server)
        .setDrmForceDefaultLicenseUri(false)
        .setDrmSessionForClearPeriods(false)
        .setDrmPlayClearContentWithoutKey(true)
        .setDrmMultiSession(true)
      ;

      if (drmHeadersMap != null) {
        builder.setDrmLicenseRequestHeaders(drmHeadersMap);
      }
    }

    if (!forOfflineDownload) {
      setSubtitleConfigurations(builder, this);
    }

    return builder.build();
  }

  // static helper

  private static void setSubtitleConfigurations(MediaItem.Builder builder, VideoSource sample) {
    ArrayList<MediaItem.SubtitleConfiguration> subtitleConfigurations = new ArrayList<MediaItem.SubtitleConfiguration>();
    ArrayList<String> uriCaptions = null;
    Uri uri;
    String mimeType;
    String label;
    MediaItem.SubtitleConfiguration.Builder scb;

    if (!TextUtils.isEmpty(sample.caption)) {
      uriCaptions = new ArrayList<String>(1);
      uriCaptions.add(sample.caption);
    }
    else if (ExternalStorageUtils.isFileUri(sample.uri)) {
      // loading media from external storage without any captions file explicitly specified.
      // search within same directory as media file for external captions in a supported format.
      // file naming convention: "${video_filename}.*.${supported_caption_extension}"

      uriCaptions = ExternalStorageUtils.findMatchingSubtitles(sample.uri);
    }

    if ((uriCaptions != null) && !uriCaptions.isEmpty()) {
      for (String caption : uriCaptions) {
        uri      = Uri.parse(caption);
        mimeType = MediaTypeUtils.get_caption_mimeType(caption);
        label    = MediaTypeUtils.get_caption_label(caption);

        if (!TextUtils.isEmpty(mimeType)) {
          scb = new MediaItem.SubtitleConfiguration.Builder(uri);

          scb
            .setMimeType(mimeType)
            .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT);

          if (!TextUtils.isEmpty(label)) {
            scb
              .setLanguage(label)
              .setLabel(label);
          }

          subtitleConfigurations.add(
            scb.build()
          );
        }
      }
    }

    if (!subtitleConfigurations.isEmpty()) {
      builder.setSubtitleConfigurations(subtitleConfigurations);
    }
  }

}
