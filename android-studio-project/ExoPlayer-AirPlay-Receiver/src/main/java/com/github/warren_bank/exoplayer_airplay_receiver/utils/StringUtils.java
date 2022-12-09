package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;

import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class StringUtils {

  public static String getValue(String textBlock, String prefix, String suffix) {
    String value = "";

    if ((prefix == null) || prefix.isEmpty())
      return value;

    int indexStart, indexEnd;

    indexStart = textBlock.indexOf(prefix);
    if (indexStart < 0)
      return value;
    indexStart += prefix.length();

    indexEnd = ((suffix == null) || suffix.isEmpty())
      ? -1
      : textBlock.indexOf(suffix, indexStart);

    value = (indexEnd < 0)
      ? textBlock.substring(indexStart)
      : textBlock.substring(indexStart, indexEnd);
    value = value.trim();

    return value;
  }

  public static String getQueryStringValue(String url, String prefix) {
    String suffix = "&";
    return StringUtils.getValue(url, prefix, suffix);
  }

  public static String getRequestBodyValue(String requestBody, String prefix) {
    String suffix = "\n";
    return StringUtils.getValue(requestBody, prefix, suffix);
  }

  public static HashMap<String, String> parseRequestBody(String requestBody) {
    return StringUtils.parseRequestBody(requestBody, /* normalize_lowercase_keys= */ true);
  }

  public static HashMap<String, String> parseRequestBody(String requestBody, boolean normalize_lowercase_keys) {
    HashMap<String, String> values = new HashMap<String, String>();

    HashMap<String, ArrayList<String>> duplicateKeyValues = StringUtils.parseRequestBody_allowDuplicateKeys(requestBody, normalize_lowercase_keys);
    ArrayList<String> arrayList;
    String value;

    for (String key : duplicateKeyValues.keySet()) {
      arrayList = (ArrayList<String>) duplicateKeyValues.get(key);
      value     = (String) StringUtils.getLastListItem(arrayList);

      if (value != null)
        values.put(key, value);
    }

    return values;
  }

  public static <T> T getLastListItem(List<T> list) {
    return ((list == null) || list.isEmpty()) ? null : list.get(list.size() - 1);
  }

  public static HashMap<String, ArrayList<String>> parseRequestBody_allowDuplicateKeys(String requestBody) {
    return StringUtils.parseRequestBody_allowDuplicateKeys(requestBody, /* normalize_lowercase_keys= */ true);
  }

  public static HashMap<String, ArrayList<String>> parseRequestBody_allowDuplicateKeys(String requestBody, boolean normalize_lowercase_keys) {
    HashMap<String, ArrayList<String>> values = new HashMap<String, ArrayList<String>>();

    String[] lines = requestBody.split("(?:\\r?\\n)+");
    String[] parts;
    ArrayList<String> arrayList;

    for (String line : lines) {
      parts = line.split("\\s*[:=]\\s*", 2);

      if (parts.length == 2) {
        parts[0] = parts[0].trim();
        parts[1] = parts[1].trim();

        if (normalize_lowercase_keys)
          parts[0] = parts[0].toLowerCase();

        if (!parts[0].isEmpty() && !parts[1].isEmpty()) {
          if (!values.containsKey(parts[0]))
            values.put(parts[0], new ArrayList<String>());

          arrayList = (ArrayList<String>) values.get(parts[0]);
          arrayList.add(parts[1]);
        }
      }
    }

    return values;
  }

  public static HashMap<String, String> parseDuplicateKeyValues(String[] strArray) {
    return StringUtils.parseDuplicateKeyValues(strArray, /* normalize_lowercase_keys= */ true);
  }

  public static HashMap<String, String> parseDuplicateKeyValues(String[] strArray, boolean normalize_lowercase_keys) {
    if ((strArray == null) || (strArray.length == 0)) return null;

    List<String> list = (List<String>) Arrays.asList(strArray);

    return parseDuplicateKeyValues(list, normalize_lowercase_keys);
  }

  public static HashMap<String, String> parseDuplicateKeyValues(List<String> list) {
    return StringUtils.parseDuplicateKeyValues(list, /* normalize_lowercase_keys= */ true);
  }

  public static HashMap<String, String> parseDuplicateKeyValues(List<String> list, boolean normalize_lowercase_keys) {
    if ((list == null) || list.isEmpty()) return null;

    String requestBody = StringUtils.convertListToString(list, "\n");

    return StringUtils.parseRequestBody(requestBody, normalize_lowercase_keys);
  }

  public static String convertEscapedLinefeeds(String requestBody) {
    return requestBody.replaceAll("\\\\n", "\n");
  }

  public static String serializeURLs(ArrayList<String> list) {
    return StringUtils.convertArrayListToString(list, Constant.Delimiter.PLAYLIST_URLS);
  }

  public static ArrayList<String> deserializeURLs(String text) {
    return StringUtils.convertStringToArrayList(text, Pattern.quote(Constant.Delimiter.PLAYLIST_URLS));
  }

  public static String toString(HashMap<String, String> map) {
    if ((map == null) || map.isEmpty()) return null;

    String value = "";
    for (String key : map.keySet()) {
      value += key + ": " + map.get(key) + "\n";
    }

    value = value.trim();
    value = (value == "") ? null : value;

    return value;
  }

  public static String[] toStringArray(HashMap<String, String> map) {
    if ((map == null) || map.isEmpty()) return null;

    ArrayList<String> arrayList = new ArrayList<String>(map.size());

    String value;
    for (String key : map.keySet()) {
      value = key + ": " + map.get(key);
      arrayList.add(value);
    }

    return arrayList.toArray(new String[arrayList.size()]);
  }

  public static Bundle toBundle(HashMap<String, String> map) {
    if ((map == null) || map.isEmpty()) return null;

    Bundle bundle = new Bundle(map.size());

    for (String key : map.keySet()) {
      bundle.putString(key, map.get(key));
    }

    return bundle;
  }

  // unlike TextUtils, trim leading/trailing whitespace before testing for 0-length
  public static boolean isEmpty(String text) {
    return (text == null) || text.trim().isEmpty();
  }

  // =========================================================================== normalize serialized data types

  public static String normalizeBooleanString(String bool) {
    if (bool != null)
      bool = bool.toLowerCase().trim();

    return (
        StringUtils.isEmpty(bool)
     || bool.equals("false")
     || bool.equals("0")
     || bool.equals("null")
     || bool.equals("undefined")
    ) ? "false" : "true";
  }

  // ===================================

  private static String removeTrailingCaseInsensitiveSuffix(String value, char suffix) {
    return removeTrailingCaseInsensitiveSuffix(value, suffix, /* default_value= */ "0");
  }

  private static String removeTrailingCaseInsensitiveSuffix(String value, char suffix, String default_value) {
    suffix = Character.toLowerCase(suffix);

    value = StringUtils.isEmpty(value)
      ? default_value
      : value.trim();

    if (StringUtils.isEmpty(value))
      return value;

    int  tail_index = value.length() - 1;
    char tail;
    tail = value.charAt(tail_index);
    tail = Character.toLowerCase(tail);

    if (tail == suffix) {
      value = value.substring(0, tail_index);
    }

    return value;
  }

  public static String normalizeLongString(String value) {
    return removeTrailingCaseInsensitiveSuffix(value, /* suffix= */ 'L');
  }

  public static String normalizeFloatString(String value) {
    return removeTrailingCaseInsensitiveSuffix(value, /* suffix= */ 'F');
  }

  public static String normalizeDoubleString(String value) {
    return removeTrailingCaseInsensitiveSuffix(value, /* suffix= */ 'D');
  }

  // ===================================

  public static HashMap<String, String> convertArrayListToHashMap(ArrayList<String> list) {
    if ((list == null) || list.isEmpty()) return null;

    HashMap<String, String> map = new HashMap<String, String>();
    String key, val;
    for (int i=0; i < list.size(); i++) {
      key = StringUtils.repeatString("a", (i+1));
      val = list.get(i);
      map.put(key, val);
    }
    return map;
  }

  public static ArrayList<String> convertHashMapToArrayList(HashMap<String, String> map) {
    if ((map == null) || map.isEmpty()) return null;

    TreeMap<String, String> sorted = new TreeMap<String, String>();
    sorted.putAll(map);

    return new ArrayList<String>(sorted.values());
  }

  // ===================================

  public static String convertArrayListToString(ArrayList<String> list, String delimiter_token) {
    return StringUtils.convertListToString((List<String>) list, delimiter_token);
  }

  public static String convertListToString(List<String> list, String delimiter_token) {
    if ((list == null) || list.isEmpty()) return null;

    if (delimiter_token == null)
      delimiter_token = Constant.Delimiter.DEFAULT;

    return TextUtils.join(delimiter_token, list);
  }

  public static ArrayList<String> convertStringToArrayList(String text, String delimiter_token) {
    List<String> list = StringUtils.convertStringToList(text, delimiter_token);

    return new ArrayList<String>(list);
  }

  public static List<String> convertStringToList(String text, String delimiter_token) {
    if (TextUtils.isEmpty(text)) return Collections.emptyList();

    if (delimiter_token == null)
      delimiter_token = Pattern.quote(Constant.Delimiter.DEFAULT);

    String[] strArray = TextUtils.split(text, delimiter_token);
    List<String> list = (List<String>) Arrays.asList(strArray);

    return list;
  }

  // ===================================

  public static String repeatString(String value, int count) {
    return ((value != null) && (count > 0))
      ? (new String(new char[count])).replace("\0", value)
      : null;
  }

}
