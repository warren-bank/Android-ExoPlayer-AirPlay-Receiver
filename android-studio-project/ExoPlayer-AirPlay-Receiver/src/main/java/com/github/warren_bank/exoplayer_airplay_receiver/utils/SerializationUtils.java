package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SerializationUtils {

  // ---------------------------------------------------------------------------
  // Object <-> String: Base64

  private static int Base64Flags = Base64.NO_PADDING | Base64.NO_WRAP;

  public static String serializeObject(Object obj) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();
      return Base64.encodeToString(baos.toByteArray(), Base64Flags);
    }
    catch(Exception e) {}
    return null;
  }

  public static Object deserializeObject(String str) {
    try {
      byte[] data = Base64.decode(str, Base64Flags);
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      ObjectInputStream ois = new ObjectInputStream(bais);
      return ois.readObject();
    }
    catch(Exception e) {}
    return null;
  }

  // ---------------------------------------------------------------------------
  // HashMap <-> String: "Key: Value" per line

  public static String serializeHashMap(HashMap<String, String> map) {
    try {
      ArrayList<String> lines = new ArrayList<String>();
      String value;

      for (String key : map.keySet()) {
        value = (String) map.get(key);

        lines.add(
          String.format("%s: %s", key, value)
        );
      }

      return TextUtils.join("\n", lines);
    }
    catch(Exception e) {}
    return null;
  }

}
