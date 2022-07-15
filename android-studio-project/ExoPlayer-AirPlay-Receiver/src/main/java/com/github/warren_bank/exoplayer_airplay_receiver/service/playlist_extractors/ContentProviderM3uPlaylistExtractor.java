package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ContentProviderM3uPlaylistExtractor extends ContentProviderBasePlaylistExtractor {

  public ContentProviderM3uPlaylistExtractor(Context context) {
    super(context);
  }

  private static Pattern playlist_regex = Pattern.compile("\\.(?:m3u)$");

  protected boolean isParserForUri(Uri uri) {
    if (uri == null) return false;

    Matcher matcher = ContentProviderM3uPlaylistExtractor.playlist_regex.matcher(uri.toString().toLowerCase());
    return matcher.find();
  }

  protected void parseLine(String line, Uri context, ArrayList<String> matches) {
    if (ignoreM3uLine(line)) return;

    String uri = resolveM3uPlaylistItem(context, line);
    if (uri != null)
      matches.add(uri);
  }

}
