package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class DirectoryIndexBasePlaylistExtractor {

  protected abstract boolean isParserForDirectory(File directory);

  protected abstract void parseDirectory(File subdirectory, File directory, ArrayList<File> files);

  protected abstract void parseFile(File file, File directory, ArrayList<String> matches);

  protected void preParse(File directory) {}

  protected void postParse(File directory, ArrayList<String> matches) {}

  protected void appendFilesInDirectory(File directory, ArrayList<File> files) {
    if (!directory.isDirectory()) return;

    File[] filesArray = directory.listFiles();
    if (filesArray == null) return;
    if (filesArray.length == 0) return;

    Arrays.sort(filesArray);

    files.addAll(
      Arrays.asList(filesArray)
    );
  }

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

    ArrayList<File>   files   = new ArrayList<File>();
    ArrayList<String> matches = new ArrayList<String>();

    try {
      File file;

      // read list of all files in the directory
      appendFilesInDirectory(directory, files);

      preParse(directory);
      while(files.size() > 0) {
        file = files.remove(0);

        if (file.isDirectory())
          parseDirectory(file, directory, files);

        if (file.isFile())
          parseFile(file, directory, matches);
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
