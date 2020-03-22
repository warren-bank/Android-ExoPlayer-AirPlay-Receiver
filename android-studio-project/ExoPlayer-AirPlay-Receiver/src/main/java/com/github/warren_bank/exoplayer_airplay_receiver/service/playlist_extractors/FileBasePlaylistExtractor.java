package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public abstract class FileBasePlaylistExtractor {

  protected abstract boolean isParserForFile(File file);

  protected abstract void parseLine(String line, File context, ArrayList<String> matches);

  protected void preParse(File context) {}

  protected void postParse(File context, ArrayList<String> matches) {}

  public ArrayList<String> expandPlaylist(String strUrl) {
    if (!ExternalStorageUtils.isFileUri(strUrl))
      return null;

    File file = ExternalStorageUtils.getFile(strUrl);

    if (file == null)
      return null;

    // file must: exist, not be a directory, be readable
    try {
      if (!file.isFile())
        return null;
    }
    catch(Exception e) {
      return null;
    }

    if (!isParserForFile(file))
      return null;

    ArrayList<String> matches = new ArrayList<String>();
    BufferedReader in = null;

    try {
      String line;

      // read the content of the text file
      in = new BufferedReader(new FileReader(file));

      preParse(file);
      while ((line = in.readLine()) != null) {
        // `line` is one line of text; readLine() strips the newline character(s)
        parseLine(line, file, matches);
      }
      postParse(file, matches);
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
