package com.github.warren_bank.exoplayer_airplay_receiver.ui;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.VideoActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public class VideoPlayerActivity extends VideoActivity {
  private Handler handler;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    handler = new VideoHandler(this);
    MainApp.registerHandler(VideoPlayerActivity.class.getName(), handler);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

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

      switch (msg.what) {
        case Constant.Msg.Msg_Photo :
        case Constant.Msg.Msg_Hide_Player :
          activity.finish();
          break;
      }

    }
  }
}
