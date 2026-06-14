package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import java.util.ArrayList;

public class HttpM3uPlaylistExtractor extends HttpBasePlaylistExtractor {

  protected boolean isParserForUrl(String strUrl) {
    return (strUrl != null)
      ? isParserForM3uUri(strUrl)
      : false;
  }

  protected void parseLine(String line, String strUrl, ArrayList<String> matches) {
    if (ignoreM3uLine(line)) return;

    String uri = resolveM3uPlaylistItem(strUrl, line, true);
    if (uri != null)
      matches.add(uri);
  }

}
