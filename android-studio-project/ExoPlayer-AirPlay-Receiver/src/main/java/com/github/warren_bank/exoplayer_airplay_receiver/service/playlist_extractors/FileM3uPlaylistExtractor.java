package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;

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
    // remove comments
    if (line.charAt(0) == '#') return;

    // remove ascii encoding
    line = StringUtils.decodeURL(line);

    // convert Windows path separators
    line = line.replaceAll("[\\\\]", "/");

    try {
      // if `line` contains a  relative spec, then resolve it relative to context
      // if `line` contains an absolute spec, then context is ignored
      if (line.charAt(0) != '/') {
        line = ExternalStorageUtils.joinFilePaths(context, line);
      }

      matches.add(line);
    }
    catch(Exception e) {}
  }

}
