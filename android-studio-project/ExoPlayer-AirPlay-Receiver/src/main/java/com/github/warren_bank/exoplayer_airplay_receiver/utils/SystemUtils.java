package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.hardware.display.DisplayManager;
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
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isScreenOn();
        }
    }

}
