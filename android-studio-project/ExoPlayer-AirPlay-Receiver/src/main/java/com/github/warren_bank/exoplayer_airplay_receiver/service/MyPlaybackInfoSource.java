package com.github.warren_bank.exoplayer_airplay_receiver.service;

import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.PlayerManager;
import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.RequestListenerThread;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

public class MyPlaybackInfoSource implements RequestListenerThread.PlaybackInfoSource {

  private final PlayerManager playerManager;
  private final Object monitor;
  private final Handler handler;
  private final Runnable refresher;

  private JSONObject info;

  // ----------------------
  // called from UI thread:
  // ----------------------

  public MyPlaybackInfoSource(PlayerManager playerManager) {
    this.info          = null;
    this.playerManager = playerManager;
    this.monitor       = new Object();
    this.handler       = new Handler(Looper.getMainLooper());
    this.refresher     = new Runnable() {
      @Override
      public void run() {
        if (playerManager != null) {
          synchronized (monitor) {
            info = playerManager.getCurrentItemJSONObject();
            try {
              monitor.notifyAll();
            }
            catch(IllegalMonitorStateException e) {}
          }
        }
      }
    };
  }

  // ------------------------------
  // called from networking thread:
  // ------------------------------

  public boolean refresh() {
    if (playerManager != null) {
      synchronized (monitor) {
        handler.post(refresher);
        try {
          monitor.wait();
          return true;
        }
        catch(InterruptedException e) {}
      }
    }
    return false;
  }

  public boolean release() {
    if (playerManager != null) {
      synchronized (monitor) {
        info = null;
        return true;
      }
    }
    return false;
  }

  private boolean isPlayerReady() {
    try {
      return ((info != null) && info.getBoolean(Constant.MediaItemInfo.IS_PLAYER_READY));
    }
    catch(Exception e) {
      return false;
    }
  }

  public boolean isPlaybackFinished() {
    synchronized (monitor) {
      return !isPlayerReady();
    }
  }

  // units: milliseconds
  public long getCurrentPosition() {
    synchronized (monitor) {
      try {
        if (!isPlayerReady()) throw new Exception("player is not ready");
        return info.getLong(Constant.MediaItemInfo.CURRENT_POSITION);
      }
      catch(Exception e) {
        return 0l;
      }
    }
  }

  // units: milliseconds
  public long getDuration() {
    synchronized (monitor) {
      try {
        if (!isPlayerReady()) throw new Exception("player is not ready");
        return info.getLong(Constant.MediaItemInfo.DURATION);
      }
      catch(Exception e) {
        return 0l;
      }
    }
  }

}
