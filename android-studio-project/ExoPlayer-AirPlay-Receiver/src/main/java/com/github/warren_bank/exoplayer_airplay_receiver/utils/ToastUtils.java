package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.BuildConfig;
import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.PlayerManager;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.VideoSource;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ToastUtils {

  public static String interpolate_variables(Context context, PlayerManager playerManager, String text) {
    if (TextUtils.isEmpty(text)) return null;

    if (playerManager != null) {
      text = ToastUtils.interpolate_video_uri(context, playerManager, text);
      text = ToastUtils.interpolate_video_caption(context, playerManager, text);
      text = ToastUtils.interpolate_video_req_headers(context, playerManager, text);
      text = ToastUtils.interpolate_video_drm_scheme(context, playerManager, text);
      text = ToastUtils.interpolate_video_drm_server(context, playerManager, text);
      text = ToastUtils.interpolate_video_drm_headers(context, playerManager, text);
    }

    text = ToastUtils.interpolate_date(context, text);
    text = ToastUtils.interpolate_time(context, text);
    text = ToastUtils.interpolate_version(context, text);
    text = ToastUtils.interpolate_abi(context, text);
    text = ToastUtils.interpolate_top_process(context, text);
    text = ToastUtils.interpolate_top_activity(context, text);

    text = (TextUtils.isEmpty(text)) ? null : text.trim();

    return text;
  }

  private static VideoSource getVideoSource(PlayerManager playerManager) {
    if (playerManager == null) return null;

    return playerManager.getItem(
      playerManager.getCurrentItemIndex()
    );
  }

  private static String interpolate_video_uri(Context context, PlayerManager playerManager, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_video_uri);

    if (!text.contains(variable_substring))
      return text;

    try {
      VideoSource sample = getVideoSource(playerManager);
      String video_uri = (sample == null) ? null : sample.uri;

      return ToastUtils.interpolate_variable(text, variable_substring, video_uri);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_video_caption(Context context, PlayerManager playerManager, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_video_caption);

    if (!text.contains(variable_substring))
      return text;

    try {
      VideoSource sample = getVideoSource(playerManager);
      String video_caption = (sample == null) ? null : sample.caption;

      return ToastUtils.interpolate_variable(text, variable_substring, video_caption);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_video_req_headers(Context context, PlayerManager playerManager, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_video_req_headers);

    if (!text.contains(variable_substring))
      return text;

    try {
      VideoSource sample = getVideoSource(playerManager);
      String video_req_headers = (sample == null) ? null : StringUtils.toString(sample.reqHeadersMap);

      return ToastUtils.interpolate_variable(text, variable_substring, video_req_headers);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_video_drm_scheme(Context context, PlayerManager playerManager, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_video_drm_scheme);

    if (!text.contains(variable_substring))
      return text;

    try {
      VideoSource sample = getVideoSource(playerManager);
      String video_drm_scheme = (sample == null) ? null : sample.drm_scheme;

      return ToastUtils.interpolate_variable(text, variable_substring, video_drm_scheme);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_video_drm_server(Context context, PlayerManager playerManager, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_video_drm_server);

    if (!text.contains(variable_substring))
      return text;

    try {
      VideoSource sample = getVideoSource(playerManager);
      String video_drm_server = (sample == null) ? null : sample.drm_license_server;

      return ToastUtils.interpolate_variable(text, variable_substring, video_drm_server);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
  }

  private static String interpolate_video_drm_headers(Context context, PlayerManager playerManager, String text) {
    if (TextUtils.isEmpty(text)) return null;

    String variable_substring = context.getString(R.string.toast_variable_video_drm_headers);

    if (!text.contains(variable_substring))
      return text;

    try {
      VideoSource sample = getVideoSource(playerManager);
      String video_drm_headers = (sample == null) ? null : StringUtils.toString(sample.drmHeadersMap);

      return ToastUtils.interpolate_variable(text, variable_substring, video_drm_headers);
    }
    catch(Exception e) {
      return ToastUtils.interpolate_variable(text, variable_substring, "");
    }
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
