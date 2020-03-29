package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.service.NetworkingService;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.RepeatModeUtil;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class VideoActivity extends AppCompatActivity implements PlayerControlView.VisibilityListener, View.OnClickListener {
  private PlayerManager playerManager;
  private PlayerView    playerView;
  private Button        selectTracksButton;
  private Button        selectTextOffsetButton;
  private boolean       isShowingTrackSelectionDialog;
  private boolean       isShowingTextOffsetSelectionDialog;

  // Activity lifecycle methods.

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    playerManager = NetworkingService.getPlayerManager();
    if (playerManager == null) {
      finish();
      return;
    }

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_video);

    playerView = (PlayerView) findViewById(R.id.player_view);
    playerView.setControllerVisibilityListener(this);
    playerView.setKeepContentOnPlayerReset(false);
    playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);
    playerView.setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE | RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE | RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL);
    playerView.requestFocus();

    selectTracksButton = (Button) findViewById(R.id.select_tracks_button);
    selectTracksButton.setOnClickListener(this);
    selectTextOffsetButton = (Button) findViewById(R.id.select_text_offset_button);
    selectTextOffsetButton.setOnClickListener(this);
    isShowingTrackSelectionDialog      = false;
    isShowingTextOffsetSelectionDialog = false;
  }

  @Override
  protected void onStart() {
    super.onStart();
    playerManager.setPlayerView(playerView);
  }

  @Override
  protected void onStop() {
    super.onStop();
    playerManager.setPlayerView(null);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();

    playerManager.AirPlay_stop(/* play_animation= */ false);
  }

  // Activity input.

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    return super.dispatchKeyEvent(event) || playerManager.dispatchKeyEvent(event);
  }

  // Event handler interfaces.

  // PlayerControlView.VisibilityListener
  @Override
  public void onVisibilityChange(int visibility) {
    selectTracksButton.setVisibility(visibility);
    selectTextOffsetButton.setVisibility(visibility);

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

    if (
         view == selectTextOffsetButton
      && !isShowingTextOffsetSelectionDialog
    ) {
      isShowingTextOffsetSelectionDialog = true;
      MultiFieldTimePickerDialogContainer.show(
        /* context= */ this,
        playerManager.textSynchronizer,
        /* onDismissListener= */ dismissedDialog -> isShowingTextOffsetSelectionDialog = false
      );
    }
  }

}
