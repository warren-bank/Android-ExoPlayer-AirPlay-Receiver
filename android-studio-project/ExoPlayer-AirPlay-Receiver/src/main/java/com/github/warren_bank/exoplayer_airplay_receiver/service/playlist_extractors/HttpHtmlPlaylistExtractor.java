package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HttpHtmlPlaylistExtractor extends HttpBasePlaylistExtractor {

  private static Pattern playlist_regex = Pattern.compile("(?:\\/|\\.s?html?)(?:[\\?#]|$)");
  private static Pattern linkhref_regex = Pattern.compile("href=\"([^\"]+\\.)(mp3|m4a|ogg|wav|flac)\"", Pattern.CASE_INSENSITIVE);
  private static String format_priority = "|mp3|m4a|ogg|wav|flac|";

  private static int getFormatPriority(String format) {
    return HttpHtmlPlaylistExtractor.format_priority.indexOf(format);
  }

  private HashMap<String, String[]> url_chunks;

  @Override
  protected void preParse(URL context) {
    url_chunks = new HashMap<String, String[]>();
  }

  @Override
  protected void postParse(URL context, ArrayList<String> matches) {
    String href;
    URL url;

    for (String[] val : url_chunks.values()) {
      href = val[0] + val[1];

      try {
        // if `href` contains a  relative spec, then resolve it relative to context
        // if `href` contains an absolute spec, then context is ignored
        url = new URL(context, href);

        matches.add(
          StringUtils.encodeURL(url)
        );
      }
      catch(Exception e) {}
    }

    url_chunks.clear();
    url_chunks = null;
  }

  protected boolean isParserForUrl(String strUrl) {
    if (strUrl == null) return false;

    Matcher matcher = HttpHtmlPlaylistExtractor.playlist_regex.matcher(strUrl.toLowerCase());
    return matcher.find();
  }

  protected void parseLine(String line, URL context, ArrayList<String> matches) {
    if (line == null) return;
    line = line.trim();

    // remove empty lines
    if (line.isEmpty()) return;

    Matcher matcher = HttpHtmlPlaylistExtractor.linkhref_regex.matcher(line);
    String m1, m2, lm1, lm2, lo2;
    int po, pm;
    String[] old;

    // find all matches in `line`
    while (matcher.find()) {
      m1 = matcher.group(1);
      m2 = matcher.group(2);

      // remove ascii encoding
      m1 = StringUtils.decodeURL(m1);

      lm1 = m1.toLowerCase();
      lm2 = m2.toLowerCase();

      if (url_chunks.containsKey(lm1)) {
        old = url_chunks.get(lm1);
        lo2 = old[1].toLowerCase();

        po = HttpHtmlPlaylistExtractor.getFormatPriority(lo2);
        pm = HttpHtmlPlaylistExtractor.getFormatPriority(lm2);

        if ((pm >=0) && (pm < po))
          url_chunks.put(lm1, new String[] {m1,m2});
      }
      else {
        url_chunks.put(lm1, new String[] {m1,m2});
      }
    }
  }

}
