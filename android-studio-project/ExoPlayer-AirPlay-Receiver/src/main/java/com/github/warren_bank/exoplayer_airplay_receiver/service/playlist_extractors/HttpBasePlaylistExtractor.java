package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.UrlUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public abstract class HttpBasePlaylistExtractor extends BasePlaylistExtractor {

  private static boolean isHttpUrl(String strUrl) {
    return (strUrl == null)
      ? false
      : (strUrl.toLowerCase().indexOf("http") == 0);
  }

  protected abstract boolean isParserForUrl(String strUrl);

  protected abstract void parseLine(String line, URL context, ArrayList<String> matches);

  protected void preParse(URL context) {}

  protected void postParse(URL context, ArrayList<String> matches) {}

  protected String resolveM3uPlaylistItem(URL context, String relative) {
    String uri = null;

    uri = resolveM3uPlaylistItem(
      ((context != null) ? context.toString() : ""),
      UrlUtils.decodeURL(relative)
    );

    if (uri != null)
      uri = UrlUtils.encodeURL(uri);

    return uri;
  }

  public ArrayList<String> expandPlaylist(String strUrl) {
    // https://developer.android.com/reference/java/nio/charset/Charset#standard-charsets
    // https://en.wikipedia.org/wiki/Extended_ASCII#ISO_8859_and_proprietary_adaptations
    // https://en.wikipedia.org/wiki/ISO/IEC_8859-1
    return expandPlaylist(strUrl, "ISO-8859-1");
  }

  public ArrayList<String> expandPlaylist(String strUrl, String charsetName) {
    Charset cs = null;

    if ((charsetName == null) || charsetName.isEmpty()) {
      cs = Charset.defaultCharset(); // UTF-8
    }
    else {
      try {
        cs = Charset.forName(charsetName);
      }
      catch (Exception e) {
        cs = Charset.defaultCharset(); // UTF-8
      }
    }

    return expandPlaylist(strUrl, cs);
  }

  protected ArrayList<String> expandPlaylist(String strUrl, Charset cs) {
    if (!isHttpUrl(strUrl))
      return null;

    if (!isParserForUrl(strUrl))
      return null;

    ArrayList<String> matches = new ArrayList<String>();
    BufferedReader in = null;

    try {
      URL url;
      String line;

      // ascii encoded
      url = new URL(strUrl);

      // read all the text returned by the server
      in = new BufferedReader(new InputStreamReader(url.openStream(), cs));

      // remove ascii encoding
      url = new URL(UrlUtils.decodeURL(strUrl));

      preParse(url);
      while ((line = in.readLine()) != null) {
        line = normalizeLine(line);
        if (line == null) continue;

        parseLine(line, url, matches);
      }
      postParse(url, matches);
    }
    catch (Exception e) {
    }
    finally {
      if (in != null) {
        try {
          in.close();
        }
        catch(Exception e) {}
      }

      // normalize that non-null return value must include matches
      if (matches.isEmpty())
        matches = null;
    }

    return matches;
  }

}
