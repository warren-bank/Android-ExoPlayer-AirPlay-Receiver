package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public class PipUtils {

  public static boolean supportsPictureInPictureMode(Context context) {
    if (Build.VERSION.SDK_INT < 24) return false;

    PackageManager pm = context.getPackageManager();
    return pm.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
  }

  public static boolean supportsPictureInPictureParams(Context context) {
    return supportsPictureInPictureMode(context) && (Build.VERSION.SDK_INT >= 26);
  }

  public static void enterPictureInPictureMode(Activity activity) {
    if (!supportsPictureInPictureMode((Context) activity)) return;

    activity.enterPictureInPictureMode();
  }

  public static void exitPictureInPictureMode(Activity activity, Intent intent) {
    if (!supportsPictureInPictureMode((Context) activity)) return;

    Context context = activity.getApplicationContext();
    activity.finish();
    context.startActivity(intent);
  }

  public static boolean isInPictureInPictureMode(Activity activity) {
    if (!supportsPictureInPictureMode((Context) activity)) return false;

    return activity.isInPictureInPictureMode();
  }

}
