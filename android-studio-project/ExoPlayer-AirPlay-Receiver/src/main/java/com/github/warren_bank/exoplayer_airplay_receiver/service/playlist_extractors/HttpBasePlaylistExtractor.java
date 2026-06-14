package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.UriUtils;

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

  protected abstract void parseLine(String line, String strUrl, ArrayList<String> matches);

  protected void preParse(String strUrl) {}

  protected void postParse(String strUrl, ArrayList<String> matches) {}

  protected String resolveM3uPlaylistItem(String baseUri, String pathSegment, boolean resolveAbsolutePathToFileUri) {
    if (!resolveAbsolutePathToFileUri && !StringUtils.isEmpty(baseUri) && !StringUtils.isEmpty(pathSegment) && (pathSegment.charAt(0) == '/')) {
      pathSegment = UriUtils.resolve(baseUri, pathSegment);
      baseUri = null;
    }
    else if (!MediaTypeUtils.is_protocol_supported(pathSegment)) {
      pathSegment = UriUtils.normalizePath(pathSegment);
    }

    return resolveM3uPlaylistItem(baseUri, pathSegment);
  }

  public ArrayList<String> expandPlaylist(String strUrl) {
    // https://developer.android.com/reference/java/nio/charset/Charset#standard-charsets
    // https://en.wikipedia.org/wiki/Extended_ASCII#ISO_8859_and_proprietary_adaptations
    // https://en.wikipedia.org/wiki/ISO/IEC_8859-1
    return expandPlaylist(strUrl, "ISO-8859-1");
  }

  public ArrayList<String> expandPlaylist(String strUrl, String charsetName) {
    Charset cs = null;

    if (StringUtils.isEmpty(charsetName)) {
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

      url = new URL(strUrl);

      // read all the text returned by the server
      in = new BufferedReader(new InputStreamReader(url.openStream(), cs));

      preParse(strUrl);
      while ((line = in.readLine()) != null) {
        line = normalizeLine(line);
        if (line == null) continue;

        parseLine(line, strUrl, matches);
      }
      postParse(strUrl, matches);
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
