package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class ResourceUtils {

  // ---------------------------------------------------------------------------
  // with explicit context:

  public static String getString(Context context, int id) {
    return context.getString(id);
  }

  public static boolean getBoolean(Context context, int id) {
    return context.getResources().getBoolean(id);
  }

  public static int getInteger(Context context, int id) {
    return context.getResources().getInteger(id);
  }

  public static float getFloat(Context context, int id) {
    return getFloat(context, id, /* divisor= */ 100.0f);
  }

  public static float getFloat(Context context, int id, float divisor) {
    int value = getInteger(context, id);

    return (float) (value / divisor);
  }

  public static Bitmap getBitmap(Context context, int id) {
    return ((BitmapDrawable) context.getResources().getDrawable(id)).getBitmap();
  }

  // ---------------------------------------------------------------------------
  // internal:

  private static Context getApplicationContext() {
    return (Context) MainApp.getInstance();
  }

  // ---------------------------------------------------------------------------
  // with implicit context:

  public static String getString(int id) {
    return getString(getApplicationContext(), id);
  }

  public static boolean getBoolean(int id) {
    return getBoolean(getApplicationContext(), id);
  }

  public static int getInteger(int id) {
    return getInteger(getApplicationContext(), id);
  }

  public static float getFloat(int id) {
    return getFloat(getApplicationContext(), id);
  }

  public static float getFloat(int id, float divisor) {
    return getFloat(getApplicationContext(), id, divisor);
  }

  public static Bitmap getBitmap(int id) {
    return getBitmap(getApplicationContext(), id);
  }

  // ---------------------------------------------------------------------------

}
