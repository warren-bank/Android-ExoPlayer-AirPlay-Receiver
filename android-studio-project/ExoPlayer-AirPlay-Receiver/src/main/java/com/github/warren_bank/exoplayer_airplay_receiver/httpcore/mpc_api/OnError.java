package com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;

import java.io.File;

public class OnError {

  public static final int DIRECTORY_DOES_NOT_EXIST = 1;
  public static final int REQUEST_EXTERNAL_STORAGE = 2;

  public static String getHtml(int what) {
    return getHtml(what, null, null);
  }

  public static String getHtml(int what, File directory) {
    return getHtml(what, directory.getParent(), directory.getPath());
  }

  public static String getHtml(int what, String parent, String cwd) {
    StringBuilder sb = new StringBuilder();
    boolean hidden   = false;
    String title     = null;

    appendHtmlPrefix(sb);

    switch(what) {
      case DIRECTORY_DOES_NOT_EXIST : {
        hidden = false;
        title = cwd;
        cwd = null;
        break;
      }

      case REQUEST_EXTERNAL_STORAGE : {
        appendHtmlRequestExternalStorage(sb);

        hidden = true;
        title = "Permission Denied";
        cwd = "/";
        break;
      }
    }

    appendHtmlBoilerplate(sb, hidden, title, parent, cwd);
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

  // content for compatibility with clients
  private static void appendHtmlBoilerplate(StringBuilder sb, boolean hidden, String title, String parent, String cwd) {
    if (title == null)
      title = "";

    sb.append("    <div" + (hidden ? " style=\"display:none;\"" : "") + ">\n");
    sb.append("      <table class=\"browser-table\">\n");
    sb.append("        <tr><td class=\"text-center\"><strong>Location: </strong>" + title + "</td></tr>\n");
    sb.append("      </table>\n");
    sb.append("    </div>\n");
    sb.append("    <div" + (hidden ? " style=\"display:none;\"" : "") + ">\n");
    sb.append("      <table class=\"browser-table\">\n");
    sb.append("        <tr><th>Name</th><th>Type</th><th>Size</th><th>Date Modified</th></tr>\n");

    if (parent != null) {
      sb.append("        <tr><td class=\"dirname\"><a href=\"/browser.html?path=" + StringUtils.encodeURIComponent(parent) + "\">..</a></td><td class=\"dirtype\">Directory</td><td class=\"dirsize\">&nbsp;</td><td class=\"dirdate\">&nbsp;</td></tr>\n");
    }
    if (cwd != null) {
      sb.append("        <tr><td class=\"dirname\"><a href=\"/browser.html?path=" + StringUtils.encodeURIComponent(cwd) + "\">.</a></td><td class=\"dirtype\">Directory</td><td class=\"dirsize\">&nbsp;</td><td class=\"dirdate\">&nbsp;</td></tr>\n");
    }

    sb.append("      </table>\n");
    sb.append("    </div>\n");
  }

}
