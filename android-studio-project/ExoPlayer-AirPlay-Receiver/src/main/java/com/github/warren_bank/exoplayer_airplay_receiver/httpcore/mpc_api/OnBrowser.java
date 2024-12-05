package com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api;

/*
 * based on:
 *   https://github.com/mpc-hc/mpc-hc/blob/1.7.13/src/mpc-hc/res/web/browser.html
 *   https://github.com/mpc-hc/mpc-hc/blob/1.7.13/src/mpc-hc/WebClientSocket.cpp#L453-L635
 */

import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ByteUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;

import android.text.TextUtils;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class OnBrowser {

  public static String getHtml(File directory) {
    StringBuilder sb = new StringBuilder();

    appendHtmlPrefix(sb, directory);
    appendHtmlDirectoryContent(sb, directory);
    appendHtmlSuffix(sb);

    return sb.toString();
  }

  private static void appendHtmlPrefix(StringBuilder sb, File directory) {
    sb.append("<!DOCTYPE html>\n");
    sb.append("<html lang=\"en\">\n");
    sb.append("  <head>\n");
    sb.append("    <meta charset=\"utf-8\">\n");
    sb.append("    <title>MPC-HC WebServer - File Browser</title>\n");
    sb.append("  </head>\n");
    sb.append("  <body class=\"page-file-browser\">\n");
    sb.append("    <div>\n");
    sb.append("      <table class=\"browser-table\">\n");
    sb.append("        <tr>\n");
    sb.append("          <td class=\"text-center\">\n");
    sb.append("            <strong>Location: </strong>" + directory.getPath() + "\n");
    sb.append("          </td>\n");
    sb.append("        </tr>\n");
    sb.append("      </table>\n");
    sb.append("    </div>\n");
    sb.append("    <span>&nbsp;</span>\n");
    sb.append("    <div>\n");
    sb.append("      <table class=\"browser-table\">\n");
    sb.append("        <tr>\n");
    sb.append("          <th>Name</th>\n");
    sb.append("          <th>Type</th>\n");
    sb.append("          <th>Size</th>\n");
    sb.append("          <th>Date Modified</th>\n");
    sb.append("        </tr>\n");
  }

  private static void appendHtmlSuffix(StringBuilder sb) {
    sb.append("      </table>\n");
    sb.append("    </div>\n");
    sb.append("  </body>\n");
    sb.append("</html>\n");
  }

  private static void appendHtmlDirectoryContent(StringBuilder sb, File directory) {
    String parent = directory.getParent();
    if (!TextUtils.isEmpty(parent)) {
      sb.append("<tr>\n");
      sb.append("  <td class=\"dirname\"><a href=\"" + getUrl(parent) + "\">..</a></td>\n");
      sb.append("  <td class=\"dirtype\">Directory</td>\n");
      sb.append("  <td class=\"dirsize\">&nbsp;</td>\n");
      sb.append("  <td class=\"dirdate\">&nbsp;</td>\n");
      sb.append("</tr>\n");
    }

    File[] subdirs = directory.listFiles(new FileFilter() {
      public boolean accept(File child) {
        return child.isDirectory();
      }
    });

    for (File child : subdirs) {
      sb.append("<tr>\n");
      sb.append("  <td class=\"dirname\"><a href=\"" + getUrl(child.getPath()) + "\">" + child.getName() + "</a></td>\n");
      sb.append("  <td class=\"dirtype\">Directory</td>\n");
      sb.append("  <td class=\"dirsize\">&nbsp;</td>\n");
      sb.append("  <td class=\"dirdate\">" + getLastModified(child, "&nbsp;") + "</td>\n");
      sb.append("</tr>\n");
    }
    subdirs = null;

    File[] mediafiles = directory.listFiles(new FileFilter() {
      public boolean accept(File child) {
        if (!child.isFile()) return false;

        String uri = "file:" + child.getName();

        return (
          MediaTypeUtils.isVideoFileUrl(uri) ||
          MediaTypeUtils.isAudioFileUrl(uri)
        );
      }
    });

    for (File child : mediafiles) {
      String uri  = "file:" + child.getName();
      String ext  = null;
      String type = null;

      if (MediaTypeUtils.isVideoFileUrl(uri)) {
        ext  = MediaTypeUtils.get_video_fileExtension(uri);
        type = MediaTypeUtils.get_video_mimeType(uri);
      }
      else if (MediaTypeUtils.isAudioFileUrl(uri)) {
        ext  = MediaTypeUtils.get_audio_fileExtension(uri);
        type = MediaTypeUtils.get_audio_mimeType(uri);
      }

      if (TextUtils.isEmpty(type))
        type = "&nbsp;";

      sb.append("<tr class=\"" + (TextUtils.isEmpty(ext) ? "noext" : ext) + "\">\n");
      sb.append("  <td><a href=\"" + getUrl(child.getPath()) + "\">" + child.getName() + "</a></td>\n");
      sb.append("  <td><span class=\"nobr\">" + type + "</span></td>\n");
      sb.append("  <td><span class=\"nobr\">" + getSize(child, "0", false) + "</span></td>\n");
      sb.append("  <td><span class=\"nobr\">" + getLastModified(child, "&nbsp;") + "</span></td>\n");
      sb.append("</tr>\n");
    }
  }

  private static String getUrl(String path) {
    return Constant.Target.MPC_API_BROWSER + "?path=" + StringUtils.encodeURIComponent(path);
  }

  private static String getLastModified(File file, String defaultValue) {
    if (defaultValue == null) defaultValue = "";

    try {
      long milliseconds = file.lastModified();

      if (milliseconds == 0L) throw new Exception();

      Date date = new Date(milliseconds);
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm");
      formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      return formatter.format(date);
    }
    catch(Exception e) {
      return defaultValue;
    }
  }

  private static String getSize(File file, String defaultValue, boolean formatBytes) {
    if (defaultValue == null) defaultValue = "";

    try {
      long bytes = file.length();
      return formatBytes
        ? ByteUtils.formatBytes(bytes)
        : Long.toString(bytes, 10);
    }
    catch(Exception e) {
      return defaultValue;
    }
  }

}
