package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.content.Context;
import android.os.PowerManager;

public final class WakeLockMgr {
    private static PowerManager.WakeLock wakeLock;

    public static void acquire(Context context) {
        release();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WakeLock"
        );
        wakeLock.acquire();
    }

    public static void release() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
