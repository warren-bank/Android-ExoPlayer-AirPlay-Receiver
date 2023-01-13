package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class RuntimePermissionUtils {

  // ---------------------------------------------------------------------------
  // Listener interface

  public interface RuntimePermissionListener {
    public void onRequestPermissionsGranted (int requestCode, Object passthrough);
    public void onRequestPermissionsDenied  (int requestCode, Object passthrough, String[] missingPermissions);
  }

  // ---------------------------------------------------------------------------
  // cache of "passthrough" Objects

  private static HashMap<Integer,Object> passthroughCache = new HashMap<Integer,Object>();

  private static void setPassthroughCache(int requestCode, Object passthrough) {
    RuntimePermissionUtils.passthroughCache.put(requestCode, passthrough);
  }

  private static Object getPassthroughCache(int requestCode) {
    Object passthrough = RuntimePermissionUtils.passthroughCache.remove(requestCode);
    return passthrough;
  }

  // ---------------------------------------------------------------------------
  // public API

  public static boolean hasAllPermissions(Context context, int requestCode) {
    String[] allRequestedPermissions = RuntimePermissionUtils.getAllRequestedPermissions(requestCode);
    return RuntimePermissionUtils.hasAllPermissions(context, allRequestedPermissions);
  }

  public static boolean hasAllPermissions(Context context, String[] allRequestedPermissions) {
    String[] missingPermissions = RuntimePermissionUtils.getMissingPermissions(context, allRequestedPermissions);
    return (missingPermissions == null);
  }

  public static void requestPermissions(Activity activity, RuntimePermissionListener listener, int requestCode) {
    String[] allRequestedPermissions = RuntimePermissionUtils.getAllRequestedPermissions(requestCode);
    RuntimePermissionUtils.requestPermissions(activity, listener, requestCode, allRequestedPermissions);
  }

  public static void requestPermissions(Activity activity, RuntimePermissionListener listener, int requestCode, String[] allRequestedPermissions) {
    Object passthrough = null;
    RuntimePermissionUtils.requestPermissions(activity, listener, requestCode, allRequestedPermissions, passthrough);
  }

  public static void requestPermissions(Activity activity, RuntimePermissionListener listener, int requestCode, String[] allRequestedPermissions, Object passthrough) {
    String[] missingPermissions = RuntimePermissionUtils.getMissingPermissions(activity, allRequestedPermissions);

    if (missingPermissions == null) {
      listener.onRequestPermissionsGranted(requestCode, passthrough);
    }
    else {
      RuntimePermissionUtils.setPassthroughCache(requestCode, passthrough);

      activity.requestPermissions(missingPermissions, requestCode);
    }
  }

  public static void onRequestPermissionsResult(RuntimePermissionListener listener, int requestCode, String[] permissions, int[] grantResults) {
    Object passthrough          = RuntimePermissionUtils.getPassthroughCache(requestCode);
    String[] missingPermissions = RuntimePermissionUtils.getMissingPermissions(permissions, grantResults);

    if (missingPermissions == null) {
      listener.onRequestPermissionsGranted(requestCode, passthrough);
    }
    else {
      listener.onRequestPermissionsDenied(requestCode, passthrough, missingPermissions);
    }
  }

  public static void onActivityResult(RuntimePermissionListener listener, int requestCode, int resultCode, Intent data) {
    Object passthrough = RuntimePermissionUtils.getPassthroughCache(requestCode);

    if (resultCode == Activity.RESULT_OK) {
      listener.onRequestPermissionsGranted(requestCode, passthrough);
    }
    else {
      listener.onRequestPermissionsDenied(requestCode, passthrough, /* missingPermissions= */ null);
    }
  }

  // ---------------------------------------------------------------------------
  // internal

  private static String[] getAllRequestedPermissions(int requestCode) {
    String[] allRequestedPermissions = null;

    switch(requestCode) {
      case Constant.PermissionRequestCode.POST_NOTIFICATIONS : {
        if (Build.VERSION.SDK_INT >= 33) {
          allRequestedPermissions = new String[]{"android.permission.POST_NOTIFICATIONS"};
        }
        break;
      }
      case Constant.PermissionRequestCode.READ_EXTERNAL_STORAGE : {
        allRequestedPermissions = (Build.VERSION.SDK_INT >= 33)
          ? new String[]{"android.permission.READ_MEDIA_AUDIO", "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO"}
          : new String[]{"android.permission.READ_EXTERNAL_STORAGE"};
        break;
      }
    }

    return allRequestedPermissions;
  }

  private static String[] getMissingPermissions(Context context, int requestCode) {
    String[] allRequestedPermissions = RuntimePermissionUtils.getAllRequestedPermissions(requestCode);
    return RuntimePermissionUtils.getMissingPermissions(context, allRequestedPermissions);
  }

  private static String[] getMissingPermissions(Context context, String[] allRequestedPermissions) {
    if (Build.VERSION.SDK_INT < 23)
      return null;

    if ((allRequestedPermissions == null) || (allRequestedPermissions.length == 0))
      return null;

    List<String> missingPermissions = new ArrayList<String>();

    for (String permission : allRequestedPermissions) {
      if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(permission);
      }
    }

    if (missingPermissions.isEmpty())
      return null;

    return missingPermissions.toArray(new String[missingPermissions.size()]);
  }

  private static String[] getMissingPermissions(String[] allRequestedPermissions, int[] allGrantResults) {
    if ((allRequestedPermissions == null) || (allRequestedPermissions.length == 0))
      return null;

    if ((allGrantResults == null) || (allGrantResults.length == 0))
      return allRequestedPermissions;

    List<String> missingPermissions = new ArrayList<String>();
    int index;

    for (index = 0; (index < allGrantResults.length) && (index < allRequestedPermissions.length); index++) {
      if (allGrantResults[index] != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(allRequestedPermissions[index]);
      }
    }

    while (index < allRequestedPermissions.length) {
      missingPermissions.add(allRequestedPermissions[index]);
      index++;
    }

    if (missingPermissions.isEmpty())
      return null;

    return missingPermissions.toArray(new String[missingPermissions.size()]);
  }

  // ---------------------------------------------------------------------------
  // special case: "android.permission.MANAGE_EXTERNAL_STORAGE"

  public static void showFilePermissions(Activity activity) {
    int requestCode = Constant.PermissionRequestCode.MANAGE_EXTERNAL_STORAGE;
    RuntimePermissionUtils.showFilePermissions(activity, requestCode);
  }

  public static void showFilePermissions(Activity activity, int requestCode) {
    Object passthrough = null;
    RuntimePermissionUtils.showFilePermissions(activity, requestCode, passthrough);
  }

  public static void showFilePermissions(Activity activity, int requestCode, Object passthrough) {
    Uri uri       = Uri.parse("package:" + activity.getPackageName());
    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);

    RuntimePermissionUtils.setPassthroughCache(requestCode, passthrough);

    activity.startActivityForResult(intent, requestCode);
  }

  public static boolean hasFilePermissions() {
    return RuntimePermissionUtils.canAccessAllFiles();
  }

  private static boolean canAccessAllFiles() {
    return (Build.VERSION.SDK_INT < 30)
      ? true
      : Environment.isExternalStorageManager();
  }

  // ---------------------------------------------------------------------------
  // special case: "android.permission.SYSTEM_ALERT_WINDOW"

  public static void showDrawOverlayPermissions(Activity activity) {
    int requestCode = Constant.PermissionRequestCode.DRAW_OVERLAY;
    RuntimePermissionUtils.showDrawOverlayPermissions(activity, requestCode);
  }

  public static void showDrawOverlayPermissions(Activity activity, int requestCode) {
    Object passthrough = null;
    RuntimePermissionUtils.showDrawOverlayPermissions(activity, requestCode, passthrough);
  }

  public static void showDrawOverlayPermissions(Activity activity, int requestCode, Object passthrough) {
    Uri uri       = Uri.parse("package:" + activity.getPackageName());
    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);

    RuntimePermissionUtils.setPassthroughCache(requestCode, passthrough);

    activity.startActivityForResult(intent, requestCode);
  }

  public static boolean hasDrawOverlayPermissions(Context context) {
    return RuntimePermissionUtils.canStartActivityFromBackground() || RuntimePermissionUtils.canDrawOverlays(context);
  }

  private static boolean canStartActivityFromBackground() {
    return (Build.VERSION.SDK_INT < 29);
  }

  private static boolean canDrawOverlays(Context context) {
    if (Build.VERSION.SDK_INT < 23)
      return true;

    return Settings.canDrawOverlays(context);
  }

  // ---------------------------------------------------------------------------
}
