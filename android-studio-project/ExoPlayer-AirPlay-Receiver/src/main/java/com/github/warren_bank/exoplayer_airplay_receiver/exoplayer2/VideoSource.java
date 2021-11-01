package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.util.Util;

import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

public final class VideoSource {

  public static String DEFAULT_USER_AGENT = null;

  public final String uri;
  public final String caption;
  public final String uri_mimeType;
  public final String caption_mimeType;
  public final String referer;
  public final HashMap<String, String> reqHeadersMap;
  public final boolean useCache;
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
    if (uri == null)
      uri = "";

    if (useCache) {
      String uri_lc = ((uri.length() > 8) ? uri.substring(0, 8) : uri).toLowerCase();

      useCache = uri_lc.startsWith("http://") || uri_lc.startsWith("https://");
    }

    if ((stopPosition >= 1f) && (stopPosition <= startPosition))
      stopPosition = -1f;

    String uri_mimeType     = MediaTypeUtils.get_media_mimeType(uri);
    String caption_mimeType = MediaTypeUtils.get_caption_mimeType(caption);

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
    this.caption            = caption;
    this.uri_mimeType       = uri_mimeType;
    this.caption_mimeType   = caption_mimeType;
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

  public MediaItem getMediaItem() {
    MediaItem.Builder builder = new MediaItem.Builder();

    builder
      .setUri(uri)
      .setMimeType(uri_mimeType)
    ;

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

    return builder.build();
  }
}
