package com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api;

/*
 * based on:
 *   https://github.com/eeeeeric/mpc-hc-api/blob/0.1.0/src/main/java/com/eeeeeric/mpc/hc/api/WMCommand.java
 */

/**
 * These are commands that MPC-HC recognizes.
 */
public enum WMCommand {
  SET_VOLUME("Set Volume", -2),
  SEEK("Seek", -1),
  QUICK_OPEN_FILE("Quick Open File", 969),
  OPEN_FILE("Open File", 800),
  OPEN_DVD_BD("Open DVD/BD", 801),
  OPEN_DEVICE("Open Device", 802),
  REOPEN_FILE("Reopen File", 976),
  MOVE_TO_RECYCLE_BIN("Move to Recycle Bin", 24044),
  SAVE_A_COPY("Save a Copy", 805),
  SAVE_IMAGE("Save Image", 806),
  SAVE_IMAGE_AUTO("Save Image (auto)", 807),
  SAVE_THUMBNAILS("Save thumbnails", 808),
  LOAD_SUBTITLE("Load Subtitle", 809),
  SAVE_SUBTITLE("Save Subtitle", 810),
  CLOSE("Close", 804),
  PROPERTIES("Properties", 814),
  EXIT("Exit", 816),
  PLAY_PAUSE("Play/Pause", 889),
  PLAY("Play", 887),
  PAUSE("Pause", 888),
  STOP("Stop", 890),
  FRAMESTEP("Framestep", 891),
  FRAMESTEP_BACK("Framestep back", 892),
  GO_TO("Go To", 893),
  INCREASE_RATE("Increase Rate", 895),
  DECREASE_RATE("Decrease Rate", 894),
  RESET_RATE("Reset Rate", 896),
  AUDIO_DELAY_PLUS_10_MS("Audio Delay +10 ms", 905),
  AUDIO_DELAY_MINUS_10_MS("Audio Delay -10 ms", 906),
  JUMP_FORWARD_SMALL("Jump Forward (small)", 900),
  JUMP_BACKWARD_SMALL("Jump Backward (small)", 899),
  JUMP_FORWARD_MEDIUM("Jump Forward (medium)", 902),
  JUMP_BACKWARD_MEDIUM("Jump Backward (medium)", 901),
  JUMP_FORWARD_LARGE("Jump Forward (large)", 904),
  JUMP_BACKWARD_LARGE("Jump Backward (large)", 903),
  JUMP_FORWARD_KEYFRAME("Jump Forward (keyframe)", 898),
  JUMP_BACKWARD_KEYFRAME("Jump Backward (keyframe)", 897),
  JUMP_TO_BEGINNING("Jump to Beginning", 996),
  NEXT("Next", 922),
  PREVIOUS("Previous", 921),
  NEXT_FILE("Next File", 920),
  PREVIOUS_FILE("Previous File", 919),
  TUNER_SCAN("Tuner scan", 974),
  QUICK_ADD_FAVORITE("Quick add favorite", 975),
  TOGGLE_CAPTION_AND_MENU("Toggle Caption&Menu", 817),
  TOGGLE_SEEKER("Toggle Seeker", 818),
  TOGGLE_CONTROLS("Toggle Controls", 819),
  TOGGLE_INFORMATION("Toggle Information", 820),
  TOGGLE_STATISTICS("Toggle Statistics", 821),
  TOGGLE_STATUS("Toggle Status", 822),
  TOGGLE_SUBRESYNC_BAR("Toggle Subresync Bar", 823),
  TOGGLE_PLAYLIST_BAR("Toggle Playlist Bar", 824),
  TOGGLE_CAPTURE_BAR("Toggle Capture Bar", 825),
  TOGGLE_NAVIGATION_BAR("Toggle Navigation Bar", 33415),
  TOGGLE_DEBUG_SHADERS("Toggle Debug Shaders", 826),
  VIEW_MINIMAL("View Minimal", 827),
  VIEW_COMPACT("View Compact", 828),
  VIEW_NORMAL("View Normal", 829),
  FULLSCREEN("Fullscreen", 830),
  FULLSCREEN_WITHOUT_RES_CHANGE("Fullscreen (w/o res.change)", 831),
  ZOOM_50("Zoom 50%", 832),
  ZOOM_100("Zoom 100%", 833),
  ZOOM_200("Zoom 200%", 834),
  ZOOM_AUTO_FIT("Zoom Auto Fit", 968),
  ZOOM_AUTO_FIT_LARGER_ONLY("Zoom Auto Fit (Larger Only)", 4900),
  NEXT_AR_PRESET("Next AR Preset", 859),
  VIDFRM_HALF("VidFrm Half", 835),
  VIDFRM_NORMAL("VidFrm Normal", 836),
  VIDFRM_DOUBLE("VidFrm Double", 837),
  VIDFRM_STRETCH("VidFrm Stretch", 838),
  VIDFRM_INSIDE("VidFrm Inside", 839),
  VIDFRM_ZOOM_1("VidFrm Zoom 1", 841),
  VIDFRM_ZOOM_2("VidFrm Zoom 2", 842),
  VIDFRM_OUTSIDE("VidFrm Outside", 840),
  VIDFRM_SWITCH_ZOOM("VidFrm Switch Zoom", 843),
  ALWAYS_ON_TOP("Always On Top", 884),
  PNS_RESET("PnS Reset", 861),
  PNS_INC_SIZE("PnS Inc Size", 862),
  PNS_INC_WIDTH("PnS Inc Width", 864),
  PNS_INC_HEIGHT("PnS Inc Height", 866),
  PNS_DEC_SIZE("PnS Dec Size", 863),
  PNS_DEC_WIDTH("PnS Dec Width", 865),
  PNS_DEC_HEIGHT("PnS Dec Height", 867),
  PNS_CENTER("PnS Center", 876),
  PNS_LEFT("PnS Left", 868),
  PNS_RIGHT("PnS Right", 869),
  PNS_UP("PnS Up", 870),
  PNS_DOWN("PnS Down", 871),
  PNS_UP_LEFT("PnS Up/Left", 872),
  PNS_UP_RIGHT("PnS Up/Right", 873),
  PNS_DOWN_LEFT("PnS Down/Left", 874),
  PNS_DOWN_RIGHT("PnS Down/Right", 875),
  PNS_ROTATE_X_PLUS("PnS Rotate X+", 877),
  PNS_ROTATE_X_MINUS("PnS Rotate X-", 878),
  PNS_ROTATE_Y_PLUS("PnS Rotate Y+", 879),
  PNS_ROTATE_Y_MINUS("PnS Rotate Y-", 880),
  PNS_ROTATE_Z_PLUS("PnS Rotate Z+", 881),
  PNS_ROTATE_Z_MINUS("PnS Rotate Z-", 882),
  VOLUME_UP("Volume Up", 907),
  VOLUME_DOWN("Volume Down", 908),
  VOLUME_MUTE("Volume Mute", 909),
  VOLUME_BOOST_INCREASE("Volume boost increase", 970),
  VOLUME_BOOST_DECREASE("Volume boost decrease", 971),
  VOLUME_BOOST_MIN("Volume boost Min", 972),
  VOLUME_BOOST_MAX("Volume boost Max", 973),
  TOGGLE_CUSTOM_CHANNEL_MAPPING("Toggle custom channel mapping", 993),
  TOGGLE_NORMALIZATION("Toggle normalization", 994),
  TOGGLE_REGAIN_VOLUME("Toggle regain volume", 995),
  BRIGHTNESS_INCREASE("Brightness increase", 984),
  BRIGHTNESS_DECREASE("Brightness decrease", 985),
  CONTRAST_INCREASE("Contrast increase", 986),
  CONTRAST_DECREASE("Contrast decrease", 987),
  HUE_INCREASE("Hue increase", 988),
  HUE_DECREASE("Hue decrease", 989),
  SATURATION_INCREASE("Saturation increase", 990),
  SATURATION_DECREASE("Saturation decrease", 991),
  RESET_COLOR_SETTINGS("Reset color settings", 992),
  DVD_TITLE_MENU("DVD Title Menu", 923),
  DVD_ROOT_MENU("DVD Root Menu", 924),
  DVD_SUBTITLE_MENU("DVD Subtitle Menu", 925),
  DVD_AUDIO_MENU("DVD Audio Menu", 926),
  DVD_ANGLE_MENU("DVD Angle Menu", 927),
  DVD_CHAPTER_MENU("DVD Chapter Menu", 928),
  DVD_MENU_LEFT("DVD Menu Left", 929),
  DVD_MENU_RIGHT("DVD Menu Right", 930),
  DVD_MENU_UP("DVD Menu Up", 931),
  DVD_MENU_DOWN("DVD Menu Down", 932),
  DVD_MENU_ACTIVATE("DVD Menu Activate", 933),
  DVD_MENU_BACK("DVD Menu Back", 934),
  DVD_MENU_LEAVE("DVD Menu Leave", 935),
  BOSS_KEY("Boss key", 944),
  PLAYER_MENU_SHORT("Player Menu (short)", 949),
  PLAYER_MENU_LONG("Player Menu (long)", 950),
  FILTERS_MENU("Filters Menu", 951),
  OPTIONS("Options", 815),
  NEXT_AUDIO("Next Audio", 952),
  PREV_AUDIO("Prev Audio", 953),
  NEXT_SUBTITLE("Next Subtitle", 954),
  PREV_SUBTITLE("Prev Subtitle", 955),
  ON_OFF_SUBTITLE("On/Off Subtitle", 956),
  RELOAD_SUBTITLES("Reload Subtitles", 2302),
  DOWNLOAD_SUBTITLES("Download subtitles", 812),
  NEXT_AUDIO_OGM("Next Audio (OGM)", 957),
  PREV_AUDIO_OGM("Prev Audio (OGM)", 958),
  NEXT_SUBTITLE_OGM("Next Subtitle (OGM)", 959),
  PREV_SUBTITLE_OGM("Prev Subtitle (OGM)", 960),
  NEXT_ANGLE_DVD("Next Angle (DVD)", 961),
  PREV_ANGLE_DVD("Prev Angle (DVD)", 962),
  NEXT_AUDIO_DVD("Next Audio (DVD)", 963),
  PREV_AUDIO_DVD("Prev Audio (DVD)", 964),
  NEXT_SUBTITLE_DVD("Next Subtitle (DVD)", 965),
  PREV_SUBTITLE_DVD("Prev Subtitle (DVD)", 966),
  ON_OFF_SUBTITLE_DVD("On/Off Subtitle (DVD)", 967),
  TEARING_TEST("Tearing Test", 32769),
  REMAINING_TIME("Remaining Time", 32778),
  NEXT_SHADER_PRESET("Next Shader Preset", 57382),
  PREV_SHADER_PRESET("Prev Shader Preset", 57384),
  TOGGLE_DIRECT3D_FULLSCREEN("Toggle Direct3D fullscreen", 32779),
  GOTO_PREV_SUBTITLE("Goto Prev Subtitle", 32780),
  GOTO_NEXT_SUBTITLE("Goto Next Subtitle", 32781),
  SHIFT_SUBTITLE_LEFT("Shift Subtitle Left", 32782),
  SHIFT_SUBTITLE_RIGHT("Shift Subtitle Right", 32783),
  DISPLAY_STATS("Display Stats", 32784),
  RESET_DISPLAY_STATS("Reset Display Stats", 33405),
  VSYNC("VSync", 33243),
  ENABLE_FRAME_TIME_CORRECTION("Enable Frame Time Correction", 33265),
  ACCURATE_VSYNC("Accurate VSync", 33260),
  DECREASE_VSYNC_OFFSET("Decrease VSync Offset", 33246),
  INCREASE_VSYNC_OFFSET("Increase VSync Offset", 33247),
  SUBTITLE_DELAY_MINUS("Subtitle Delay -", 24000),
  SUBTITLE_DELAY_PLUS("Subtitle Delay +", 24001),
  AFTER_PLAYBACK_EXIT("After Playback: Exit", 912),
  AFTER_PLAYBACK_STAND_BY("After Playback: Stand By", 913),
  AFTER_PLAYBACK_HIBERNATE("After Playback: Hibernate", 914),
  AFTER_PLAYBACK_SHUTDOWN("After Playback: Shutdown", 915),
  AFTER_PLAYBACK_LOG_OFF("After Playback: Log Off", 916),
  AFTER_PLAYBACK_LOCK("After Playback: Lock", 917),
  AFTER_PLAYBACK_TURN_OFF_THE_MONITOR("After Playback: Turn off the monitor", 918),
  AFTER_PLAYBACK_PLAY_NEXT_FILE_IN_THE_FOLDER("After Playback: Play next file in the folder", 947),
  TOGGLE_EDL_WINDOW("Toggle EDL window", 846),
  EDL_SET_IN("EDL set In", 847),
  EDL_SET_OUT("EDL set Out", 848),
  EDL_NEW_CLIP("EDL new clip", 849),
  EDL_SAVE("EDL save", 860);

  private String commandName;
  private int value;

  /**
   * Create a new instance.
   *
   * @param commandName
   *        A human friendly string of what the command does
   * @param value
   *        The integer command code
   */
  WMCommand(String commandName, int value) {
    this.commandName = commandName;
    this.value = value;
  }

  /**
   * Returns a human friendly string of what the command does
   *
   * @return a human friendly string of what the command does
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Returns the integer command code.
   *
   * @return the integer command code.
   */
  public int getValue() {
    return value;
  }

  /**
   * Returns the instance having a specific command code, or null if none exists.
   *
   * @return the instance having a specific command code, or null if none exists.
   */
  public static WMCommand findValue(String value) {
    try {
      return findValue(Integer.parseInt(value, 10));
    }
    catch(Exception e) {
      return null;
    }
  }

  /**
   * Returns the instance having a specific command code, or null if none exists.
   *
   * @return the instance having a specific command code, or null if none exists.
   */
  public static WMCommand findValue(int value) {
    for (WMCommand instance : values()) {
      if (instance.getValue() == value) {
        return instance;
      }
    }
    return null;
  }
}
