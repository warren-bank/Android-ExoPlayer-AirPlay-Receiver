package com.github.warren_bank.exoplayer_airplay_receiver.ui;

import java.lang.ref.WeakReference;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.exoplayer2.VideoActivity;

public class VideoPlayerActivity extends VideoActivity {
  private static final String tag = "VideoPlayerActivity";

  private static volatile boolean isVideoActivityFinished = false;
  private static volatile long    duration    = 0l;
  private static volatile long    curPosition = 0l;

  private Handler  handler;
  private Handler  timer;
  private Runnable timerTask;

  public static boolean isVideoActivityFinished() {
    return isVideoActivityFinished;
  }

  public static long getDuration() {
    return duration;
  }

  public static long getCurrentPosition() {
    return curPosition;
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    handler = new VideoHandler(this);
    MainApp.registerHandler(VideoPlayerActivity.class.getName(), handler);

    timerTask = new Runnable() {
      @Override
      public void run() {
        isVideoActivityFinished = ((playerManager == null) || (playerManager.isPlayerReady() == false));

        if (isVideoActivityFinished) {
          duration    = 0l;
          curPosition = 0l;
        }
        else {
          duration    = playerManager.getCurrentVideoDuration();
          curPosition = playerManager.getCurrentVideoPosition();
        }

        // scheduleAtFixedRate
        timer.postDelayed(this, 1 * 1000);
      }
    };

    timer = new Handler();
    timer.postDelayed(timerTask, 1 * 1000);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.d(tag, "airplay VideoPlayerActivity onDestroy");

    if (timer != null) {
      timer.removeCallbacks(timerTask);
      timer = null;
    }

    MainApp.unregisterHandler(VideoPlayerActivity.class.getName());
  }

  private static class VideoHandler extends Handler {
    private WeakReference<VideoPlayerActivity> weakReference;

    public VideoHandler(VideoPlayerActivity activity) {
      weakReference = new WeakReference<VideoPlayerActivity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);

      VideoPlayerActivity activity = weakReference.get();
      if (activity == null)
        return;
      if (activity.isFinishing())
        return;
      if (activity.playerManager == null)
        return;

      switch (msg.what) {
        case Constant.Msg.Msg_Video_Seek :
          float positionSec = (float) msg.obj;
          activity.playerManager.AirPlay_scrub(positionSec);
          break;
        case Constant.Msg.Msg_Video_Rate :
          float rate = (float) msg.obj;
          activity.playerManager.AirPlay_rate(rate);
          break;
        case Constant.Msg.Msg_Stop :
          activity.playerManager.AirPlay_stop();
        //activity.finish();
          break;
        case Constant.Msg.Msg_Photo :
          activity.finish();
          break;

        case Constant.Msg.Msg_Video_Next :
          activity.playerManager.AirPlay_next();
          break;
        case Constant.Msg.Msg_Video_Prev :
          activity.playerManager.AirPlay_previous();
          break;
        case Constant.Msg.Msg_Audio_Volume :
          float audioVolume = (float) msg.obj;
          activity.playerManager.AirPlay_volume(audioVolume);
          break;
      }

    }
  }
}
