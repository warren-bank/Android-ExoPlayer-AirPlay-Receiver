package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HttpM3uPlaylistExtractor extends HttpBasePlaylistExtractor {

  private static Pattern playlist_regex = Pattern.compile("\\.(?:m3u)(?:[\\?#]|$)");

  protected boolean isParserForUrl(String strUrl) {
    if (strUrl == null) return false;

    Matcher matcher = HttpM3uPlaylistExtractor.playlist_regex.matcher(strUrl.toLowerCase());
    return matcher.find();
  }

  protected void parseLine(String line, URL context, ArrayList<String> matches) {
    if (ignoreM3uLine(line)) return;

    String uri = resolveM3uPlaylistItem(context, line);
    if (uri != null)
      matches.add(uri);
  }

}
