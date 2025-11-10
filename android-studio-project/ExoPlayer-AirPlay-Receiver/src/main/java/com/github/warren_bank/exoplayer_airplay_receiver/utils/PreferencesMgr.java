package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class PreferencesMgr {

  private static class OnSharedPreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      int pref_key_id = get_pref_key_id(key);
      if (pref_key_id == -1) return;

      update_internal_state(pref_key_id);
      notifyListeners(pref_key_id);
    }
  }

  // ---------------------------------------------------------------------------
  // internal helper: to map keys from String values to integer resource id values

  private static int[] pref_key_ids = new int[]{
    R.string.prefkey_http_api_password,
    R.string.prefkey_default_user_agent,
    R.string.prefkey_max_audio_volume_boost_db,
    R.string.prefkey_max_parallel_downloads,
    R.string.prefkey_seek_back_ms_increment,
    R.string.prefkey_seek_forward_ms_increment,
    R.string.prefkey_audio_volume_percent_increment,
    R.string.prefkey_audio_volume_boost_db_increment,
    R.string.prefkey_ts_extractor_timestamp_search_bytes_factor,
    R.string.prefkey_enable_tunneled_video_playback,
    R.string.prefkey_enable_hdmv_dts_audio_streams,
    R.string.prefkey_pause_on_change_to_audio_output_device,
    R.string.prefkey_prefer_extension_renderer,
    R.string.prefkey_enable_audio_passthrough,
    R.string.prefkey_enable_downmix_surround_sound_to_stereo,
    R.string.prefkey_enable_downmix_stereo_sound_to_mono
  };

  private static int get_pref_key_id(String key) {
    Context context = getApplicationContext();

    for (int i=0; i < pref_key_ids.length; i++) {
      if (key.equals(
        ResourceUtils.getString(context, pref_key_ids[i])
      )) {
        return pref_key_ids[i];
      }
    }

    return -1;
  }

  // ---------------------------------------------------------------------------
  // internal:

  private static Context getApplicationContext() {
    return (Context) MainApp.getInstance();
  }

  private static SharedPreferences getPrefs(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  private static SharedPreferences.Editor getPrefsEditor(Context context) {
    SharedPreferences prefs = getPrefs(context);

    return getPrefsEditor(prefs);
  }

  private static SharedPreferences.Editor getPrefsEditor(SharedPreferences prefs) {
    return prefs.edit();
  }

  // ---------------------------------------------------------------------------
  // internal generic getters:

  private static String getPrefString(int pref_key_id, int default_value_id) {
    Context context         = getApplicationContext();
    SharedPreferences prefs = getPrefs(context);

    return getPrefString(context, prefs, pref_key_id, default_value_id);
  }

  private static String getPrefString(Context context, SharedPreferences prefs, int pref_key_id, int default_value_id) {
    String pref_key         = ResourceUtils.getString(context, pref_key_id);
    String default_value    = ResourceUtils.getString(context, default_value_id);

    return prefs.getString(pref_key, default_value);
  }

  // -----------------------------------

  private static boolean getPrefBoolean(int pref_key_id, int default_value_id) {
    Context context         = getApplicationContext();
    SharedPreferences prefs = getPrefs(context);

    return getPrefBoolean(context, prefs, pref_key_id, default_value_id);
  }

  private static boolean getPrefBoolean(Context context, SharedPreferences prefs, int pref_key_id, int default_value_id) {
    String pref_key         = ResourceUtils.getString(context, pref_key_id);
    boolean default_value   = ResourceUtils.getBoolean(context, default_value_id);

    return prefs.getBoolean(pref_key, default_value);
  }

  // -----------------------------------

  private static int getPrefInteger(int pref_key_id, int default_value_id) {
    Context context         = getApplicationContext();
    SharedPreferences prefs = getPrefs(context);

    return getPrefInteger(context, prefs, pref_key_id, default_value_id);
  }

  private static int getPrefInteger(Context context, SharedPreferences prefs, int pref_key_id, int default_value_id) {
    String pref_key         = ResourceUtils.getString(context, pref_key_id);
    int default_value       = ResourceUtils.getInteger(context, default_value_id);

    return prefs.getInt(pref_key, default_value);
  }

  // -----------------------------------

  private static float getPrefFloat(int pref_key_id, int default_value_id) {
    Context context         = getApplicationContext();
    SharedPreferences prefs = getPrefs(context);

    return getPrefFloat(context, prefs, pref_key_id, default_value_id);
  }

  private static float getPrefFloat(Context context, SharedPreferences prefs, int pref_key_id, int default_value_id) {
    String pref_key         = ResourceUtils.getString(context, pref_key_id);
    float default_value     = ResourceUtils.getFloat(context, default_value_id);

    return prefs.getFloat(pref_key, default_value);
  }

  // ---------------------------------------------------------------------------
  // internal getters:

  private static String get_http_api_password(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefString(
          /* pref_key_id= */      R.string.prefkey_http_api_password,
          /* default_value_id= */ R.string.prefval_http_api_password
        )
      : getPrefString(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_http_api_password,
          /* default_value_id= */ R.string.prefval_http_api_password
        )
    ;
  }

  private static String get_default_user_agent(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefString(
          /* pref_key_id= */      R.string.prefkey_default_user_agent,
          /* default_value_id= */ R.string.prefval_default_user_agent
        )
      : getPrefString(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_default_user_agent,
          /* default_value_id= */ R.string.prefval_default_user_agent
        )
    ;
  }

  private static int get_max_audio_volume_boost_db(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefInteger(
          /* pref_key_id= */      R.string.prefkey_max_audio_volume_boost_db,
          /* default_value_id= */ R.integer.prefval_max_audio_volume_boost_db
        )
      : getPrefInteger(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_max_audio_volume_boost_db,
          /* default_value_id= */ R.integer.prefval_max_audio_volume_boost_db
        )
    ;
  }

  private static int get_max_parallel_downloads(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefInteger(
          /* pref_key_id= */      R.string.prefkey_max_parallel_downloads,
          /* default_value_id= */ R.integer.prefval_max_parallel_downloads
        )
      : getPrefInteger(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_max_parallel_downloads,
          /* default_value_id= */ R.integer.prefval_max_parallel_downloads
        )
    ;
  }

  private static int get_seek_back_ms_increment(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefInteger(
          /* pref_key_id= */      R.string.prefkey_seek_back_ms_increment,
          /* default_value_id= */ R.integer.prefval_seek_back_ms_increment
        )
      : getPrefInteger(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_seek_back_ms_increment,
          /* default_value_id= */ R.integer.prefval_seek_back_ms_increment
        )
    ;
  }

  private static int get_seek_forward_ms_increment(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefInteger(
          /* pref_key_id= */      R.string.prefkey_seek_forward_ms_increment,
          /* default_value_id= */ R.integer.prefval_seek_forward_ms_increment
        )
      : getPrefInteger(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_seek_forward_ms_increment,
          /* default_value_id= */ R.integer.prefval_seek_forward_ms_increment
        )
    ;
  }

  private static float get_audio_volume_percent_increment(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefFloat(
          /* pref_key_id= */      R.string.prefkey_audio_volume_percent_increment,
          /* default_value_id= */ R.dimen.prefval_audio_volume_percent_increment
        )
      : getPrefFloat(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_audio_volume_percent_increment,
          /* default_value_id= */ R.dimen.prefval_audio_volume_percent_increment
        )
    ;
  }

  private static float get_audio_volume_boost_db_increment(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefFloat(
          /* pref_key_id= */      R.string.prefkey_audio_volume_boost_db_increment,
          /* default_value_id= */ R.dimen.prefval_audio_volume_boost_db_increment
        )
      : getPrefFloat(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_audio_volume_boost_db_increment,
          /* default_value_id= */ R.dimen.prefval_audio_volume_boost_db_increment
        )
    ;
  }

  private static float get_ts_extractor_timestamp_search_bytes_factor(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefFloat(
          /* pref_key_id= */      R.string.prefkey_ts_extractor_timestamp_search_bytes_factor,
          /* default_value_id= */ R.dimen.prefval_ts_extractor_timestamp_search_bytes_factor
        )
      : getPrefFloat(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_ts_extractor_timestamp_search_bytes_factor,
          /* default_value_id= */ R.dimen.prefval_ts_extractor_timestamp_search_bytes_factor
        )
    ;
  }

  private static boolean get_enable_tunneled_video_playback(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefBoolean(
          /* pref_key_id= */      R.string.prefkey_enable_tunneled_video_playback,
          /* default_value_id= */ R.bool.prefval_enable_tunneled_video_playback
        )
      : getPrefBoolean(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_enable_tunneled_video_playback,
          /* default_value_id= */ R.bool.prefval_enable_tunneled_video_playback
        )
    ;
  }

  private static boolean get_enable_hdmv_dts_audio_streams(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefBoolean(
          /* pref_key_id= */      R.string.prefkey_enable_hdmv_dts_audio_streams,
          /* default_value_id= */ R.bool.prefval_enable_hdmv_dts_audio_streams
        )
      : getPrefBoolean(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_enable_hdmv_dts_audio_streams,
          /* default_value_id= */ R.bool.prefval_enable_hdmv_dts_audio_streams
        )
    ;
  }

  private static boolean get_pause_on_change_to_audio_output_device(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefBoolean(
          /* pref_key_id= */      R.string.prefkey_pause_on_change_to_audio_output_device,
          /* default_value_id= */ R.bool.prefval_pause_on_change_to_audio_output_device
        )
      : getPrefBoolean(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_pause_on_change_to_audio_output_device,
          /* default_value_id= */ R.bool.prefval_pause_on_change_to_audio_output_device
        )
    ;
  }

  private static boolean get_prefer_extension_renderer(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefBoolean(
          /* pref_key_id= */      R.string.prefkey_prefer_extension_renderer,
          /* default_value_id= */ R.bool.prefval_prefer_extension_renderer
        )
      : getPrefBoolean(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_prefer_extension_renderer,
          /* default_value_id= */ R.bool.prefval_prefer_extension_renderer
        )
    ;
  }

  private static boolean get_enable_audio_passthrough(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefBoolean(
          /* pref_key_id= */      R.string.prefkey_enable_audio_passthrough,
          /* default_value_id= */ R.bool.prefval_enable_audio_passthrough
        )
      : getPrefBoolean(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_enable_audio_passthrough,
          /* default_value_id= */ R.bool.prefval_enable_audio_passthrough
        )
    ;
  }

  private static boolean get_enable_downmix_surround_sound_to_stereo(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefBoolean(
          /* pref_key_id= */      R.string.prefkey_enable_downmix_surround_sound_to_stereo,
          /* default_value_id= */ R.bool.prefval_enable_downmix_surround_sound_to_stereo
        )
      : getPrefBoolean(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_enable_downmix_surround_sound_to_stereo,
          /* default_value_id= */ R.bool.prefval_enable_downmix_surround_sound_to_stereo
        )
    ;
  }

  private static boolean get_enable_downmix_stereo_sound_to_mono(Context context, SharedPreferences prefs) {
    return ((context == null) || (prefs == null))
      ? getPrefBoolean(
          /* pref_key_id= */      R.string.prefkey_enable_downmix_stereo_sound_to_mono,
          /* default_value_id= */ R.bool.prefval_enable_downmix_stereo_sound_to_mono
        )
      : getPrefBoolean(
          context,
          prefs,
          /* pref_key_id= */      R.string.prefkey_enable_downmix_stereo_sound_to_mono,
          /* default_value_id= */ R.bool.prefval_enable_downmix_stereo_sound_to_mono
        )
    ;
  }

  // ---------------------------------------------------------------------------
  // internal state:

  private static boolean is_initialized = false;

  private static OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

  private static String  http_api_password;
  private static String  default_user_agent;
  private static int     max_audio_volume_boost_db;
  private static int     max_parallel_downloads;
  private static int     seek_back_ms_increment;
  private static int     seek_forward_ms_increment;
  private static float   audio_volume_percent_increment;
  private static float   audio_volume_boost_db_increment;
  private static float   ts_extractor_timestamp_search_bytes_factor;
  private static boolean enable_tunneled_video_playback;
  private static boolean enable_hdmv_dts_audio_streams;
  private static boolean pause_on_change_to_audio_output_device;
  private static boolean prefer_extension_renderer;
  private static boolean enable_audio_passthrough;
  private static boolean enable_downmix_surround_sound_to_stereo;
  private static boolean enable_downmix_stereo_sound_to_mono;

  private static void initialize() {
    if (is_initialized) return;

    is_initialized          = true;
    Context context         = getApplicationContext();
    SharedPreferences prefs = getPrefs(context);

    onSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener();
    prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

    http_api_password                          = get_http_api_password(context, prefs);
    default_user_agent                         = get_default_user_agent(context, prefs);
    max_audio_volume_boost_db                  = get_max_audio_volume_boost_db(context, prefs);
    max_parallel_downloads                     = get_max_parallel_downloads(context, prefs);
    seek_back_ms_increment                     = get_seek_back_ms_increment(context, prefs);
    seek_forward_ms_increment                  = get_seek_forward_ms_increment(context, prefs);
    audio_volume_percent_increment             = get_audio_volume_percent_increment(context, prefs);
    audio_volume_boost_db_increment            = get_audio_volume_boost_db_increment(context, prefs);
    ts_extractor_timestamp_search_bytes_factor = get_ts_extractor_timestamp_search_bytes_factor(context, prefs);
    enable_tunneled_video_playback             = get_enable_tunneled_video_playback(context, prefs);
    enable_hdmv_dts_audio_streams              = get_enable_hdmv_dts_audio_streams(context, prefs);
    pause_on_change_to_audio_output_device     = get_pause_on_change_to_audio_output_device(context, prefs);
    prefer_extension_renderer                  = get_prefer_extension_renderer(context, prefs);
    enable_audio_passthrough                   = get_enable_audio_passthrough(context, prefs);
    enable_downmix_surround_sound_to_stereo    = get_enable_downmix_surround_sound_to_stereo(context, prefs);
    enable_downmix_stereo_sound_to_mono        = get_enable_downmix_stereo_sound_to_mono(context, prefs);
  }

  // ---------------------------------------------------------------------------
  // public getters:

  public static String get_http_api_password() {
    initialize();
    return http_api_password;
  }

  public static String get_default_user_agent() {
    initialize();
    return default_user_agent;
  }

  public static int get_max_audio_volume_boost_db() {
    initialize();
    return max_audio_volume_boost_db;
  }

  public static int get_max_parallel_downloads() {
    initialize();
    return max_parallel_downloads;
  }

  public static int get_seek_back_ms_increment() {
    initialize();
    return seek_back_ms_increment;
  }

  public static int get_seek_forward_ms_increment() {
    initialize();
    return seek_forward_ms_increment;
  }

  public static float get_audio_volume_percent_increment() {
    initialize();
    return audio_volume_percent_increment;
  }

  public static float get_audio_volume_boost_db_increment() {
    initialize();
    return audio_volume_boost_db_increment;
  }

  public static float get_ts_extractor_timestamp_search_bytes_factor() {
    initialize();
    return ts_extractor_timestamp_search_bytes_factor;
  }

  public static boolean get_enable_tunneled_video_playback() {
    initialize();
    return enable_tunneled_video_playback;
  }

  public static boolean get_enable_hdmv_dts_audio_streams() {
    initialize();
    return enable_hdmv_dts_audio_streams;
  }

  public static boolean get_pause_on_change_to_audio_output_device() {
    initialize();
    return pause_on_change_to_audio_output_device;
  }

  public static boolean get_prefer_extension_renderer() {
    initialize();
    return prefer_extension_renderer;
  }

  public static boolean get_enable_audio_passthrough() {
    initialize();
    return enable_audio_passthrough;
  }

  public static boolean get_enable_downmix_surround_sound_to_stereo() {
    initialize();
    return enable_downmix_surround_sound_to_stereo;
  }

  public static boolean get_enable_downmix_stereo_sound_to_mono() {
    initialize();
    return enable_downmix_stereo_sound_to_mono;
  }

  // ---------------------------------------------------------------------------
  // internal generic setters:

  private static boolean setPrefString(Context context, SharedPreferences.Editor editor, int pref_key_id, String old_value, String raw_value) {
    raw_value = StringUtils.isEmpty(raw_value) ? null : raw_value.trim();

    boolean did_edit = false;
    String new_value = raw_value;

    if (
      ((old_value != null) && !old_value.equals(new_value)) ||
      ((old_value == null) && (new_value != null))
    ) {
      did_edit = true;

      if (new_value == null) {
        editor.remove(
          ResourceUtils.getString(context, pref_key_id)
        );
      }
      else {
        editor.putString(
          ResourceUtils.getString(context, pref_key_id),
          new_value
        );
      }
    }

    return did_edit;
  }

  // -----------------------------------

  private static boolean setPrefBoolean(Context context, SharedPreferences.Editor editor, int pref_key_id, boolean old_value, String raw_value) {
    raw_value = StringUtils.isEmpty(raw_value) ? null : raw_value.trim();

    boolean did_edit  = false;
    Boolean new_value = (raw_value == null) ? null : Boolean.valueOf(StringUtils.normalizeBooleanString(raw_value));

    if ((new_value == null) || (new_value.booleanValue() != old_value)) {
      did_edit = true;

      if (new_value == null) {
        editor.remove(
          ResourceUtils.getString(context, pref_key_id)
        );
      }
      else {
        editor.putBoolean(
          ResourceUtils.getString(context, pref_key_id),
          new_value.booleanValue()
        );
      }
    }

    return did_edit;
  }

  // -----------------------------------

  private static boolean setPrefInteger(Context context, SharedPreferences.Editor editor, int pref_key_id, int old_value, String raw_value) {
    raw_value = StringUtils.isEmpty(raw_value) ? null : raw_value.trim();

    boolean did_edit = false;
    Integer new_value;
    try {
      new_value = (raw_value == null) ? null : Integer.valueOf(raw_value);
    }
    catch(Exception e) {
      new_value = null;
    }

    if ((new_value == null) || (new_value.intValue() != old_value)) {
      did_edit = true;

      if (new_value == null) {
        editor.remove(
          ResourceUtils.getString(context, pref_key_id)
        );
      }
      else {
        editor.putInt(
          ResourceUtils.getString(context, pref_key_id),
          new_value.intValue()
        );
      }
    }

    return did_edit;
  }

  // -----------------------------------

  private static boolean setPrefFloat(Context context, SharedPreferences.Editor editor, int pref_key_id, float old_value, String raw_value) {
    raw_value = StringUtils.isEmpty(raw_value) ? null : raw_value.trim();

    boolean did_edit = false;
    Float new_value;
    try {
      new_value = (raw_value == null) ? null : Float.valueOf(raw_value);

      if (new_value.isNaN()) throw new Exception("");
    }
    catch(Exception e) {
      new_value = null;
    }

    if ((new_value == null) || (new_value.floatValue() != old_value)) {
      did_edit = true;

      if (new_value == null) {
        editor.remove(
          ResourceUtils.getString(context, pref_key_id)
        );
      }
      else {
        editor.putFloat(
          ResourceUtils.getString(context, pref_key_id),
          new_value.floatValue()
        );
      }
    }

    return did_edit;
  }

  // ---------------------------------------------------------------------------
  // listeners:

  public interface OnPreferenceChangeListener {
    void onPreferenceChange(int pref_key_id);
  }

  private static ArrayList<OnPreferenceChangeListener> listeners = new ArrayList<OnPreferenceChangeListener>();

  public static void addOnPreferenceChangedListener(OnPreferenceChangeListener listener) {
    listeners.add(listener);
  }

  public static void removeOnPreferenceChangedListener(OnPreferenceChangeListener listener) {
    listeners.remove(listener);
  }

  private static void notifyListeners(int pref_key_id) {
    for (OnPreferenceChangeListener listener : listeners) {
      listener.onPreferenceChange(pref_key_id);
    }
  }

  // ---------------------------------------------------------------------------
  // public setters:

  public static void edit_preferences(HashMap<String, String> values) {
    if ((values == null) || values.isEmpty()) return;

    initialize();

    Context context                   = getApplicationContext();
    SharedPreferences prefs           = getPrefs(context);
    SharedPreferences.Editor editor   = getPrefsEditor(prefs);
    ArrayList<Integer> updated_ids    = new ArrayList<Integer>();
    String value;
    int pref_key_id;
    boolean did_edit;

    // pass 1: update persistent preferences
    for (String key : values.keySet()) {
      value = (String) values.get(key);

      // normalize API key to match SharedPreferences key
      key = key.replace('-', '_');

      pref_key_id = get_pref_key_id(key);
      if (pref_key_id == -1) continue;

      did_edit = update_preference(context, editor, pref_key_id, /* raw_value= */ value);
      if (did_edit) updated_ids.add(Integer.valueOf(pref_key_id));
    }

    if (updated_ids.isEmpty()) return;

    did_edit = editor.commit();
    if (!did_edit) return;

    // pass 2: update internal state
    for (Integer num : updated_ids) {
      pref_key_id = num.intValue();

      update_internal_state(context, prefs, pref_key_id);
    }

    // pass 3: notify listeners
    for (Integer num : updated_ids) {
      pref_key_id = num.intValue();

      notifyListeners(pref_key_id);
    }
  }

  private static boolean update_preference(Context context, SharedPreferences.Editor editor, int pref_key_id, String raw_value) {
    switch(pref_key_id) {
      case R.string.prefkey_http_api_password :
        return setPrefString(context, editor, pref_key_id, /* old_value= */ http_api_password, raw_value);

      case R.string.prefkey_default_user_agent :
        return setPrefString(context, editor, pref_key_id, /* old_value= */ default_user_agent, raw_value);

      case R.string.prefkey_max_audio_volume_boost_db :
        return setPrefInteger(context, editor, pref_key_id, /* old_value= */ max_audio_volume_boost_db, raw_value);

      case R.string.prefkey_max_parallel_downloads :
        return setPrefInteger(context, editor, pref_key_id, /* old_value= */ max_parallel_downloads, raw_value);

      case R.string.prefkey_seek_back_ms_increment :
        return setPrefInteger(context, editor, pref_key_id, /* old_value= */ seek_back_ms_increment, raw_value);

      case R.string.prefkey_seek_forward_ms_increment :
        return setPrefInteger(context, editor, pref_key_id, /* old_value= */ seek_forward_ms_increment, raw_value);

      case R.string.prefkey_audio_volume_percent_increment :
        return setPrefFloat(context, editor, pref_key_id, /* old_value= */ audio_volume_percent_increment, raw_value);

      case R.string.prefkey_audio_volume_boost_db_increment :
        return setPrefFloat(context, editor, pref_key_id, /* old_value= */ audio_volume_boost_db_increment, raw_value);

      case R.string.prefkey_ts_extractor_timestamp_search_bytes_factor :
        return setPrefFloat(context, editor, pref_key_id, /* old_value= */ ts_extractor_timestamp_search_bytes_factor, raw_value);

      case R.string.prefkey_enable_tunneled_video_playback :
        return setPrefBoolean(context, editor, pref_key_id, /* old_value= */ enable_tunneled_video_playback, raw_value);

      case R.string.prefkey_enable_hdmv_dts_audio_streams :
        return setPrefBoolean(context, editor, pref_key_id, /* old_value= */ enable_hdmv_dts_audio_streams, raw_value);

      case R.string.prefkey_pause_on_change_to_audio_output_device :
        return setPrefBoolean(context, editor, pref_key_id, /* old_value= */ pause_on_change_to_audio_output_device, raw_value);

      case R.string.prefkey_prefer_extension_renderer :
        return setPrefBoolean(context, editor, pref_key_id, /* old_value= */ prefer_extension_renderer, raw_value);

      case R.string.prefkey_enable_audio_passthrough :
        return setPrefBoolean(context, editor, pref_key_id, /* old_value= */ enable_audio_passthrough, raw_value);

      case R.string.prefkey_enable_downmix_surround_sound_to_stereo :
        return setPrefBoolean(context, editor, pref_key_id, /* old_value= */ enable_downmix_surround_sound_to_stereo, raw_value);

      case R.string.prefkey_enable_downmix_stereo_sound_to_mono :
        return setPrefBoolean(context, editor, pref_key_id, /* old_value= */ enable_downmix_stereo_sound_to_mono, raw_value);
    }
    return false;
  }

  private static void update_internal_state(int pref_key_id) {
    Context context         = getApplicationContext();
    SharedPreferences prefs = getPrefs(context);

    update_internal_state(context, prefs, pref_key_id);
  }

  private static void update_internal_state(Context context, SharedPreferences prefs, int pref_key_id) {
    switch(pref_key_id) {
      case R.string.prefkey_http_api_password : {
        http_api_password = get_http_api_password(context, prefs);
        break;
      }

      case R.string.prefkey_default_user_agent : {
        default_user_agent = get_default_user_agent(context, prefs);
        break;
      }

      case R.string.prefkey_max_audio_volume_boost_db : {
        max_audio_volume_boost_db = get_max_audio_volume_boost_db(context, prefs);
        break;
      }

      case R.string.prefkey_max_parallel_downloads : {
        max_parallel_downloads = get_max_parallel_downloads(context, prefs);
        break;
      }

      case R.string.prefkey_seek_back_ms_increment : {
        seek_back_ms_increment = get_seek_back_ms_increment(context, prefs);
        break;
      }

      case R.string.prefkey_seek_forward_ms_increment : {
        seek_forward_ms_increment = get_seek_forward_ms_increment(context, prefs);
        break;
      }

      case R.string.prefkey_audio_volume_percent_increment : {
        audio_volume_percent_increment = get_audio_volume_percent_increment(context, prefs);
        break;
      }

      case R.string.prefkey_audio_volume_boost_db_increment : {
        audio_volume_boost_db_increment = get_audio_volume_boost_db_increment(context, prefs);
        break;
      }

      case R.string.prefkey_ts_extractor_timestamp_search_bytes_factor : {
        ts_extractor_timestamp_search_bytes_factor = get_ts_extractor_timestamp_search_bytes_factor(context, prefs);
        break;
      }

      case R.string.prefkey_enable_tunneled_video_playback : {
        enable_tunneled_video_playback = get_enable_tunneled_video_playback(context, prefs);
        break;
      }

      case R.string.prefkey_enable_hdmv_dts_audio_streams : {
        enable_hdmv_dts_audio_streams = get_enable_hdmv_dts_audio_streams(context, prefs);
        break;
      }

      case R.string.prefkey_pause_on_change_to_audio_output_device : {
        pause_on_change_to_audio_output_device = get_pause_on_change_to_audio_output_device(context, prefs);
        break;
      }

      case R.string.prefkey_prefer_extension_renderer : {
        prefer_extension_renderer = get_prefer_extension_renderer(context, prefs);
        break;
      }

      case R.string.prefkey_enable_audio_passthrough : {
        enable_audio_passthrough = get_enable_audio_passthrough(context, prefs);
        break;
      }

      case R.string.prefkey_enable_downmix_surround_sound_to_stereo : {
        enable_downmix_surround_sound_to_stereo = get_enable_downmix_surround_sound_to_stereo(context, prefs);
        break;
      }

      case R.string.prefkey_enable_downmix_stereo_sound_to_mono : {
        enable_downmix_stereo_sound_to_mono = get_enable_downmix_stereo_sound_to_mono(context, prefs);
        break;
      }
    }
  }

  // ---------------------------------------------------------------------------

  public static String serialize() {
    initialize();

    ArrayList<String> lines = new ArrayList<String>();

    lines.add("http-api-password: %s");
    lines.add("default-user-agent: %s");
    lines.add("max-audio-volume-boost-db: %d");
    lines.add("max-parallel-downloads: %d");
    lines.add("seek-back-ms-increment: %d");
    lines.add("seek-forward-ms-increment: %d");
    lines.add("audio-volume-percent-increment: %.4f");
    lines.add("audio-volume-boost-db-increment: %.4f");
    lines.add("ts-extractor-timestamp-search-bytes-factor: %.4f");
    lines.add("enable-tunneled-video-playback: %b");
    lines.add("enable-hdmv-dts-audio-streams: %b");
    lines.add("pause-on-change-to-audio-output-device: %b");
    lines.add("prefer-extension-renderer: %b");
    lines.add("enable-audio-passthrough: %b");
    lines.add("enable-downmix-surround-sound-to-stereo: %b");
    lines.add("enable-downmix-stereo-sound-to-mono: %b");

    return String.format(
      TextUtils.join("\n", lines),
      http_api_password,
      default_user_agent,
      max_audio_volume_boost_db,
      max_parallel_downloads,
      seek_back_ms_increment,
      seek_forward_ms_increment,
      audio_volume_percent_increment,
      audio_volume_boost_db_increment,
      ts_extractor_timestamp_search_bytes_factor,
      enable_tunneled_video_playback,
      enable_hdmv_dts_audio_streams,
      pause_on_change_to_audio_output_device,
      prefer_extension_renderer,
      enable_audio_passthrough,
      enable_downmix_surround_sound_to_stereo,
      enable_downmix_stereo_sound_to_mono
    );
  }

}
