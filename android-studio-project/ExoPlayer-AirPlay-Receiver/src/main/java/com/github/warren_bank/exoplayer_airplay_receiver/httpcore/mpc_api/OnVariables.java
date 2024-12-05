package com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api;

/*
 * based on:
 *   https://github.com/mpc-hc/mpc-hc/blob/1.7.13/src/mpc-hc/res/web/variables.html
 *   https://github.com/mpc-hc/mpc-hc/blob/1.7.13/src/mpc-hc/WebClientSocket.cpp#L694-L752
 *   https://github.com/mpc-hc/mpc-hc/blob/1.7.13/src/mpc-hc/mpc-hc.rc#L2647
 */

import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.RequestListenerThread.PlaybackInfoSource;

import org.json.JSONObject;

import android.net.Uri;
import android.text.TextUtils;

public class OnVariables {

  public static String getHtml(PlaybackInfoSource playbackInfoSource) {
    StringBuilder sb = new StringBuilder();

    appendHtmlPrefix(sb);
    appendHtmlVariables(sb, playbackInfoSource);
    appendHtmlDebug(sb, playbackInfoSource);
    appendHtmlSuffix(sb);

    return sb.toString();
  }

  private static void appendHtmlPrefix(StringBuilder sb) {
    sb.append("<!DOCTYPE html>\n");
    sb.append("<html lang=\"en\">\n");
    sb.append("  <head>\n");
    sb.append("    <meta charset=\"utf-8\">\n");
    sb.append("    <title>MPC-HC WebServer - Variables</title>\n");
    sb.append("  </head>\n");
    sb.append("  <body class=\"page-variables\">\n");
  }

  private static void appendHtmlSuffix(StringBuilder sb) {
    sb.append("  </body>\n");
    sb.append("</html>\n");
  }

  private static void appendHtmlVariables(StringBuilder sb, PlaybackInfoSource playbackInfoSource) {
    boolean finished = false;
    JSONObject media_item = null;

    if ((playbackInfoSource != null) && playbackInfoSource.refresh()) {
      finished = playbackInfoSource.isPlaybackFinished();
      if (!finished) {
        try {
          String media_item_json = playbackInfoSource.getMediaItemJson();
          media_item = new JSONObject(media_item_json);
        }
        catch(Exception e) {}
      }
      playbackInfoSource.release();
    }

    String media_url      = getJsonString(media_item, "media_url", null);
    Uri    media_uri      = getUri(media_url);
    String file           = (media_uri == null) ? "&nbsp;" : media_uri.getLastPathSegment();
    String filepatharg    = (media_uri == null) ? "&nbsp;" : media_uri.getEncodedPath();
    String filepath       = (media_uri == null) ? "&nbsp;" : media_uri.getPath();
    String filedirarg     = (media_uri == null) ? "&nbsp;" : filepatharg.substring(0, filepatharg.lastIndexOf("/"));
    String filedir        = (media_uri == null) ? "&nbsp;" : filepath.substring(0, filepath.lastIndexOf("/"));
    String statestring    = getState(media_item);
    int position          = getJsonInt(media_item, "current_position", 0);
    int duration          = getJsonInt(media_item, "duration", 0);
    String positionstring = getTimeCodeString(position);
    String durationstring = getTimeCodeString(duration);

    sb.append("    <p id=\"file\">"           + file           + "</p>\n");
    sb.append("    <p id=\"filepatharg\">"    + filepatharg    + "</p>\n");
    sb.append("    <p id=\"filepath\">"       + filepath       + "</p>\n");
    sb.append("    <p id=\"filedirarg\">"     + filedirarg     + "</p>\n");
    sb.append("    <p id=\"filedir\">"        + filedir        + "</p>\n");
    sb.append("    <p id=\"state\">"          + "0"            + "</p>\n");
    sb.append("    <p id=\"statestring\">"    + statestring    + "</p>\n");
    sb.append("    <p id=\"position\">"       + position       + "</p>\n");
    sb.append("    <p id=\"positionstring\">" + positionstring + "</p>\n");
    sb.append("    <p id=\"duration\">"       + duration       + "</p>\n");
    sb.append("    <p id=\"durationstring\">" + durationstring + "</p>\n");
    sb.append("    <p id=\"volumelevel\">"    + "100"          + "</p>\n");
    sb.append("    <p id=\"muted\">"          + "0"            + "</p>\n");
    sb.append("    <p id=\"playbackrate\">"   + "1.0"          + "</p>\n");
    sb.append("    <p id=\"size\">"           + "0"            + "</p>\n");
    sb.append("    <p id=\"reloadtime\">"     + "0"            + "</p>\n");
    sb.append("    <p id=\"version\">"        + "1"            + "</p>\n");
  }

  private static Uri getUri(String uri) {
    try {
      if (TextUtils.isEmpty(uri)) throw new Exception();

      return Uri.parse(uri);
    }
    catch(Exception e) {}
    return null;
  }

  private static String getState(JSONObject media_item) {
    boolean is_player_ready  = getJsonBoolean(media_item, "is_player_ready",  false);
    boolean is_player_paused = getJsonBoolean(media_item, "is_player_paused", false);

    if (!is_player_ready) return "Stopped";
    if (is_player_paused) return "Paused";
    return "Playing";
  }

  private static String getTimeCodeString(int milliseconds) {
    try {
      int seconds = milliseconds / 1000;
      return new TimeCode(seconds).toString();
    }
    catch(Exception e) {
      return "";
    }
  }

  private static void appendHtmlDebug(StringBuilder sb, PlaybackInfoSource playbackInfoSource) {
    boolean finished = false;
    String media_item_json = null;

    if ((playbackInfoSource != null) && playbackInfoSource.refresh()) {
      finished = playbackInfoSource.isPlaybackFinished();
      if (!finished) {
        media_item_json = playbackInfoSource.getMediaItemJson();
      }
      playbackInfoSource.release();
    }

    if (!TextUtils.isEmpty(media_item_json)) {
      sb.append("<pre id=\"debug\">");
      sb.append(  media_item_json);
      sb.append("</pre>\n");
    }
  }

  private static String getJsonString(JSONObject media_item, String key, String defaultValue) {
    if (defaultValue == null) defaultValue = "";

    try {
      if (media_item == null) throw new Exception();

      String value = media_item.getString(key);
      if (TextUtils.isEmpty(value)) throw new Exception();

      return value;
    }
    catch(Exception e) {
      return defaultValue;
    }
  }

  private static boolean getJsonBoolean(JSONObject media_item, String name, boolean defaultValue) {
    try {
      return media_item.getBoolean(name);
    }
    catch(Exception e) {
      return defaultValue;
    }
  }

  private static int getJsonInt(JSONObject media_item, String name, int defaultValue) {
    try {
      return media_item.getInt(name);
    }
    catch(Exception e) {
      return defaultValue;
    }
  }

}
