package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import java.io.File;
import java.util.ArrayList;

public class DirectoryIndexMediaPlaylistExtractor extends DirectoryIndexBasePlaylistExtractor {

  protected boolean isParserForDirectory(File directory) {
    return true;
  }

  protected void parseDirectory(File subdirectory, File directory, ArrayList<File> files) {
  }

  protected void parseFile(File file, File directory, ArrayList<String> matches) {
    String uri = resolvePlaylistItem(file);
    if (uri != null)
      matches.add(uri);
  }

}
