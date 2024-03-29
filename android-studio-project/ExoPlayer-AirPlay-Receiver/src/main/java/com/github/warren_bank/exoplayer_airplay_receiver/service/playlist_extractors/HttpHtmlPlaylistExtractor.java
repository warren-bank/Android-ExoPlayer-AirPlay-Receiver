package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HttpHtmlPlaylistExtractor extends HttpBasePlaylistExtractor {

  private static Pattern linkhref_regex = Pattern.compile("(?:href|src)\\s*=\\s*[\"]([^\"]+\\.)(mp4|mp4v|mpv|m1v|m4v|mpg|mpg2|mpeg|xvid|webm|3gp|avi|mov|mkv|ogg|ogv|ogm|m3u8|mpd|ism[vc]?|mp3|m4a|ogg|wav|flac)[\"]", Pattern.CASE_INSENSITIVE);
  private static String format_priority = "|mp4|mp4v|mpv|m1v|m4v|mpg|mpg2|mpeg|xvid|webm|3gp|avi|mov|mkv|ogg|ogv|ogm|m3u8|mpd|ism|ismv|ismc|mp3|m4a|ogg|wav|flac|";

  private static int getFormatPriority(String format) {
    return HttpHtmlPlaylistExtractor.format_priority.indexOf("|" + format + "|");
  }

  private ArrayList<String> hash_keys;
  private HashMap<String, String[]> url_chunks;

  @Override
  protected void preParse(URL context) {
    hash_keys  = new ArrayList<String>();
    url_chunks = new HashMap<String, String[]>();
  }

  @Override
  protected void postParse(URL context, ArrayList<String> matches) {
    String hash_key;
    String[] val;
    String href;
    String uri;

    for (int i=0; i < hash_keys.size(); i++) {
      hash_key = hash_keys.get(i);
      val      = url_chunks.get(hash_key);
      href     = val[0] + val[1];
      uri      = resolveM3uPlaylistItem(context, href);

      if (uri != null)
        matches.add(uri);
    }

    hash_keys.clear();
    hash_keys = null;

    url_chunks.clear();
    url_chunks = null;
  }

  protected boolean isParserForUrl(String strUrl) {
    return (strUrl != null)
      ? MediaTypeUtils.isPlaylistHtmlUrl(strUrl)
      : false;
  }

  protected void parseLine(String line, URL context, ArrayList<String> matches) {
    Matcher matcher = HttpHtmlPlaylistExtractor.linkhref_regex.matcher(line);
    String m1, m2, lm1, lm2, lo2;
    int po, pm;
    String[] old;

    // find all matches in `line`
    while (matcher.find()) {
      m1 = matcher.group(1);
      m2 = matcher.group(2);

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
        hash_keys.add(lm1);
        url_chunks.put(lm1, new String[] {m1,m2});
      }
    }
  }

}
