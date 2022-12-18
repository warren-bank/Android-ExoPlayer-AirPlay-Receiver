package com.github.warren_bank.exoplayer_airplay_receiver.ui;

import android.app.Activity;

public abstract class RuntimePermissionsListenerActivity extends Activity {
    public abstract void onPermissionsGranted();
    public abstract void onPermissionsDenied(String[] permissions);
}
