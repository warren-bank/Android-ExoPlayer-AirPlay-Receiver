package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;

import java.util.ArrayList;

public class SerializedPlaylistExtractor {
  public ArrayList<String> expandPlaylist(String strUrl) {
    ArrayList<String> matches = StringUtils.deserializeURLs(strUrl);

    return ((matches == null) || (matches.size() < 2))
      ? null
      : matches;
  }
}
