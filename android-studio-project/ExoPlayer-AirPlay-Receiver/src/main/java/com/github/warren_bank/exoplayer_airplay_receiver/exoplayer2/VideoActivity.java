package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

/*
 * based on:
 *   https://github.com/androidx/media/blob/1.0.0-beta03/demos/main/src/main/java/androidx/media3/demo/main/PlayerActivity.java
 *   https://github.com/androidx/media/blob/1.0.0-beta03/demos/main/src/main/res/layout/player_activity.xml
 */

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.service.NetworkingService;

import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.util.RepeatModeUtil;
import androidx.media3.ui.PlayerView;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class VideoActivity extends AppCompatActivity implements PlayerView.ControllerVisibilityListener, View.OnClickListener {
  public static boolean isVisible = false;

  private PlayerManager playerManager;
  private PlayerView    playerView;
  private Button        selectTracksButton;
  private Button        selectTextOffsetButton;
  private Button        toggleDownloadButton;
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
    toggleDownloadButton = (Button) findViewById(R.id.toggle_download_button);
    toggleDownloadButton.setOnClickListener(this);
    isShowingTrackSelectionDialog      = false;
    isShowingTextOffsetSelectionDialog = false;
  }

  @Override
  protected void onStart() {
    super.onStart();
    playerManager.setPlayerView(playerView);
    isVisible = true;
  }

  @Override
  protected void onStop() {
    super.onStop();
    playerManager.setPlayerView(null);
    isVisible = false;
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

  // PlayerView.ControllerVisibilityListener
  @Override
  public void onVisibilityChanged(int visibility) {
    if (visibility == View.VISIBLE)
      updateButtons(/* textOnly= */ false);

    selectTracksButton.setVisibility(visibility);
    selectTextOffsetButton.setVisibility(visibility);
    toggleDownloadButton.setVisibility(visibility);
  }

  // View.OnClickListener
  @Override
  public void onClick(View view) {
    if (
         view == selectTracksButton
      && !isShowingTrackSelectionDialog
      && (playerManager != null)
      && (playerManager.exoPlayer != null)
      && TrackSelectionDialog.willHaveContent(playerManager.exoPlayer)
    ) {
      isShowingTrackSelectionDialog = true;
      TrackSelectionDialog trackSelectionDialog = TrackSelectionDialog.createForPlayer(
        playerManager.exoPlayer,
        /* onDismissListener= */ dismissedDialog -> isShowingTrackSelectionDialog = false
      );
      trackSelectionDialog.show(getSupportFragmentManager(), /* tag= */ null);
    }

    if (
         view == selectTextOffsetButton
      && !isShowingTextOffsetSelectionDialog
      && (playerManager != null)
      && (playerManager.textSynchronizer != null)
    ) {
      isShowingTextOffsetSelectionDialog = true;
      MultiFieldTimePickerDialogContainer.show(
        /* context= */ this,
        playerManager.textSynchronizer,
        /* onDismissListener= */ dismissedDialog -> isShowingTextOffsetSelectionDialog = false
      );
    }

    if (
         view == toggleDownloadButton
      && (playerManager != null)
      && (playerManager.getCurrentItem() != null)
    ) {
      playerManager.toggleCurrentItemUseCache();
      updateButtons(/* textOnly= */ true);
    }
  }

  private void updateButtons(boolean textOnly) {
    if (!textOnly) {
      selectTracksButton.setEnabled(
        (playerManager != null) && (playerManager.exoPlayer != null) && TrackSelectionDialog.willHaveContent(playerManager.exoPlayer)
      );
      selectTextOffsetButton.setEnabled(
        (playerManager != null) && (playerManager.textSynchronizer != null)
      );
      toggleDownloadButton.setEnabled(
        (playerManager != null) && (playerManager.getCurrentItem() != null)
      );
    }

    toggleDownloadButton.setText(
      ((playerManager != null) && playerManager.doesCurrentItemUseCache())
        ? R.string.toggle_download_button_stop
        : R.string.toggle_download_button_start
    );
  }

}
