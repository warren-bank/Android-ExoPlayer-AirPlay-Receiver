package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;

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
    this.uri_mimeType     = MediaTypeUtils.get_media_mimeType(uri);
    this.caption_mimeType = MediaTypeUtils.get_caption_mimeType(caption);
    this.referer          = referer;
    this.startPosition    = startPosition;
  }

  // Public methods.

  @Override
  public String toString() {
    return uri;
  }
}
