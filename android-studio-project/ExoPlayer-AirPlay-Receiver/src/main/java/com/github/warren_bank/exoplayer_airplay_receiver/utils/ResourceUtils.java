package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class ResourceUtils {

  public static int getInteger(Context context, int id) {
    return context.getResources().getInteger(id);
  }

  public static Bitmap getBitmap(Context context, int id) {
    return ((BitmapDrawable) context.getResources().getDrawable(id)).getBitmap();
  }

}
