package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;

import java.io.File;
import java.util.ArrayList;

public abstract class DirectoryIndexBasePlaylistExtractor {

  protected abstract boolean isParserForDirectory(File directory);

  protected abstract void parseFile(File file, File directory, ArrayList<String> matches);

  protected void preParse(File directory) {}

  protected void postParse(File directory, ArrayList<String> matches) {}

  public ArrayList<String> expandPlaylist(String strUrl) {
    if (!ExternalStorageUtils.isFileUri(strUrl))
      return null;

    File directory = ExternalStorageUtils.getFile(strUrl);

    if (directory == null)
      return null;

    // file must: exist, be a directory
    try {
      if (!directory.isDirectory())
        return null;
    }
    catch(Exception e) {
      return null;
    }

    if (!isParserForDirectory(directory))
      return null;

    ArrayList<String> matches = new ArrayList<String>();

    try {
      // read list of all files in the directory
      File[] files = directory.listFiles();

      preParse(directory);
      if (files != null) {
        for (File file : files) {
          if (file.isFile())
            parseFile(file, directory, matches);
        }
      }
      postParse(directory, matches);
    }
    catch (Exception e) {
    }
    finally {
      // normalize that non-null return value must include matches
      if (matches.isEmpty())
        matches = null;
    }

    return matches;
  }

}
