package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

public final class WakeLockMgr {
  private static PowerManager.WakeLock     wakeLock;
  private static WifiManager.WifiLock      wifiLock;
  private static WifiManager.MulticastLock mdnsLock;

  public static void acquire(Context context) {
    release();

    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    WifiManager  wm = (WifiManager)  context.getSystemService(Context.WIFI_SERVICE);

    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
    wakeLock.setReferenceCounted(false);
    wakeLock.acquire();

    wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WifiLock");
    wifiLock.setReferenceCounted(false);
    wifiLock.acquire();

    mdnsLock = wm.createMulticastLock("WifiMulticastLock");
    mdnsLock.setReferenceCounted(false);
    mdnsLock.acquire();
  }

  public static void release() {

    if (wakeLock != null) {
      if (wakeLock.isHeld())
        wakeLock.release();
      wakeLock = null;
    }

    if (wifiLock != null) {
      if (wifiLock.isHeld())
        wifiLock.release();
      wifiLock = null;
    }

    if (mdnsLock != null) {
      if (mdnsLock.isHeld())
        mdnsLock.release();
      mdnsLock = null;
    }

  }
}
