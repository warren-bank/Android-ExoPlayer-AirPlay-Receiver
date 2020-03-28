package com.github.warren_bank.exoplayer_airplay_receiver.ui.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.SystemUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.WakeLockMgr;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.ArrayList;

public class VideoActivity extends AppCompatActivity implements PlayerControlView.VisibilityListener, View.OnClickListener {
  private static final String tag = "VideoActivity";

  private final ArrayList<Intent> externalStorageIntents = new ArrayList<Intent>();

  private PlayerView    playerView;
  private Button        selectTracksButton;
  private Button        selectTextOffsetButton;
  private boolean       isShowingTrackSelectionDialog;
  private boolean       isShowingTextOffsetSelectionDialog;
  private boolean       isPlayingAudioWithScreenOff;
  private boolean       didPauseVideo;
  private boolean       didWakeLock;

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
    selectTextOffsetButton = (Button) findViewById(R.id.select_text_offset_button);
    selectTextOffsetButton.setOnClickListener(this);
    isShowingTrackSelectionDialog      = false;
    isShowingTextOffsetSelectionDialog = false;
    isPlayingAudioWithScreenOff        = false;
    didPauseVideo = false;
    didWakeLock   = false;

    playerManager = PlayerManager.createPlayerManager(/* context= */ this, playerView);

    handleIntent(getIntent());
  }

  @Override
  public void onNewIntent(Intent intent) {
    handleIntent(intent);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (ExternalStorageUtils.is_permission_granted(requestCode, grantResults)) {
      handleExternalStorageIntents();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();

    if (isPlayingAudioWithScreenOff)
      return;

    boolean isScreenOn, shouldFinish, shouldPause, shouldWakeLock;

    isScreenOn     = SystemUtils.isScreenOn(/* context= */ this);
    shouldFinish   = isScreenOn
                       || (playerManager == null)
                       || !playerManager.isPlayerReady();
    shouldPause    = !shouldFinish
                       && !playerManager.isPlayerPaused()
                       && !TextUtils.isEmpty(playerManager.getCurrentVideoMimeType());
    shouldWakeLock = !shouldFinish
                       && !shouldPause;

    if (shouldFinish) {
      if (playerManager != null) {
        playerManager.release();
        playerManager = null;
      }
      finish();
    }
    else if (shouldPause) {
      // screen is turned off, player is ready and not paused, source contains a video track.
      playerManager.AirPlay_rate(0f);
    }
    else if (shouldWakeLock) {
      // screen is turned off, player is ready and not paused, source only contains an audio track.
      // allow it to continue with the screen turned off.

      // this wakelock is to keep the Activity responsive to HTTP commands.
      // ExoPlayer would continue playing music in the queue without it.
      WakeLockMgr.acquire(/* context= */ this);
    }

    didPauseVideo = shouldPause;
    didWakeLock   = shouldWakeLock;
  }

  @Override
  protected void onRestart () {
    super.onRestart();

    if (isPlayingAudioWithScreenOff) {
      boolean isScreenOn = SystemUtils.isScreenOn(/* context= */ this);

      if (isScreenOn)
        isPlayingAudioWithScreenOff = false;
      else
        return;
    }

    if (didPauseVideo && (playerManager != null))
      playerManager.AirPlay_rate(1f);

    if (didWakeLock)
      WakeLockMgr.release();

    didPauseVideo = false;
    didWakeLock   = false;
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

  // Internal methods.

  private void handleExternalStorageIntents() {
    final Handler handler   = new Handler();
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        if (!externalStorageIntents.isEmpty()) {
          handleIntent(externalStorageIntents.remove(0));
          handler.postDelayed(this, 1000l);
        }
      }
    };

    handler.post(runnable);
  }

  private void handleIntent(Intent intent) {
    if (intent == null)
      return;

    if (isFinishing() || (playerManager == null)) {
      setIntent(intent);
      recreate();
      return;
    }

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

    boolean requiresExternalStoragePermission = false;
    if (ExternalStorageUtils.isFileUri(uri)) {
      uri = ExternalStorageUtils.normalizeFileUri(uri);
      requiresExternalStoragePermission = true;
    }
    if (ExternalStorageUtils.isFileUri(caption)) {
      caption = ExternalStorageUtils.normalizeFileUri(caption);
      requiresExternalStoragePermission = true;
    }
    if (requiresExternalStoragePermission && !ExternalStorageUtils.has_permission(this)) {
      externalStorageIntents.add(intent);
      ExternalStorageUtils.request_permission(this);
      return;
    }

    if ((mode != null) && mode.equals("queue")) {
      playerManager.AirPlay_queue(uri, caption, referer, startPosition);
      Log.d(tag, "queue video: url = " + uri + "; position = " + startPosition + "; captions = " + caption + "; referer = " + referer);
    }
    else /* if ((mode != null) && mode.equals("play")) */ {
      playerManager.AirPlay_play(uri, caption, referer, startPosition);
      Log.d(tag, "play video: url = "  + uri + "; position = " + startPosition + "; captions = " + caption + "; referer = " + referer);

      if (!isPlayingAudioWithScreenOff) {
        boolean isScreenOn = SystemUtils.isScreenOn(/* context= */ this);

        if (!isScreenOn) {
          // user has initiated media playback while the screen is turned off
          //
          // lifecycle with screen off:
          // - onCreate()    => handleIntent() => onStop()
          // - onNewIntent() => handleIntent() => onRestart() => onStop()
          //
          // onStop() runs immediately
          // - need to be careful that the HTTP command to begin playback
          //   doesn't construct an instance of ExoPlayer..
          //   only to immediately send it to garbage collection
          //
          // - under normal conditions:
          //   * media playback is started with the screen on
          //   * the Activity opens in the foreground
          //   * ExoPlayer has time to load metadata for items in the playlist
          //   * when the screen is subsequently turned off,
          //     onStop() has the ability to query whether the current playlist item is audio-only
          //
          // - in this situation:
          //   * onStop() can't query metadata about the current playlist item,
          //     because ExoPlayer hasn't been given time to gather this info
          //
          // only audio media should be allowed to begin playback when the screen is off.
          // since the URL for the media is currently known, a viable workaround is to:
          // - use a regex to test whether the URL matches a known audio file extension
          // - set a variable to serves as a flag that indicates when this feature is active
          // - clear the flag when the screen is turned back on

          isPlayingAudioWithScreenOff = VideoSource.isAudioFileUrl(uri);

          if (isPlayingAudioWithScreenOff) {
            // this wakelock is to keep the Activity responsive to HTTP commands.
            // ExoPlayer would continue playing music in the queue without it.

            WakeLockMgr.acquire(/* context= */ this);
            didWakeLock = true;
          }
        }
      }
    }
  }

}
