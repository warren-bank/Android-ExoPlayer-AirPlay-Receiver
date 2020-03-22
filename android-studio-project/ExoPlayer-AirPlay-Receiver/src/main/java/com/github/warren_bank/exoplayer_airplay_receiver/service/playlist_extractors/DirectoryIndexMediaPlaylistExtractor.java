package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DirectoryIndexMediaPlaylistExtractor extends DirectoryIndexBasePlaylistExtractor {

  private static Pattern playlist_media_regex = Pattern.compile("\\.(?:mp4|mp4v|mpv|m1v|m4v|mpg|mpg2|mpeg|xvid|webm|3gp|avi|mov|mkv|ogg|ogv|ogm|m3u8|mpd|ism[vc]?|mp3|m4a|ogg|wav|flac)$");

  protected boolean isParserForDirectory(File directory) {
    return true;
  }

  protected void parseDirectory(File subdirectory, File directory, ArrayList<File> files) {
  }

  protected void parseFile(File file, File directory, ArrayList<String> matches) {
    Matcher matcher = DirectoryIndexMediaPlaylistExtractor.playlist_media_regex.matcher(file.getAbsolutePath().toLowerCase());

    if (matcher.find())
      matches.add(file.getAbsolutePath());
  }

}
