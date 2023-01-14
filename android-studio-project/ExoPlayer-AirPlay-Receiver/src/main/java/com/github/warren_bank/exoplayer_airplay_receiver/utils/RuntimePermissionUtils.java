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
    public void onAllRequestsCompleted      (Exception exception, Object passthrough);
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
  // convenience class: request multiple runtime permissions in sequence

  private static final class SequentialRequesterClass implements RuntimePermissionListener {

    // ---------------------------------
    // static

    private static SequentialRequesterClass activeInstance = null;

    private static HashMap<RuntimePermissionListener,SequentialRequesterClass> allInstances = new HashMap<RuntimePermissionListener,SequentialRequesterClass>();

    public static SequentialRequesterClass getInstance(RuntimePermissionListener listener) {
      SequentialRequesterClass instance = SequentialRequesterClass.allInstances.get(listener);
      return instance;
    }

    public static boolean isActiveInstance(SequentialRequesterClass instance) {
      return (instance == SequentialRequesterClass.activeInstance);
    }

    public static void updateActiveInstance() {
        SequentialRequesterClass.activeInstance = (SequentialRequesterClass.allInstances.isEmpty())
          ? null
          : SequentialRequesterClass.allInstances.values().iterator().next();

        if (SequentialRequesterClass.activeInstance != null)
          SequentialRequesterClass.activeInstance.requestNextCode();
    }

    public static List<Integer> convertRequestCodes(int[] requestCodes) throws Exception {
      if (requestCodes == null)
        throw new Exception("parameter cannot be null: requestCodes");
      if (requestCodes.length == 0)
        throw new Exception("array parameter cannot be empty: requestCodes");

      List<Integer> result = new ArrayList<Integer>(requestCodes.length);
      for (int requestCode : requestCodes) {
        result.add(Integer.valueOf(requestCode));
      }
      return result;
    }

    // ---------------------------------
    // instance

    private Activity mActivity;
    private RuntimePermissionListener mListener;
    private List<Integer> mRequestCodes;
    private List<Object> mPassthroughs;
    private int mCurrentRequestCode;

    public SequentialRequesterClass(Activity activity, RuntimePermissionListener listener, int[] requestCodes, Object passthrough) throws Exception {
      this(activity, listener, SequentialRequesterClass.convertRequestCodes(requestCodes), passthrough);
    }

    public SequentialRequesterClass(Activity activity, RuntimePermissionListener listener, List<Integer> requestCodes, Object passthrough) throws Exception {
      if (activity == null)
        throw new Exception("parameter cannot be null: activity");
      if (listener == null)
        throw new Exception("parameter cannot be null: listener");
      if (requestCodes == null)
        throw new Exception("parameter cannot be null: requestCodes");
      if (requestCodes.isEmpty())
        throw new Exception("list parameter cannot be empty: requestCodes");

      SequentialRequesterClass instance = SequentialRequesterClass.getInstance(listener);
      if (instance != null) {
        instance.appendRequestCodes(requestCodes);
        instance.appendPassthrough(passthrough);
      }
      else {
        mActivity           = activity;
        mListener           = listener;
        mRequestCodes       = new ArrayList<Integer>();
        mPassthroughs       = new ArrayList<Object>();
        mCurrentRequestCode = -1;

        appendRequestCodes(requestCodes);
        appendPassthrough(passthrough);

        SequentialRequesterClass.allInstances.put(mListener, this);

        if (SequentialRequesterClass.activeInstance == null) {
          SequentialRequesterClass.activeInstance = this;
          requestNextCode();
        }
      }
    }

    public void appendRequestCodes(List<Integer> newRequestCodes) {
      if ((newRequestCodes == null) || newRequestCodes.isEmpty()) return;

      for (Integer newRequestCode : newRequestCodes) {
        appendRequestCode(newRequestCode);
      }
    }

    public void appendRequestCode(int newRequestCode) {
      appendRequestCode(Integer.valueOf(newRequestCode));
    }

    public void appendRequestCode(Integer newRequestCode) {
      if ((newRequestCode == null) || mRequestCodes.contains(newRequestCode)) return;
      mRequestCodes.add(newRequestCode);
    }

    public void appendPassthroughs(List<Object> newPassthroughs) {
      if ((newPassthroughs == null) || newPassthroughs.isEmpty()) return;

      for (Object newPassthrough : newPassthroughs) {
        appendPassthrough(newPassthrough);
      }
    }

    public void appendPassthrough(Object newPassthrough) {
      if ((newPassthrough == null) || mPassthroughs.contains(newPassthrough)) return;
      mPassthroughs.add(newPassthrough);
    }

    @Override
    public void onRequestPermissionsGranted(int requestCode, Object passthrough) {
      mRequestCodes.remove(Integer.valueOf(requestCode));
      mListener.onRequestPermissionsGranted(requestCode, passthrough);

      if (SequentialRequesterClass.isActiveInstance(this) && (mCurrentRequestCode == requestCode))
        requestNextCode();
    }

    @Override
    public void onRequestPermissionsDenied(int requestCode, Object passthrough, String[] missingPermissions) {
      mRequestCodes.remove(Integer.valueOf(requestCode));
      mListener.onRequestPermissionsDenied(requestCode, passthrough, missingPermissions);

      if (SequentialRequesterClass.isActiveInstance(this) && (mCurrentRequestCode == requestCode))
        requestNextCode();
    }

    @Override
    public void onAllRequestsCompleted(Exception exception, Object passthrough) {
      for (Object mPassthrough : mPassthroughs) {
        mListener.onAllRequestsCompleted(exception, mPassthrough);
      }
      mPassthroughs.clear();

      SequentialRequesterClass.allInstances.remove(mListener);
      SequentialRequesterClass.updateActiveInstance();
    }

    protected void requestNextCode() {
      if (mRequestCodes.isEmpty()) {
        this.onAllRequestsCompleted((Exception) null, (Object) null);
      }
      else {
        mCurrentRequestCode = ((Integer) mRequestCodes.get(0)).intValue();
        RuntimePermissionUtils.requestPermissions(mActivity, /* listener= */ this, mCurrentRequestCode);
      }
    }
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

    SequentialRequesterClass instance = SequentialRequesterClass.getInstance(listener);
    if (instance != null)
      listener = (RuntimePermissionListener) instance;

    if (missingPermissions == null) {
      listener.onRequestPermissionsGranted(requestCode, passthrough);
    }
    else {
      RuntimePermissionUtils.setPassthroughCache(requestCode, passthrough);

      if (instance != null)
        instance.appendRequestCode(requestCode);
      else
        activity.requestPermissions(missingPermissions, requestCode);
    }
  }

  public static void requestAllPermissions(Activity activity, RuntimePermissionListener listener, int[] requestCodes) {
    Object passthrough = null;
    RuntimePermissionUtils.requestAllPermissions(activity, listener, requestCodes, passthrough);
  }

  public static void requestAllPermissions(Activity activity, RuntimePermissionListener listener, int[] requestCodes, Object passthrough) {
    try {
      new RuntimePermissionUtils.SequentialRequesterClass(activity, listener, requestCodes, passthrough);
    }
    catch(Exception exception) {
      listener.onAllRequestsCompleted(exception, passthrough);
    }
  }

  public static void onRequestPermissionsResult(RuntimePermissionListener listener, int requestCode, String[] permissions, int[] grantResults) {
    Object passthrough          = RuntimePermissionUtils.getPassthroughCache(requestCode);
    String[] missingPermissions = RuntimePermissionUtils.getMissingPermissions(permissions, grantResults);

    SequentialRequesterClass instance = SequentialRequesterClass.getInstance(listener);
    if (instance != null)
      listener = (RuntimePermissionListener) instance;

    if (missingPermissions == null) {
      listener.onRequestPermissionsGranted(requestCode, passthrough);
    }
    else {
      listener.onRequestPermissionsDenied(requestCode, passthrough, missingPermissions);
    }
  }

  public static void onActivityResult(RuntimePermissionListener listener, int requestCode, int resultCode, Intent data) {
    Object passthrough = RuntimePermissionUtils.getPassthroughCache(requestCode);

    SequentialRequesterClass instance = SequentialRequesterClass.getInstance(listener);
    if (instance != null)
      listener = (RuntimePermissionListener) instance;

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
