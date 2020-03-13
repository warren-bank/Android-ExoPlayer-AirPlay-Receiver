package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class M3uPlaylistExtractor extends BasePlaylistExtractor {

  private static Pattern playlist_regex = Pattern.compile("\\.(?:m3u)(?:[\\?#]|$)");

  protected boolean isParserForUrl(String strUrl) {
    if (strUrl == null) return false;

    Matcher matcher = M3uPlaylistExtractor.playlist_regex.matcher(strUrl.toLowerCase());
    return matcher.find();
  }

  protected void parseLine(String line, URL context, ArrayList<String> matches) {
    if (line == null) return;
    line = line.trim();

    // remove empty lines
    if (line.isEmpty()) return;

    // remove comments
    if (line.charAt(0) == '#') return;

    // remove ascii encoding
    line = StringUtils.decodeURL(line);

    // not sure if this is a good idea.. convert Windows path separators
    line = line.replaceAll("[\\\\]", "/");

    try {
      // if `line` contains a  relative spec, then resolve it relative to context
      // if `line` contains an absolute spec, then context is ignored
      URL url = new URL(context, line);

      matches.add(
        StringUtils.encodeURL(url)
      );
    }
    catch(Exception e) {}
  }

}
