package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.util.Util;

import android.text.TextUtils;

public final class VideoSource {

  public final String uri;
  public final String caption;
  public final String uri_mimeType;
  public final String caption_mimeType;
  public final String referer;
  public final float startPosition;
  public final float stopPosition;
  public final String drm_scheme;
  public final String drm_license_server;

  // static factory

  public static VideoSource createVideoSource() {
    return VideoSource.createVideoSource(
      (String) null  /* uri */
    );
  }

  public static VideoSource createVideoSource(String uri) {
    return VideoSource.createVideoSource(
      uri,
      (String) null, /* caption            */
      (String) null, /* referer            */
      -1f,           /* startPosition      */
      -1f,           /* stopPosition       */
      (String) null, /* drm_scheme         */
      (String) null  /* drm_license_server */
    );
  }

  public static VideoSource createVideoSource(
    String uri,
    String caption,
    String referer,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server
  ) {
    return new VideoSource(uri, caption, referer, startPosition, stopPosition, drm_scheme, drm_license_server);
  }

  private VideoSource(
    String uri,
    String caption,
    String referer,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server
  ) {
    if (uri == null)
      uri = "";

    if ((stopPosition >= 1f) && (stopPosition <= startPosition))
      stopPosition = -1f;

    this.uri                = uri;
    this.caption            = caption;
    this.uri_mimeType       = MediaTypeUtils.get_media_mimeType(uri);
    this.caption_mimeType   = MediaTypeUtils.get_caption_mimeType(caption);
    this.referer            = referer;
    this.startPosition      = startPosition;
    this.stopPosition       = stopPosition;
    this.drm_scheme         = drm_scheme;
    this.drm_license_server = drm_license_server;
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
    }

    return builder.build();
  }
}
