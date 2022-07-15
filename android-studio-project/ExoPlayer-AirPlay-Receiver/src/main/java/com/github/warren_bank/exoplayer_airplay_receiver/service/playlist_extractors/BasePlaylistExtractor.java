package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;

public class BasePlaylistExtractor {

  protected String normalizeLine(String line) {
    if (line == null) return null;

    // remove utf-8 BOM
    line = line.replace("\uFEFF", "");

    // trim surrounding whitespace
    line = line.trim();

    // ignore empty lines
    if (line.isEmpty()) return null;

    return line;
  }

  protected boolean isParserForM3uUri(String uri) {
    return MediaTypeUtils.isPlaylistM3uUrl(uri);
  }

  protected boolean ignoreM3uLine(String line) {
    return (line == null) || (line.charAt(0) == '#');
  }

  protected String resolveM3uPlaylistItem(String context, String relative) {
    String uri = null;

    if ((relative == null) || relative.isEmpty())
      return uri;

    if (MediaTypeUtils.is_protocol_supported(relative)) {
      uri = relative;
    }
    else if (MediaTypeUtils.is_protocol_supported(context)) {
      int index = context.lastIndexOf('/');

      if (index >= 0) {
        uri = context.substring(0, index + 1) + relative;
      }
    }

    if (uri != null) {
      if (ExternalStorageUtils.isFileUri(uri))
        uri = ExternalStorageUtils.normalizeFileUri(uri);

      if (!MediaTypeUtils.isVideoFileUrl(uri) && !MediaTypeUtils.isAudioFileUrl(uri) && !MediaTypeUtils.isPlaylistFileUrl(uri))
        uri = null;
    }

    return uri;
  }

}
