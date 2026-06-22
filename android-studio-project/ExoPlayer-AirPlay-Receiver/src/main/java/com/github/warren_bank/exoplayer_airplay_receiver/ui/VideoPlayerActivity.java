package com.github.warren_bank.exoplayer_airplay_receiver.ui;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.VideoActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.PipUtils;

import androidx.annotation.RequiresApi;
import androidx.media3.ui.PlayerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;

import java.lang.ref.WeakReference;

public class VideoPlayerActivity extends VideoActivity implements PlayerView.FullscreenButtonClickListener {
  public static boolean isPipMode = false;

  private boolean enterPipMode;

  private Handler handler;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    isPipMode = false;

    handler = new VideoHandler(this);
    MainApp.registerHandler(VideoPlayerActivity.class.getName(), handler);

    processIntent(getIntent());

    if (PipUtils.supportsPictureInPictureMode(this))
      playerView.setFullscreenButtonClickListener(this);
  }

  @Override
  protected void onResume() {
    super.onResume();

    updatePictureInPictureMode(null);
  }

  @Override
  public void onNewIntent (Intent intent) {
    super.onNewIntent(intent);

    processIntent(intent);
    updatePictureInPictureMode(intent);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    isPipMode = false;

    MainApp.unregisterHandler(VideoPlayerActivity.class.getName());
  }

  @Override
  @RequiresApi(24)
  public void onPictureInPictureModeChanged (boolean isInPictureInPictureMode) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode);

    isPipMode = isInPictureInPictureMode;
  }

  private void processIntent(Intent intent) {
    enterPipMode = intent.getBooleanExtra(Constant.Extra.ENTER_PIP_MODE, false);
  }

  private void updatePictureInPictureMode(Intent intent) {
    if (!PipUtils.supportsPictureInPictureMode(this)) return;

    playerView.setFullscreenButtonState(!enterPipMode);

    if (!isPipMode && enterPipMode) {
      onVisibilityChanged(View.GONE);
      isPipMode = true; // redundant to avoid race condition: will also be set by onPictureInPictureModeChanged(), but should be set early for onVisibilityChanged() to prevent FOUC
      enterPipMode = false;
      PipUtils.enterPictureInPictureMode(this);
    }
    else if (isPipMode && !enterPipMode && (intent != null)) {
      isPipMode = false;
      PipUtils.exitPictureInPictureMode(this, intent);
    }
  }

  private void togglePictureInPictureMode() {
    if (enterPipMode) return;

    if (!isPipMode) {
      enterPipMode = true;
      updatePictureInPictureMode(null);
    }
    else {
      updatePictureInPictureMode(getIntent());
    }
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
        case Constant.Msg.Msg_Exit_Service :
          activity.finish();
          break;
      }

    }
  }

  // Activity input.

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    boolean isHandled = super.dispatchKeyEvent(event);

    if (!isHandled && (event.getRepeatCount() == 0) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
      // apply custom handler(s)
      switch(event.getKeyCode()) {

        case KeyEvent.KEYCODE_WINDOW : {
          togglePictureInPictureMode();
          isHandled = true;
          break;
        }
      }
    }

    return isHandled;
  }

  // Event handler interfaces.

  // PlayerView.FullscreenButtonClickListener
  @Override
  public void onFullscreenButtonClick(boolean isFullscreen) {
    if (!isFullscreen) {
      enterPipMode = true;
      updatePictureInPictureMode(null);
    }
  }

  // PlayerView.ControllerVisibilityListener
  @Override
  public void onVisibilityChanged(int visibility) {
    if (isPipMode)
      visibility = View.GONE;

    super.onVisibilityChanged(visibility);
  }
}
