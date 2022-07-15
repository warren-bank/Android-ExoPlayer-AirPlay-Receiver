package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FileM3uPlaylistExtractor extends FileBasePlaylistExtractor {

  private static Pattern playlist_regex = Pattern.compile("\\.(?:m3u)$");

  protected boolean isParserForFile(File file) {
    if (file == null) return false;

    Matcher matcher = FileM3uPlaylistExtractor.playlist_regex.matcher(file.getAbsolutePath().toLowerCase());
    return matcher.find();
  }

  protected void parseLine(String line, File context, ArrayList<String> matches) {
    if (ignoreM3uLine(line)) return;

    String uri = resolveM3uPlaylistItem(context, line);
    if (uri != null)
      matches.add(uri);
  }

}
