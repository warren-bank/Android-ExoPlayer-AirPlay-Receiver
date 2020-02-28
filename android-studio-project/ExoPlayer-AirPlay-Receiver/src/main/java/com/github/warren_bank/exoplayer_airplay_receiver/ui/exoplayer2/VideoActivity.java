package com.github.warren_bank.exoplayer_airplay_receiver.ui.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;

public class VideoActivity extends AppCompatActivity implements PlayerControlView.VisibilityListener, View.OnClickListener {
  private static final String tag = "VideoActivity";

  private PlayerView    playerView;
  private Button        selectTracksButton;
  private boolean       isShowingTrackSelectionDialog;

  public  PlayerManager playerManager;

  // Activity lifecycle methods.

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_video);

    playerView = (PlayerView) findViewById(R.id.player_view);
    playerView.setControllerVisibilityListener(this);
    playerView.requestFocus();

    selectTracksButton = (Button) findViewById(R.id.select_tracks_button);
    selectTracksButton.setOnClickListener(this);
    isShowingTrackSelectionDialog = false;

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

  // Event handler interfaces.

  // PlayerControlView.VisibilityListener
  @Override
  public void onVisibilityChange(int visibility) {
    selectTracksButton.setVisibility(visibility);

    if (visibility == View.VISIBLE) {
      selectTracksButton.setEnabled(
        (playerManager != null) && TrackSelectionDialog.willHaveContent(playerManager.trackSelector)
      );
    }
  }

  // View.OnClickListener
  @Override
  public void onClick(View view) {
    if (
         view == selectTracksButton
      && !isShowingTrackSelectionDialog
      && TrackSelectionDialog.willHaveContent(playerManager.trackSelector)
    ) {
      isShowingTrackSelectionDialog = true;
      TrackSelectionDialog trackSelectionDialog = TrackSelectionDialog.createForTrackSelector(
        playerManager.trackSelector,
        /* onDismissListener= */ dismissedDialog -> isShowingTrackSelectionDialog = false
      );
      trackSelectionDialog.show(getSupportFragmentManager(), /* tag= */ null);
    }
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
    String caption      = intent.getStringExtra("caption");
    String referer      = intent.getStringExtra("referer");
    float startPosition = (float) intent.getDoubleExtra("startPosition", 0);

    // normalize empty data fields to: null
    if (TextUtils.isEmpty(uri))
      uri = null;
    if (TextUtils.isEmpty(caption))
      caption = null;
    if (TextUtils.isEmpty(referer))
      referer = null;

    // ignore bad requests
    if (uri == null)
      return;

    if ((mode != null) && mode.equals("queue")) {
      playerManager.AirPlay_queue(uri, caption, referer, startPosition);
      Log.d(tag, "queue video: url = " + uri + "; position = " + startPosition + "; captions = " + caption + "; referer = " + referer);
    }
    else /* if ((mode != null) && mode.equals("play")) */ {
      playerManager.AirPlay_play(uri, caption, referer, startPosition);
      Log.d(tag, "play video: url = "  + uri + "; position = " + startPosition + "; captions = " + caption + "; referer = " + referer);
    }
  }

}
