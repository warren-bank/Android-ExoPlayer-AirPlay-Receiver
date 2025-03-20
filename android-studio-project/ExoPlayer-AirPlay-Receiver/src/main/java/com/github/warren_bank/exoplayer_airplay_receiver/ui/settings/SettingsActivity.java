package com.github.warren_bank.exoplayer_airplay_receiver.ui.settings;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;

import java.lang.ref.WeakReference;

public class SettingsActivity extends PreferenceActivity {
  private Handler handler;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SettingsFragment settingsFragment = new SettingsFragment();
    getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();

    handler = new SettingsHandler(settingsFragment);
    MainApp.registerHandler(SettingsActivity.class.getName(), handler);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    MainApp.unregisterHandler(SettingsActivity.class.getName());
  }

  private static class SettingsHandler extends Handler {
    final private WeakReference<SettingsFragment> weakReference;
    final private Runnable reloadRunnable;

    public SettingsHandler(SettingsFragment settingsFragment) {
      weakReference = new WeakReference<SettingsFragment>(settingsFragment);

      reloadRunnable = new Runnable() {
        @Override
        public void run() {
          SettingsFragment settingsFragment = weakReference.get();

          if (settingsFragment == null)
            return;
          if (settingsFragment.isDetached() || settingsFragment.isRemoving())
            return;

          settingsFragment.reload();
        }
      };
    }

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);

      switch (msg.what) {
        case Constant.Msg.Msg_Preferences_Edit :
          reload(500l);
          break;
      }
    }

    private void reload(long delayMillis) {
      postDelayed(reloadRunnable, delayMillis);
    }
  }
}
