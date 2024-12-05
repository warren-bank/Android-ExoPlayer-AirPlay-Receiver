package com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api;

/*
 * based on:
 *   https://github.com/mpc-hc/mpc-hc/blob/1.7.13/src/mpc-hc/res/web/info.html
 *   https://github.com/mpc-hc/mpc-hc/blob/1.7.13/src/mpc-hc/WebClientSocket.cpp#L442-L451
 */

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.RequestListenerThread.PlaybackInfoSource;

import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

public class OnInfo {

  public static String getHtml(Context context, PlaybackInfoSource playbackInfoSource) {
    StringBuilder sb = new StringBuilder();

    appendHtmlPrefix(sb);
    appendHtmlInfo(sb, context, playbackInfoSource);
    appendHtmlDebug(sb, playbackInfoSource);
    appendHtmlSuffix(sb);

    return sb.toString();
  }

  private static void appendHtmlPrefix(StringBuilder sb) {
    sb.append("<!DOCTYPE html>\n");
    sb.append("<html lang=\"en\">\n");
    sb.append("  <head>\n");
    sb.append("    <meta charset=\"utf-8\">\n");
    sb.append("    <title>MPC-HC WebServer - Info</title>\n");
    sb.append("  </head>\n");
    sb.append("  <body class=\"page-info\">\n");
  }

  private static void appendHtmlSuffix(StringBuilder sb) {
    sb.append("  </body>\n");
    sb.append("</html>\n");
  }

  private static void appendHtmlInfo(StringBuilder sb, Context context, PlaybackInfoSource playbackInfoSource) {
    boolean finished = false;
    long duration = 0;
    long curPos   = 0;
    JSONObject media_item = null;

    if ((playbackInfoSource != null) && playbackInfoSource.refresh()) {
      finished = playbackInfoSource.isPlaybackFinished();
      if (!finished) {
        duration = playbackInfoSource.getDuration();
        curPos   = playbackInfoSource.getCurrentPosition();

        duration = (duration < 0) ? 0 : duration;
        curPos   = (curPos   < 0) ? 0 : curPos;

        try {
          String media_item_json = playbackInfoSource.getMediaItemJson();
          media_item = new JSONObject(media_item_json);
        }
        catch(Exception e) {}
      }
      playbackInfoSource.release();
    }

    sb.append("<p id=\"mpchc_np\">&laquo; ");
    // MPC-HC v[version] &bull; [file] &bull; [position]/[duration] &bull; [size]
    sb.append(
      context.getString(R.string.app_name)
    );
    sb.append(" &bull; ");
    // [file]
    sb.append(
      getJsonString(media_item, "media_url", "&nbsp;")
    );
    sb.append(" &bull; ");
    // [position]/[duration]
    sb.append(
      String.format("%d/%d", curPos, duration)
    );
    sb.append(" &bull; ");
    // [size]
    sb.append("&nbsp;");
    sb.append(" &raquo;</p>\n");
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

}
