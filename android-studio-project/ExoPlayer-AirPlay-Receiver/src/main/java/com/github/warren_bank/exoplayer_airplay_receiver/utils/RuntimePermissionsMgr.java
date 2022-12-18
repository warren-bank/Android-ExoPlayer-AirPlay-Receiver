package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.ui.RuntimePermissionsListenerActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public final class RuntimePermissionsMgr {
  private static final int REQUEST_CODE_DRAWOVERLAYS = 0;

  public static void onPermissionsGranted(RuntimePermissionsListenerActivity activity) {
    if (canStartActivityFromBackground() || canDrawOverlays(activity))
      activity.onPermissionsGranted();
    else
      requestPermissionDrawOverlays(activity);
  }

  public static boolean canStartActivityFromBackground() {
    return (Build.VERSION.SDK_INT < 29);
  }

  public static boolean canDrawOverlays(Context context) {
    if (Build.VERSION.SDK_INT < 23)
      return true;

    return Settings.canDrawOverlays(context);
  }

  public static void requestPermissionDrawOverlays(Activity activity) {
    Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
    activity.startActivityForResult(permissionIntent, REQUEST_CODE_DRAWOVERLAYS);
  }

  public static void onActivityResult(RuntimePermissionsListenerActivity activity, int requestCode, int resultCode, Intent data) {
    if (requestCode != REQUEST_CODE_DRAWOVERLAYS)
      return;

    if (canDrawOverlays(activity))
      activity.onPermissionsGranted();
    else
      activity.onPermissionsDenied(new String[]{"android.permission.SYSTEM_ALERT_WINDOW"});
  }

}
