package com.github.warren_bank.exoplayer_airplay_receiver.ui.settings;

import com.github.warren_bank.exoplayer_airplay_receiver.R;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    reload();
  }

  public void reload() {
    setPreferenceScreen(null);
    addPreferencesFromResource(R.xml.preferences);
  }
}
