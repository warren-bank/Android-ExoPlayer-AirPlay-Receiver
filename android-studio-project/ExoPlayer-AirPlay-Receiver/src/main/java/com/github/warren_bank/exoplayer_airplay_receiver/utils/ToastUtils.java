package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.BuildConfig;
import com.github.warren_bank.exoplayer_airplay_receiver.R;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ToastUtils {

  public static String interpolate_variables(Context context, String text) {
    if (TextUtils.isEmpty(text)) return null;

    text = ToastUtils.interpolate_date(context, text);
    text = ToastUtils.interpolate_time(context, text);
    text = ToastUtils.interpolate_version(context, text);
    text = ToastUtils.interpolate_abi(context, text);
    text = ToastUtils.interpolate_top_process(context, text);
    text = ToastUtils.interpolate_top_activity(context, text);

    text = (TextUtils.isEmpty(text)) ? null : text.trim();

    return text;
  }

  private static String interpolate_date(Context context, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_date);

    if (!text.contains(variable_substring))
      return text;

    try {
      SimpleDateFormat formatter = new SimpleDateFormat("EEE, MM/dd/yyyy");
      String date = formatter.format(new Date());

      return ToastUtils.interpolate_variable(text, variable_substring, date);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_time(Context context, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_time);

    if (!text.contains(variable_substring))
      return text;

    try {
      SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
      String time = formatter.format(new Date());

      return ToastUtils.interpolate_variable(text, variable_substring, time);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_version(Context context, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_version);

    if (!text.contains(variable_substring))
      return text;

    try {
      String appname = context.getString(R.string.app_name);
      String version = appname + " " + BuildConfig.VERSION_NAME;

      return ToastUtils.interpolate_variable(text, variable_substring, version);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_abi(Context context, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_abi);

    if (!text.contains(variable_substring))
      return text;

    try {
      String abi;

      if (Build.VERSION.SDK_INT >= 21) {
        abi = TextUtils.join(", ", Build.SUPPORTED_ABIS);
      }
      else {
        abi = TextUtils.isEmpty(Build.CPU_ABI)
          ? null
          : TextUtils.isEmpty(Build.CPU_ABI2)
            ? Build.CPU_ABI
            : Build.CPU_ABI + ", " + Build.CPU_ABI2
        ;
      }

      return ToastUtils.interpolate_variable(text, variable_substring, abi);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_top_process(Context context, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_top_process);

    if (!text.contains(variable_substring))
      return text;

    try {
      String top_process = null;

      ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

      // Android 5.1.1+ only returns the process that is running ExoAirPlayer
      for (ActivityManager.RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
        if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
          top_process = processInfo.processName;
          break;
        }
      }

      return ToastUtils.interpolate_variable(text, variable_substring, top_process);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_top_activity(Context context, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_top_activity);

    if (!text.contains(variable_substring))
      return text;

    try {
      String top_activity = null;

      if (Build.VERSION.SDK_INT < 21) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningTaskInfo taskInfo : am.getRunningTasks(3)) {
          if (taskInfo.topActivity != null) {
            top_activity = taskInfo.topActivity.getClassName();
            break;
          }
        }
      }

      return ToastUtils.interpolate_variable(text, variable_substring, top_activity);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_variable(String text, String variable, String value) {
    if (TextUtils.isEmpty(text))
      return null;
    if (TextUtils.isEmpty(variable))
      return text;
    if (value == null)
      value = "";

    try {
      return text.replace((CharSequence) variable, (CharSequence) value);
    }
    catch(Exception e) {
      return text;
    }
  }

}
