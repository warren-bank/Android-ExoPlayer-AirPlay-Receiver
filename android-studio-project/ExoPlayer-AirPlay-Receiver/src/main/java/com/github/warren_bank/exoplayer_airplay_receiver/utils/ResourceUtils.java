package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class ResourceUtils {

  public static int getInteger(int id) {
    return getInteger((Context) MainApp.getInstance(), id);
  }

  public static int getInteger(Context context, int id) {
    return context.getResources().getInteger(id);
  }

  public static Bitmap getBitmap(Context context, int id) {
    return ((BitmapDrawable) context.getResources().getDrawable(id)).getBitmap();
  }

}
