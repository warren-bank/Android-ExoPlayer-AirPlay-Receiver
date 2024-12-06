package com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api;

import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;

public class OnError {

  public static String getHtml(int what) {
    StringBuilder sb = new StringBuilder();
    String title = null;

    appendHtmlPrefix(sb);

    switch(what) {
      case Constant.Msg.Msg_Runtime_Permissions.Request_EXTERNAL_STORAGE : {
        title = "Permission Denied";
        appendHtmlRequestExternalStorage(sb);
        break;
      }
    }

    appendHtmlBoilerplate(sb, title);
    appendHtmlSuffix(sb);

    return sb.toString();
  }

  private static void appendHtmlPrefix(StringBuilder sb) {
    sb.append("<!DOCTYPE html>\n");
    sb.append("<html lang=\"en\">\n");
    sb.append("  <head>\n");
    sb.append("    <meta charset=\"utf-8\">\n");
    sb.append("    <title>MPC-HC WebServer - Error</title>\n");
    sb.append("  </head>\n");
    sb.append("  <body class=\"page-error\">\n");
  }

  private static void appendHtmlSuffix(StringBuilder sb) {
    sb.append("  </body>\n");
    sb.append("</html>\n");
  }

  private static void appendHtmlRequestExternalStorage(StringBuilder sb) {
    sb.append("    <h3>Permission Denied: READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE</h3>\n");
    sb.append("    <p>To access this page, you must grant permission in the open dialog request window.</p>\n");
  }

  // hidden content for compatibility with clients
  private static void appendHtmlBoilerplate(StringBuilder sb, String title) {
    if (title == null)
      title = "Permission Denied";

    sb.append("    <div style=\"display:none;\">\n");
    sb.append("      <table class=\"browser-table\">\n");
    sb.append("        <tr><td class=\"text-center\"><strong>Location: </strong>" + title + "</td></tr>\n");
    sb.append("      </table>\n");
    sb.append("    </div>\n");
    sb.append("    <div style=\"display:none;\">\n");
    sb.append("      <table class=\"browser-table\">\n");
    sb.append("        <tr><th>Name</th><th>Type</th><th>Size</th><th>Date Modified</th></tr>\n");
    sb.append("        <tr><td class=\"dirname\"><a href=\"/browser.html?path=%2F\">.</a></td><td class=\"dirtype\">Directory</td><td class=\"dirsize\">&nbsp;</td><td class=\"dirdate\">&nbsp;</td></tr>\n");
    sb.append("      </table>\n");
    sb.append("    </div>\n");
  }

}
