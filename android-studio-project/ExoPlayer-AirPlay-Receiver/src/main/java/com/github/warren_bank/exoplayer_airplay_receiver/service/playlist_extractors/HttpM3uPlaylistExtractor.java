package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import java.net.URL;
import java.util.ArrayList;

public class HttpM3uPlaylistExtractor extends HttpBasePlaylistExtractor {

  protected boolean isParserForUrl(String strUrl) {
    return (strUrl != null)
      ? isParserForM3uUri(strUrl)
      : false;
  }

  protected void parseLine(String line, URL context, ArrayList<String> matches) {
    if (ignoreM3uLine(line)) return;

    String uri = resolveM3uPlaylistItem(context, line);
    if (uri != null)
      matches.add(uri);
  }

}
