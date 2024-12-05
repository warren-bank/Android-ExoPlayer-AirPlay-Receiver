package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class ByteUtils {

  /*
   * based on:
   *   https://stackoverflow.com/a/3758880
   */
  public static String formatBytes(long bytes) {
    return formatBytes(bytes, false);
  }

  public static String formatBytes(long bytes, boolean binary) {
    return binary
      ? formatBytesBinary(bytes)
      : formatBytesSI(bytes);
  }

  public static String formatBytesBinary(long bytes) {
    long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
    if (absB < 1024) {
      return bytes + " B";
    }
    long value = absB;
    CharacterIterator ci = new StringCharacterIterator("KMGTPE");
    for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
      value >>= 10;
      ci.next();
    }
    value *= Long.signum(bytes);
    return String.format("%.1f %ciB", value / 1024.0, ci.current());
  }

  public static String formatBytesSI(long bytes) {
    if (-1000 < bytes && bytes < 1000) {
      return bytes + " B";
    }
    CharacterIterator ci = new StringCharacterIterator("kMGTPE");
    while (bytes <= -999_950 || bytes >= 999_950) {
      bytes /= 1000;
      ci.next();
    }
    return String.format("%.1f %cB", bytes / 1000.0, ci.current());
  }

}
