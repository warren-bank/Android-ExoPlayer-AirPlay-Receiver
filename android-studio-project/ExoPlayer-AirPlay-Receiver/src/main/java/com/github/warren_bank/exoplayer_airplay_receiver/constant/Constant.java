package com.github.warren_bank.exoplayer_airplay_receiver.constant;

public class Constant {
    public static final int AIRPLAY_PORT = 8192;
    public static final int RAOP_PORT    = 5000;

    public static final String Need_sendReverse = "SendReverse";
    public static final String ReverseMsg       = "ReverseMsg";
    public static final String PlayURL          = "playUrl";
    public static final String CaptionURL       = "textUrl";
    public static final String RefererURL       = "referUrl";
    public static final String ReqHeader        = "reqHeader";
    public static final String UseCache         = "useCache";
    public static final String Start_Pos        = "startPos";
    public static final String Stop_Pos         = "stopPos";
    public static final String DRM_Scheme       = "drmScheme";
    public static final String DRM_URL          = "drmUrl";
    public static final String DRM_Header       = "drmHeader";

    public static String getServerInfoResponse(String mac) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"
            + "<plist version=\"1.0\">\r\n" + "<dict>\r\n" + "<key>deviceid</key>\r\n"
            + "<string>" + mac + "</string>\r\n" + "<key>features</key>\r\n"
            + "<integer>10623</integer>\r\n" + "<key>model</key>\r\n"
            + "<string>AppleTV2,1</string>\r\n" + "<key>protovers</key>\r\n"
            + "<string>1.0</string>\r\n" + "<key>srcvers</key>\r\n"
            + "<string>130.14</string>\r\n" + "</dict>\r\n" + "</plist>";
    }

    /**
     * event sent by the server to the client
     *
     * @param type 0 (image), 1 (video)
     * @param sessionId
     * @param state
     * @return
     */
    public static String getStopEventMsg(int type, String sessionId, String state) {
        String category = "";
        String bodyStr = "";
        if (type == 0) {
            category = "photo";
            bodyStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<plist version=\"1.0\">\n" + "<dict>\n" + "<key>category</key>\n"
                + "<string>" + category + "</string>\n" + "<key>sessionID</key>\n"
                + "<integer>1</integer>\n" + "<key>reason</key>\n"
                + "<string>ended</string>\n" + "<key>state</key>\n" + "<string>" + state
                + "</string>\n" + "</dict>\n" + "</plist>\n";
        }
        else if (type == 1) {
            category = "video";
            bodyStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<plist version=\"1.0\">\n" + "<dict>\n" + "<key>category</key>\n"
                + "<string>" + category + "</string>\n" + "<key>reason</key>\n"
                + "<string>ended</string>\n" + "<key>state</key>\n" + "<string>" + state
                + "</string>\n" + "</dict>\n" + "</plist>\n";
        }

        String sendMsg = "POST /event HTTP/1.1\r\n" + "X-Apple-Session-ID:" + sessionId
            + "\r\n" + "Content-Type: text/x-apple-plist+xml\r\n" + "Content-Length:"
            + bodyStr.length() + "\r\n\r\n" + bodyStr;

        return sendMsg;
    }

    public static String getImageStopEvent() {
        String bodyStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\">\n" + "<dict>\n" + "<key>category</key>\n"
            + "<string>photo</string>\n" + "<key>reason</key>\n"
            + "<string>ended</string>\n" + "<key>state</key>\n"
            + "<string>stopped</string>\n" + "</dict>\n" + "</plist>\n";

        return bodyStr;
    }

    public static String getVideoStopEvent() {
        String bodyStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\">\n" + "<dict>\n" + "<key>category</key>\n"
            + "<string>video</string>\n" + "<key>reason</key>\n"
            + "<string>ended</string>\n" + "<key>state</key>\n"
            + "<string>stopped</string>\n" + "</dict>\n" + "</plist>\n";

        return bodyStr;
    }

    public static String getVideoEvent(String state) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\">\n" + "<dict>\n" + "<key>category</key>\n"
            + "<string>video</string>\n" + "<key>state</key>\n" + "<string>" + state
            + "</string>\n" + "</dict>\n" + "</plist>\n";
    }

    public static String getVideoEventMsg(String sessionId, String state) {
        String category = "video";
        String bodyStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\">\n" + "<dict>\n" + "<key>category</key>\n"
            + "<string>" + category + "</string>\n" + "<key>state</key>\n";

        String sendMsg = "POST /event HTTP/1.1\r\n" + "X-Apple-Session-ID:" + sessionId
            + "\r\n" + "Content-Type: text/x-apple-plist+xml\r\n" + "Content-Length:"
            + bodyStr.length() + "\r\n\r\n" + bodyStr;

        return sendMsg;
    }

    public static String getPlaybackInfo(float duration, float cacheDuration,
            float curPos, int playing) {
        String info = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
            + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\">\n" + "<dict>\n" + "<key>duration</key>\n"
            + "<real>"
            + duration
            + "</real>\n"
            + "<key>loadedTimeRanges</key>\n"
            + "<array>\n"
            + "  <dict>\n"
            + "  <key>duration</key>\n"
            + "  <real>"
            + cacheDuration
            + "</real>\n"
            + "  <key>start</key>\n"
            + "  <real>0.0</real>\n"
            + "  </dict>\n"
            + "</array>\n"
            + "<key>playbackBufferEmpty</key>\n"
            + "<true/>\n"
            + "<key>playbackBufferFull</key>\n"
            + "<false/>\n"
            + "<key>playbackLikelyToKeepUp</key>\n"
            + "<true/>\n"
            + "<key>position</key>\n"
            + "<real>"
            + curPos
            + "</real>\n"
            + "<key>rate</key>\n"
            + "<real>"
            + playing
            + "</real>\n"
            + "<key>readyToPlay</key>"
            + "<true/>\n"
            + "<key>seekableTimeRanges</key>\n"
            + "<array>\n"
            + "  <dict>\n"
            + "  <key>duration</key>\n"
            + "  <real>"
            + duration
            + "</real>\n"
            + "  <key>start</key>\n"
            + "  <real>0.0</real>\n"
            + "  </dict>\n" + "</array>\n" + "</dict>\n" + "</plist>\n";

        return info;

    }

    public interface Register {
        public static final int FAIL                            = -1;
        public static final int OK                              =  0;
    }

    public interface Msg {
        public static final int Msg_Photo                       =  1;
        public static final int Msg_Stop                        =  2;
        public static final int Msg_Video_Play                  =  3;
        public static final int Msg_Video_Seek                  =  4;
        public static final int Msg_Video_Rate                  =  5;

        public static final int Msg_Video_Seek_Offset           =  6;
        public static final int Msg_Video_Queue                 =  7;
        public static final int Msg_Video_Next                  =  8;
        public static final int Msg_Video_Prev                  =  9;
        public static final int Msg_Audio_Volume                = 10;
        public static final int Msg_Text_Load                   = 11;
        public static final int Msg_Text_Show                   = 12;
        public static final int Msg_Text_Set_Time               = 13;
        public static final int Msg_Text_Add_Time               = 14;

        public static final int Msg_Show_Toast                  = 15;
        public static final int Msg_Show_Player                 = 16;
        public static final int Msg_Hide_Player                 = 17;
        public static final int Msg_Start_Activity              = 18;
        public static final int Msg_Video_Share                 = 19;
        public static final int Msg_Preferences_Edit            = 20;
        public static final int Msg_Delete_Cache                = 21;
        public static final int Msg_Exit_Service                = 22;

        public static final int Msg_Runtime_Permissions_Granted = 23;
    }

    public interface Target {
        public static final String REVERSE                      = "/reverse";
        public static final String PHOTO                        = "/photo";
        public static final String SERVER_INFO                  = "/server-info";
        public static final String STOP                         = "/stop";
        public static final String PLAY                         = "/play";
        public static final String SCRUB                        = "/scrub";
        public static final String RATE                         = "/rate";
        public static final String PLAYBACK_INFO                = "/playback-info";

        public static final String MEDIA_ITEM_INFO              = "/media-item-info";
        public static final String SCRUB_OFFSET                 = "/add-scrub-offset";
        public static final String QUEUE                        = "/queue";
        public static final String NEXT                         = "/next";
        public static final String PREVIOUS                     = "/previous";
        public static final String VOLUME                       = "/volume";
        public static final String TXT_LOAD                     = "/load-captions";
        public static final String TXT_SHOW                     = "/show-captions";
        public static final String TXT_SET_OFFSET               = "/set-captions-offset";
        public static final String TXT_ADD_OFFSET               = "/add-captions-offset";

        public static final String TOAST_SHOW                   = "/show-toast";
        public static final String PLAYER_SHOW                  = "/show-player";
        public static final String PLAYER_HIDE                  = "/hide-player";
        public static final String ACTIVITY_START               = "/start-activity";
        public static final String VIDEO_SHARE                  = "/share-video";
        public static final String PREFERENCES_EDIT             = "/edit-preferences";
        public static final String CACHE_DELETE                 = "/delete-cache";
        public static final String SERVICE_EXIT                 = "/exit-service";
    }

    public interface Status {
        public static final String Status_play                  = "playing";
        public static final String Status_stop                  = "stopped";
        public static final String Status_pause                 = "paused";
        public static final String Status_load                  = "loading";
    }

    public interface Video_Source_Map {
        public static final String DATA                         = "data";
        public static final String REQ_HEADERS                  = "req-header";
        public static final String DRM_HEADERS                  = "drm-header";
        public static final String PLAYLIST_URLS                = "playlist-urls";
    }

    public interface Delimiter {
        public static final String DEFAULT                      = "\n";
        public static final String PLAYLIST_URLS                = "{{<<|^|>>}}";
    }

    public interface MediaItemInfo {
        public static final String IS_PLAYER_READY              = "is_player_ready";
        public static final String IS_PLAYER_PAUSED             = "is_player_paused";
        public static final String MEDIA_URL                    = "media_url";
        public static final String MEDIA_TYPE                   = "media_type";
        public static final String CAPTION_URL                  = "caption_url";
        public static final String REFERER_URL                  = "referer_url";
        public static final String REQUEST_HEADERS              = "request_headers";
        public static final String USE_OFFLINE_CACHE            = "use_offline_cache";
        public static final String START_POSITION               = "start_position";
        public static final String STOP_POSITION                = "stop_position";
        public static final String CURRENT_POSITION             = "current_position";
        public static final String DURATION                     = "duration";
        public static final String DRM_SCHEME                   = "drm_scheme";
        public static final String DRM_LICENSE_SERVER_URL       = "drm_license_server_url";
        public static final String DRM_REQUEST_HEADERS          = "drm_request_headers";
    }

}
