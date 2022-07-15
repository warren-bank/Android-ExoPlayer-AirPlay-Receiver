package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import java.io.File;
import java.util.ArrayList;

public class FileM3uPlaylistExtractor extends FileBasePlaylistExtractor {

  protected boolean isParserForFile(File file) {
    return (file != null)
      ? isParserForM3uUri(file.getAbsolutePath())
      : false;
  }

  protected void parseLine(String line, File context, ArrayList<String> matches) {
    if (ignoreM3uLine(line)) return;

    String uri = resolveM3uPlaylistItem(context, line);
    if (uri != null)
      matches.add(uri);
  }

}
