package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public abstract class BasePlaylistExtractor {

  protected abstract boolean isParserForUrl(String strUrl);

  protected abstract void parseLine(String line, URL context, ArrayList<String> matches);

  protected void preParse(URL context) {}

  protected void postParse(URL context, ArrayList<String> matches) {}

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
    if (!isParserForUrl(strUrl))
      return null;

    ArrayList<String> matches = new ArrayList<String>();
    BufferedReader in = null;

    try {
      URL url;
      String line;

      // ascii encoded
      url = new URL(strUrl);

      // Read all the text returned by the server
      in = new BufferedReader(new InputStreamReader(url.openStream(), cs));

      // remove ascii encoding
      url = new URL(StringUtils.decodeURL(strUrl));

      preParse(url);
      while ((line = in.readLine()) != null) {
        // `line` is one line of text; readLine() strips the newline character(s)
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
