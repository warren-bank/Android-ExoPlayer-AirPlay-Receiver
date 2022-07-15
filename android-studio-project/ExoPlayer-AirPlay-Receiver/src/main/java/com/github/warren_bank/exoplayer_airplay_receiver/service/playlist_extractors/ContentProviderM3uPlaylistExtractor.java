package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;

public class ContentProviderM3uPlaylistExtractor extends ContentProviderBasePlaylistExtractor {

  public ContentProviderM3uPlaylistExtractor(Context context) {
    super(context);
  }

  protected boolean isParserForUri(Uri uri) {
    return (uri != null)
      ? isParserForM3uUri(uri.toString())
      : false;
  }

  protected void parseLine(String line, Uri context, ArrayList<String> matches) {
    if (ignoreM3uLine(line)) return;

    String uri = resolveM3uPlaylistItem(context, line);
    if (uri != null)
      matches.add(uri);
  }

}
