package com.github.warren_bank.exoplayer_airplay_receiver.service;

import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.PlayerManager;
import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.PlaybackStatusMonitor;

import android.os.Handler;
import android.os.Looper;

public final class MyPlaybackStatusMonitor extends PlaybackStatusMonitor {
  private static final int intervalMs = 10 * 1000; // update static data fields every 10 seconds

  private PlayerManager playerManager;
  private Runnable      timerTask;
  private Handler       timer;
  private boolean       isStopped;

  public MyPlaybackStatusMonitor() {
    isStopped = true;
  }

  @Override
  public void start() {
    isStopped = false;

    Looper.prepare();

    timerTask = new Runnable() {
      @Override
      public void run() {
        if (isStopped) {
          Looper.myLooper().quit();
          return;
        }

        playerManager    = NetworkingService.getPlayerManager();
        playbackFinished = ((playerManager == null) || (playerManager.isPlayerReady() == false));

        if (playbackFinished) {
          duration    = 0l;
          curPosition = 0l;
        }
        else {
          duration    = playerManager.getCurrentVideoDuration();
          curPosition = playerManager.getCurrentVideoPosition();
        }

        // scheduleAtFixedRate
        timer.postDelayed(this, intervalMs);
      }
    };

    timer = new Handler();
    timer.postDelayed(timerTask, intervalMs);

    Looper.loop();
  }

  @Override
  public void stop() {
    isStopped = true;
  }

}
