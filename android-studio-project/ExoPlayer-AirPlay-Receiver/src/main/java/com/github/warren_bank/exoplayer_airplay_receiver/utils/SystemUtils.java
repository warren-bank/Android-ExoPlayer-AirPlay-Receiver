package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.view.Display;

public class SystemUtils {

    /**
     * Is the power state of the display screen on?
     * @param context
     * @return true when (at least one) screen is turned on
     *
     * source: https://stackoverflow.com/a/28747907
     */
    public static boolean isScreenOn(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        }
        else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isScreenOn();
        }
    }

    /**
    * Returns the total amount of RAM in the current Android device in Bytes (ex: 1610612736 == 1.5 GiB)
    * @return {Long}
    */
    public static long getMemorySizeInBytes(Context context) {
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            long totalMemory = memoryInfo.totalMem;
            return totalMemory;
        }
        else {
            return -1l;
        }
    }

}
