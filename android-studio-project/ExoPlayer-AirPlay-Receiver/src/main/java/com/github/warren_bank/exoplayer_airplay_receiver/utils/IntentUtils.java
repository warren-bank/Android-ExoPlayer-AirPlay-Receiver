package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.content.Intent;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IntentUtils {

  /*
   * supported explicit cast types:
   *   string
   *   string[]
   *   bool
   *   bool[]
   *   boolean
   *   boolean[]
   *   byte
   *   byte[]
   *   char
   *   char[]
   *   double
   *   double[]
   *   float
   *   float[]
   *   int
   *   int[]
   *   integer
   *   integer[]
   *   long
   *   long[]
   *   short
   *   short[]
   *
   * examples of explicit type casting:
   *   (string)    (int) not an integer!
   *   (string)    1
   *   (string)    1L
   *   (string)    1.0F
   *   (string)    1.0D
   *   (string)    true
   *   (string[])  length, of, String[], equals, 1
   *   (int)       1
   *   (long)      1
   *   (float)     1.0
   *   (double)    1.0
   *   (byte[])    -10, -20, -30, -40
   *   (short[])   100, 200, 300, 400
   *   (boolean[]) true, true, false, false
   *   (char[])    h,e,l,l,o
   *   (char[])    hello
   */
  private static Pattern explicit_type_cast_regex    = Pattern.compile("^\\(\\s*(string|string\\[\\]|bool|bool\\[\\]|boolean|boolean\\[\\]|byte|byte\\[\\]|char|char\\[\\]|double|double\\[\\]|float|float\\[\\]|int|int\\[\\]|integer|integer\\[\\]|long|long\\[\\]|short|short\\[\\])\\s*\\)\\s*(.+)$", Pattern.CASE_INSENSITIVE);
  private static Pattern implicit_type_boolean_regex = Pattern.compile("^(?:true|false)$", Pattern.CASE_INSENSITIVE);
  private static Pattern implicit_type_integer_regex = Pattern.compile("^[+-]?\\d+$");
  private static Pattern implicit_type_long_regex    = Pattern.compile("^[+-]?\\d+[l|L]$");
  private static Pattern implicit_type_float_regex   = Pattern.compile("^[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)[f|F]$");
  private static Pattern implicit_type_double_regex  = Pattern.compile("^[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)[d|D]$");

  public static void putExtra(Intent intent, String name, String value) {
    if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) return;

    String explicit_type          = null;
    String explicit_value         = null;
    String[] explicit_value_array = null;
    boolean is_array;
    Matcher matcher;

    matcher = explicit_type_cast_regex.matcher(value);
    if (matcher.find()) {
      explicit_type  = matcher.group(1).toLowerCase();
      explicit_value = matcher.group(2);
    }

    if (explicit_type == null) {
      matcher = implicit_type_boolean_regex.matcher(value);
      if (matcher.find()) {
        explicit_type  = "boolean";
        explicit_value = value;
      }
    }

    if (explicit_type == null) {
      matcher = implicit_type_integer_regex.matcher(value);
      if (matcher.find()) {
        explicit_type  = "int";
        explicit_value = value;
      }
    }

    if (explicit_type == null) {
      matcher = implicit_type_long_regex.matcher(value);
      if (matcher.find()) {
        explicit_type  = "long";
        explicit_value = value;
      }
    }

    if (explicit_type == null) {
      matcher = implicit_type_float_regex.matcher(value);
      if (matcher.find()) {
        explicit_type  = "float";
        explicit_value = value;
      }
    }

    if (explicit_type == null) {
      matcher = implicit_type_double_regex.matcher(value);
      if (matcher.find()) {
        explicit_type  = "double";
        explicit_value = value;
      }
    }

    if (explicit_type == null) {
      intent.putExtra(name, value);
      return;
    }

    is_array = explicit_type.endsWith("[]") && !explicit_type.equals("string[]");

    explicit_value_array = is_array
      ? explicit_value.split("\\s*[,]\\s*")
      : new String[]{explicit_value}
    ;

    switch(explicit_type) {

      case "string" : {
        intent.putExtra(name, (String) explicit_value);
        break;
      }

      case "string[]" : {
        intent.putExtra(name, (String[]) explicit_value_array);
        break;
      }

      case "bool"      :
      case "bool[]"    :
      case "boolean"   :
      case "boolean[]" : {
        boolean[] parsed_values = convert_boolean_array(explicit_value_array);

        if (is_array)
          intent.putExtra(name, (boolean[]) parsed_values);
        else
          intent.putExtra(name, (boolean)   parsed_values[0]);
        break;
      }

      case "byte"   :
      case "byte[]" : {
        byte[] parsed_values = convert_byte_array(explicit_value_array);

        if (is_array)
          intent.putExtra(name, (byte[]) parsed_values);
        else
          intent.putExtra(name, (byte)   parsed_values[0]);
        break;
      }

      case "char"   :
      case "char[]" : {
        char[] parsed_values = convert_char_array(explicit_value_array);

        if (is_array)
          intent.putExtra(name, (char[]) parsed_values);
        else
          intent.putExtra(name, (char)   parsed_values[0]);
        break;
      }

      case "double"   :
      case "double[]" : {
        double[] parsed_values = convert_double_array(explicit_value_array);

        if (is_array)
          intent.putExtra(name, (double[]) parsed_values);
        else
          intent.putExtra(name, (double)   parsed_values[0]);
        break;
      }

      case "float"   :
      case "float[]" : {
        float[] parsed_values = convert_float_array(explicit_value_array);

        if (is_array)
          intent.putExtra(name, (float[]) parsed_values);
        else
          intent.putExtra(name, (float)   parsed_values[0]);
        break;
      }

      case "int"       :
      case "int[]"     :
      case "integer"   :
      case "integer[]" : {
        int[] parsed_values = convert_int_array(explicit_value_array);

        if (is_array)
          intent.putExtra(name, (int[]) parsed_values);
        else
          intent.putExtra(name, (int)   parsed_values[0]);
        break;
      }

      case "long"   :
      case "long[]" : {
        long[] parsed_values = convert_long_array(explicit_value_array);

        if (is_array)
          intent.putExtra(name, (long[]) parsed_values);
        else
          intent.putExtra(name, (long)   parsed_values[0]);
        break;
      }

      case "short"   :
      case "short[]" : {
        short[] parsed_values = convert_short_array(explicit_value_array);

        if (is_array)
          intent.putExtra(name, (short[]) parsed_values);
        else
          intent.putExtra(name, (short)   parsed_values[0]);
        break;
      }

    }
  }

  // =========================================================================== convert arrays

  private static boolean[] convert_boolean_array(String[] old_values) {
    boolean[] new_values = new boolean[old_values.length];

    String  old_value;
    boolean new_value;
    for (int i=0; i < old_values.length; i++) {
      try {
        old_value = old_values[i];
        old_value = StringUtils.normalizeBooleanString(old_value);
        new_value = Boolean.parseBoolean(old_value);

        new_values[i] = new_value;
      }
      catch(Exception e) {}
    }

    return new_values;
  }

  // ===================================

  private static byte[] convert_byte_array(String[] old_values) {
    byte[] new_values = new byte[old_values.length];

    String old_value;
    byte   new_value;
    for (int i=0; i < old_values.length; i++) {
      try {
        old_value = old_values[i];
        new_value = Byte.parseByte(old_value);

        new_values[i] = new_value;
      }
      catch(Exception e) {}
    }

    return new_values;
  }

  // ===================================

  private static char[] convert_char_array(String[] old_values) {
    int length = 0;
    for (String old_value : old_values) {
      length += old_value.length();
    }

    char[] new_values = new char[length];
    int index = 0;

    char[] new_value;
    for (String old_value : old_values) {
      try {
        new_value = old_value.toCharArray();

        for (int i=0; (i < new_value.length) && (index < length); i++) {
          new_values[index] = new_value[i];
          index++;
        }
      }
      catch(Exception e) {}
    }

    return new_values;
  }

  // ===================================

  private static double[] convert_double_array(String[] old_values) {
    double[] new_values = new double[old_values.length];

    String old_value;
    double new_value;
    for (int i=0; i < old_values.length; i++) {
      try {
        old_value = old_values[i];
        old_value = StringUtils.normalizeDoubleString(old_value);
        new_value = Double.parseDouble(old_value);

        new_values[i] = new_value;
      }
      catch(Exception e) {}
    }

    return new_values;
  }

  // ===================================

  private static float[] convert_float_array(String[] old_values) {
    float[] new_values = new float[old_values.length];

    String old_value;
    float  new_value;
    for (int i=0; i < old_values.length; i++) {
      try {
        old_value = old_values[i];
        old_value = StringUtils.normalizeFloatString(old_value);
        new_value = Float.parseFloat(old_value);

        new_values[i] = new_value;
      }
      catch(Exception e) {}
    }

    return new_values;
  }

  // ===================================

  private static int[] convert_int_array(String[] old_values) {
    int[] new_values = new int[old_values.length];

    String old_value;
    int    new_value;
    for (int i=0; i < old_values.length; i++) {
      try {
        old_value = old_values[i];
        new_value = Integer.parseInt(old_value);

        new_values[i] = new_value;
      }
      catch(Exception e) {}
    }

    return new_values;
  }

  // ===================================

  private static long[] convert_long_array(String[] old_values) {
    long[] new_values = new long[old_values.length];

    String old_value;
    long   new_value;
    for (int i=0; i < old_values.length; i++) {
      try {
        old_value = old_values[i];
        old_value = StringUtils.normalizeLongString(old_value);
        new_value = Long.parseLong(old_value);

        new_values[i] = new_value;
      }
      catch(Exception e) {}
    }

    return new_values;
  }

  // ===================================

  private static short[] convert_short_array(String[] old_values) {
    short[] new_values = new short[old_values.length];

    String old_value;
    short  new_value;
    for (int i=0; i < old_values.length; i++) {
      try {
        old_value = old_values[i];
        new_value = Short.parseShort(old_value);

        new_values[i] = new_value;
      }
      catch(Exception e) {}
    }

    return new_values;
  }

}
