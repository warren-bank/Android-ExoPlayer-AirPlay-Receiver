package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import java.io.File;
import java.util.ArrayList;

public class DirectoryIndexRecursiveMediaPlaylistExtractor extends DirectoryIndexMediaPlaylistExtractor {

  protected void parseDirectory(File subdirectory, File directory, ArrayList<File> files) {
    // read list of all files in the subdirectory
    appendFilesInDirectory(subdirectory, files);
  }

}
