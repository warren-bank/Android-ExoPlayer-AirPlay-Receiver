package com.github.warren_bank.exoplayer_airplay_receiver.ui.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.exoplayer2.ui.PlayerView;

public class VideoActivity extends Activity {
  private static final String tag = "VideoActivity";

  private PlayerView    playerView;
  public  PlayerManager playerManager;

  // Activity lifecycle methods.

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_video);

    playerView = (PlayerView) findViewById(R.id.player_view);
    playerView.requestFocus();

    playerManager = PlayerManager.createPlayerManager(/* context= */ this, playerView);

    handleIntent(getIntent());
  }

  @Override
  public void onNewIntent(Intent intent) {
    handleIntent(intent);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (playerManager != null) {
      playerManager.release();
      playerManager = null;
    }
    finish();
  }

  // Activity input.

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    // If the event was not handled then see if the player view can handle it.
    return super.dispatchKeyEvent(event) || playerManager.dispatchKeyEvent(event);
  }

  // Internal methods.

  private void handleIntent(Intent intent) {
    if (intent == null)
      return;

    if (isFinishing()) {
      setIntent(intent);
      recreate();
      return;
    }

    if (playerManager == null)
      return;

    String mode         = intent.getStringExtra("mode");
    String uri          = intent.getStringExtra("uri");
    String referer      = intent.getStringExtra("referer");
    float startPosition = (float) intent.getDoubleExtra("startPosition", 0);

    if (uri == null)
      return;

    if ((mode != null) && mode.equals("queue")) {
      playerManager.AirPlay_queue(uri, referer, startPosition);
      Log.d(tag, "queue video: url = " + uri + "; position = " + startPosition + "; referer = " + referer);
    }
    else /* if ((mode != null) && mode.equals("play")) */ {
      playerManager.AirPlay_play(uri, startPosition);
      Log.d(tag, "play video: url = " + uri + "; position = " + startPosition);
    }
  }

}
