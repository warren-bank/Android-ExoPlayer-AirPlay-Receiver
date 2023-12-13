package com.github.warren_bank.exoplayer_airplay_receiver.service;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.PlayerManager;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.VideoSource;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.ContentProviderM3uPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.DirectoryIndexMediaPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.DirectoryIndexRecursiveMediaPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.FileM3uPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.HttpHtmlPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.HttpM3uPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.SerializedPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.ImageViewerActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.RuntimePermissionsRequestActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.VideoPlayerActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.IntentUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.PreferencesMgr;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ToastUtils;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

final class MyMessageHandler extends Handler {
  private static final String tag = NetworkingService.class.getSimpleName();

  private Looper                                        mainLooper;
  private WeakReference<NetworkingService>              weakReference;

  private ArrayList<Message>                            externalStorageMessages;

  private HandlerThread                                 networkingHandlerThread;
  private SerializedPlaylistExtractor                   serializedExtractor;
  private HttpM3uPlaylistExtractor                      httpM3uExtractor;
  private HttpHtmlPlaylistExtractor                     httpHtmlExtractor;
  private ContentProviderM3uPlaylistExtractor           contentM3uExtractor;
  private FileM3uPlaylistExtractor                      fileM3uExtractor;
  private DirectoryIndexMediaPlaylistExtractor          directoryExtractor;
  private DirectoryIndexRecursiveMediaPlaylistExtractor recursiveDirectoryExtractor;

  public MyMessageHandler(Looper looper, NetworkingService service) {
    super(looper);

    mainLooper    = looper;
    weakReference = new WeakReference<NetworkingService>(service);

    externalStorageMessages = new ArrayList<Message>();

    networkingHandlerThread = new HandlerThread("MyNetworkingThread");
    networkingHandlerThread.start();

    serializedExtractor         = new SerializedPlaylistExtractor();
    httpM3uExtractor            = new HttpM3uPlaylistExtractor();
    httpHtmlExtractor           = new HttpHtmlPlaylistExtractor();
    contentM3uExtractor         = new ContentProviderM3uPlaylistExtractor(service.getApplicationContext());
    fileM3uExtractor            = new FileM3uPlaylistExtractor();
    directoryExtractor          = new DirectoryIndexMediaPlaylistExtractor();
    recursiveDirectoryExtractor = new DirectoryIndexRecursiveMediaPlaylistExtractor();
  }

  @Override
  public void handleMessage(final Message msg) {
    super.handleMessage(msg);

    final PlayerManager playerManager = NetworkingService.getPlayerManager();
    if (playerManager == null)
      return;

    final NetworkingService service = weakReference.get();
    if (service == null)
      return;

    switch (msg.what) {

      // =======================================================================
      // Bonjour service registration:
      // =======================================================================

      case Constant.Register.OK : {
        ToastUtils.showToastCenterShort(service.getApplicationContext(), R.string.toast_registration_success);
        break;
      }

      case Constant.Register.FAIL : {
        ToastUtils.showToastCenterShort(service.getApplicationContext(), R.string.toast_registration_failure);
        break;
      }

      // =======================================================================
      // Display raw image data
      // =======================================================================

      case Constant.Msg.Msg_Photo : {
        startImageViewerActivity(
          service,
          /* pic= */ (byte[]) msg.obj
        );
        break;
      }

      // =======================================================================
      // Display Toast containing a text message
      // =======================================================================

      case Constant.Msg.Msg_Show_Toast : {
        String text;
        text = (String) msg.obj;
        text = ToastUtils.interpolate_variables(service.getApplicationContext(), playerManager, text);

        if (!TextUtils.isEmpty(text)) {
          ToastUtils.showToastBottomLong(service.getApplicationContext(), text);
        }
        break;
      }

      // =======================================================================
      // Start new Activity
      // =======================================================================

      case Constant.Msg.Msg_Start_Activity : {
        startActivity(
          service,
          /* map= */ (HashMap) msg.obj
        );
        break;
      }

      // =======================================================================
      // Share current video in queue with new Activity
      // =======================================================================

      case Constant.Msg.Msg_Video_Share : {
        shareVideo(
          service,
          playerManager.getCurrentItem(),
          /* map= */ (HashMap) msg.obj
        );
        break;
      }

      // =======================================================================
      // Edit Preferences
      // =======================================================================

      case Constant.Msg.Msg_Preferences_Edit : {
        PreferencesMgr.edit_preferences(
          /* values= */ (HashMap) msg.obj
        );
        break;
      }

      // =======================================================================
      // Delete Cache
      // =======================================================================

      case Constant.Msg.Msg_Delete_Cache : {
        playerManager.AirPlay_delete_cache();
        break;
      }

      // =======================================================================
      // Exit Service
      // =======================================================================

      case Constant.Msg.Msg_Exit_Service : {
        service.stopSelf();
        break;
      }

      // =======================================================================
      // Add media URLs to ExoPlayer queue.
      // Display a video player when starting playback of a video URL,
      //   or explicitly requested to do so.
      // =======================================================================

      case Constant.Msg.Msg_Video_Play  :
      case Constant.Msg.Msg_Video_Queue : {
        final int                                      msgWhat = (int)     msg.what;
        final HashMap<String, HashMap<String, String>> msgMap  = (HashMap) msg.obj;

        final HashMap<String, String> dataMap         = (HashMap) msgMap.get(Constant.Video_Source_Map.DATA);
        final HashMap<String, String> reqHeadersMap   = (HashMap) msgMap.get(Constant.Video_Source_Map.REQ_HEADERS);
        final HashMap<String, String> drmHeadersMap   = (HashMap) msgMap.get(Constant.Video_Source_Map.DRM_HEADERS);
        final HashMap<String, String> playlistUrlsMap = (HashMap) msgMap.get(Constant.Video_Source_Map.PLAYLIST_URLS);

        // offload to a worker Thread
        final Handler  handler  = new Handler(networkingHandlerThread.getLooper());
        final Runnable runnable = new Runnable() {
          @Override
          public void run() {
            Message newMsg = Message.obtain();
            newMsg.what = msgWhat;
            newMsg.obj  = msgMap;

            String playUrl   = dataMap.get(Constant.PlayURL);
            String textUrl   = dataMap.get(Constant.CaptionURL);
            String referUrl  = dataMap.get(Constant.RefererURL);
            String useCache  = dataMap.get(Constant.UseCache);
            String startPos  = dataMap.get(Constant.Start_Pos);
            String stopPos   = dataMap.get(Constant.Stop_Pos);
            String drmScheme = dataMap.get(Constant.DRM_Scheme);
            String drmUrl    = dataMap.get(Constant.DRM_URL);

            // normalize empty data fields to: null
            if (TextUtils.isEmpty(playUrl))
              playUrl = null;
            if (TextUtils.isEmpty(textUrl))
              textUrl = null;
            if (TextUtils.isEmpty(referUrl))
              referUrl = null;
            if (TextUtils.isEmpty(startPos))
              startPos = "-1";
            if (TextUtils.isEmpty(stopPos))
              stopPos = "-1";
            if (TextUtils.isEmpty(drmScheme))
              drmScheme = null;
            if (TextUtils.isEmpty(drmUrl))
              drmUrl = null;

            // normalize boolean data fields to: ["true", "false"]
            useCache = StringUtils.normalizeBooleanString(useCache);

            // ignore bad requests
            if (playUrl == null)
              return;

            Log.d(tag, ((msgWhat == Constant.Msg.Msg_Video_Play) ? "play" : "queue") + " media: url = " + playUrl + "; start at = " + startPos + "; stop at = " + stopPos + "; captions = " + textUrl + "; referer = " + referUrl + "; drm scheme = " + drmScheme + "; drm license url = " + drmUrl);

            if (requiresExternalStoragePermission(service, newMsg, playUrl, textUrl))
              return;

            // normalize references to external storage by converting absolute filesystem paths to file: URIs
            if (ExternalStorageUtils.isFileUri(playUrl))
              playUrl = ExternalStorageUtils.normalizeFileUri(playUrl);
            if (ExternalStorageUtils.isFileUri(textUrl))
              textUrl = ExternalStorageUtils.normalizeFileUri(textUrl);

            ArrayList<String> matches = (playlistUrlsMap != null)
              ? StringUtils.convertHashMapToArrayList(playlistUrlsMap)
              : extractPlaylists(playUrl, null);

            // check runtime permissions
            if ((playlistUrlsMap == null) && (matches != null)) {
              boolean requiresPermission = false;
              String match;
              for (int i=0; i < matches.size(); i++) {
                match = matches.get(i);
                if (ExternalStorageUtils.isFileUri(match)) {
                  requiresPermission = true;
                  match = ExternalStorageUtils.normalizeFileUri(match);
                  matches.set(i, match);
                }
              }
              if (requiresPermission && ExternalStorageUtils.has_permission(service))
                requiresPermission = false;
              if (requiresPermission) {
                msgMap.put(
                  Constant.Video_Source_Map.PLAYLIST_URLS,
                  StringUtils.convertArrayListToHashMap(matches)
                );

                requiresExternalStoragePermission(service, newMsg, "/", "/");
                return;
              }
            }

            addItems(
              playerManager,
              service,
              matches,
              /* uri=                   */ playUrl,
              /* caption=               */ textUrl,
              /* referer=               */ referUrl,
              /* reqHeadersMap=         */ reqHeadersMap,
              /* useCache=              */ "true".equals(useCache),
              /* startPosition=         */ Float.valueOf(startPos),
              /* stopPosition=          */ Float.valueOf(stopPos),
              /* drm_scheme=            */ drmScheme,
              /* drm_license_server=    */ drmUrl,
              /* drmHeadersMap=         */ drmHeadersMap,
              /* remove_previous_items= */ (msgWhat == Constant.Msg.Msg_Video_Play)
            );
          }
        };

        handler.post(runnable);
        break;
      }

      case Constant.Msg.Msg_Show_Player : {
        boolean enterPipMode = ((msg.obj != null) && (msg.obj instanceof Boolean))
          ? ((Boolean) msg.obj).booleanValue()
          : false;
        startVideoPlayerActivity(service, enterPipMode);
        break;
      }

      // =======================================================================
      // Update caption URL to current video in ExoPlayer queue.
      // =======================================================================

      case Constant.Msg.Msg_Text_Load : {
        String textUrl = (String) msg.obj;
        playerManager.loadCaptions(textUrl);
        break;
      }

      // =======================================================================
      // ExoPlayer playback controls:
      // =======================================================================

      case Constant.Msg.Msg_Video_Seek : {
        float positionSec = (float) msg.obj;
        playerManager.AirPlay_scrub(positionSec);
        break;
      }

      case Constant.Msg.Msg_Video_Seek_Offset : {
        long add_offset = (long) msg.obj;
        playerManager.AirPlay_add_scrub_offset(add_offset);
        break;
      }

      case Constant.Msg.Msg_Video_Rate : {
        float rate = (float) msg.obj;
        playerManager.AirPlay_rate(rate);
        break;
      }

      case Constant.Msg.Msg_Stop : {
        playerManager.AirPlay_stop();
        break;
      }

      case Constant.Msg.Msg_Video_Next : {
        playerManager.AirPlay_next();
        break;
      }

      case Constant.Msg.Msg_Video_Prev : {
        playerManager.AirPlay_previous();
        break;
      }

      case Constant.Msg.Msg_Audio_Volume : {
        float audioVolume = (float) msg.obj;
        playerManager.AirPlay_volume(audioVolume);
        break;
      }

      case Constant.Msg.Msg_Text_Show : {
        boolean showCaptions = (boolean) msg.obj;
        playerManager.AirPlay_show_captions(showCaptions);
        break;
      }

      case Constant.Msg.Msg_Text_Set_Style : {
        final HashMap<String, Object> msgMap = (HashMap) msg.obj;
        Integer fontSize      = (Integer) msgMap.get(Constant.FontSize);
        Boolean applyEmbedded = (Boolean) msgMap.get(Constant.ApplyEmbedded);

        playerManager.AirPlay_set_captions_style(fontSize, applyEmbedded);
        break;
      }

      case Constant.Msg.Msg_Text_Set_Time : {
        long set_offset = (long) msg.obj;
        playerManager.AirPlay_set_captions_offset(set_offset);
        break;
      }

      case Constant.Msg.Msg_Text_Add_Time : {
        long add_offset = (long) msg.obj;
        playerManager.AirPlay_add_captions_offset(add_offset);
        break;
      }

      case Constant.Msg.Msg_Repeat_Mode : {
        int repeatMode = (int) msg.obj;
        playerManager.AirPlay_repeat_mode(repeatMode);
        break;
      }

      case Constant.Msg.Msg_Resize_Mode : {
        int resizeMode = (int) msg.obj;
        playerManager.AirPlay_resize_mode(resizeMode);
        break;
      }

      // =======================================================================
      // Runtime Permissions:
      // =======================================================================

      case Constant.Msg.Msg_Runtime_Permissions_Granted : {
        int requestCode = (int) msg.obj;
        switch(requestCode) {
          case Constant.PermissionRequestCode.READ_EXTERNAL_STORAGE : {
            handleExternalStorageMessages();
            break;
          }
        }
        break;
      }

    }
  }

  private void startActivity(NetworkingService service, HashMap<String, ArrayList<String>> map) {
    Log.d(tag, "starting Activity from HashMap");
    Intent intent = new Intent();
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    // explicit
    if (map.containsKey("package") && map.containsKey("class")) {
      intent.setClassName(
        (String) StringUtils.getLastListItem(map.get("package")),
        (String) StringUtils.getLastListItem(map.get("class"))
      );
    }

    // implicit
    if (map.containsKey("action")) {
      intent.setAction(
        (String) StringUtils.getLastListItem(map.get("action"))
      );
    }

    if (map.containsKey("data")) {
      Uri data = Uri.parse(
        (String) StringUtils.getLastListItem(map.get("data"))
      );

      if (map.containsKey("type")) {
        intent.setDataAndType(
          data,
          (String) StringUtils.getLastListItem(map.get("type"))
        );
      }
      else {
        intent.setData(data);
      }
    }
    else if (map.containsKey("type")) {
      intent.setType(
        (String) StringUtils.getLastListItem(map.get("type"))
      );
    }

    if (map.containsKey("category")) {
      for (String category : map.get("category")) {
        intent.addCategory(category);
      }
    }

    if (map.containsKey("flag")) {
      for (String flag : map.get("flag")) {
        try {
          if (flag.startsWith("0x"))
            intent.addFlags(Integer.parseInt(flag.substring(2), 16));
          else
            intent.addFlags(Integer.parseInt(flag, 10));
        }
        catch (Exception e) {}
      }
    }

    for (String key : map.keySet()) {
      if (key.startsWith("extra-")) {
        String name = key.substring(6);
        ArrayList<String> values = map.get(key);

        if (values.size() > 1)
          intent.putExtra(name, (String[]) values.toArray(new String[values.size()]));
        else
          IntentUtils.putExtra(intent, name, (String) values.get(0));
      }
    }

    if (map.containsKey("chooser-title")) {
      String title = (String) StringUtils.getLastListItem(map.get("chooser-title"));
      intent = Intent.createChooser(intent, title);
    }

    try {
      service.startActivity(intent);
    }
    catch(ActivityNotFoundException e) {
      ToastUtils.showToastBottomShort(service.getApplicationContext(), R.string.toast_start_activity_error_activity_not_found);
    }
    catch(SecurityException e) {
      ToastUtils.showToastBottomShort(service.getApplicationContext(), R.string.toast_start_activity_error_security);
    }
    catch(Exception e) {
      Log.e(tag, "startActivity", e);
    }
  }

  private void shareVideo(NetworkingService service, VideoSource sample, HashMap<String, String> map) {
    Log.d(tag, "starting Activity from current video in queue");

    if ((sample == null) || TextUtils.isEmpty(sample.uri))
      return;

    Uri data = Uri.parse(sample.uri);

    Intent intent = new Intent();
    intent.setAction("android.intent.action.VIEW");
    intent.addCategory("android.intent.category.DEFAULT");
    intent.addCategory("android.intent.category.BROWSABLE");
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    if (TextUtils.isEmpty(sample.uri_mimeType)) {
      intent.setData(data);
    }
    else {
      intent.setDataAndType(data, sample.uri_mimeType.toLowerCase());
    }

    String[]  string_names = new String[] {"referUrl", "textUrl", "drmScheme", "drmUrl"};
    String[] hashmap_names = new String[] {"reqHeader", "drmHeader"};
    String lc_name, alias_name;
    String string_value;
    HashMap<String, String> hashmap_value;
    String[] array_value;
    Bundle bundle_value;

    for (String name : string_names) {
      lc_name      = name.toLowerCase();
      alias_name   = (String) map.get(lc_name);
      string_value = null;

      switch(lc_name) {
        case "referurl" : {
          string_value = sample.referer;
          break;
        }

        case "texturl" : {
          string_value = sample.caption;
          break;
        }

        case "drmscheme" : {
          string_value = sample.drm_scheme;
          break;
        }

        case "drmurl" : {
          string_value = sample.drm_license_server;
          break;
        }
      }

      if (!TextUtils.isEmpty(string_value)) {
        // always include a String extra that can be read by ExoAirPlayer
        intent.putExtra(name, (String) string_value);

        // conditionally include a String extra, when the request provides a name
        if (!TextUtils.isEmpty(alias_name)) {
          intent.putExtra(alias_name, (String) string_value);
        }
      }
    }

    for (String name : hashmap_names) {
      lc_name       = name.toLowerCase();
      alias_name    = (String) map.get(lc_name);
      hashmap_value = null;

      switch(lc_name) {
        case "reqheader" : {
          hashmap_value = sample.reqHeadersMap;
          break;
        }

        case "drmheader" : {
          hashmap_value = sample.drmHeadersMap;
          break;
        }
      }

      if ((hashmap_value != null) && !hashmap_value.isEmpty()) {
        // always include a String[] extra that can be read by ExoAirPlayer
        array_value = StringUtils.toStringArray(hashmap_value);
        intent.putExtra(name, (String[]) array_value);

        // conditionally include a Bundle extra w/ String based key-value pairs, when the request provides a name
        if (!TextUtils.isEmpty(alias_name)) {
          bundle_value = StringUtils.toBundle(hashmap_value);
          intent.putExtra(alias_name, (Bundle) bundle_value);
        }
      }
    }

    service.startActivity(intent);
  }

  private void startImageViewerActivity(NetworkingService service, byte[] pic) {
    Log.d(tag, "starting ImageViewerActivity");
    Intent intent = new Intent(service, ImageViewerActivity.class);
    intent.putExtra(Constant.Extra.RAW_IMAGE_DATA, pic);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    service.startActivity(intent);
  }

  private void startVideoPlayerActivity(NetworkingService service) {
    startVideoPlayerActivity(service, /* enterPipMode */ false);
  }

  private void startVideoPlayerActivity(NetworkingService service, boolean enterPipMode) {
    Log.d(tag, "starting VideoPlayerActivity" + (enterPipMode ? " in PiP mode" : ""));
    Intent intent = new Intent(service, VideoPlayerActivity.class);
    intent.putExtra(Constant.Extra.ENTER_PIP_MODE, enterPipMode);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    service.startActivity(intent);
  }

  private void startRuntimePermissionsRequestActivity(NetworkingService service, int requestCode) {
    Log.d(tag, "starting RuntimePermissionsRequestActivity");
    Intent intent = new Intent(service, RuntimePermissionsRequestActivity.class);
    intent.putExtra(Constant.Extra.PERMISSION_REQUEST_CODE, requestCode);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    service.startActivity(intent);
  }

  // ===========================================================================
  // Network Requests (in separate Thread to avoid NetworkOnMainThreadException)
  // ===========================================================================

  private ArrayList<String> extractPlaylists(String uri, ArrayList<String> playlists) {
    if (playlists == null)
      playlists = new ArrayList<String>();

    ArrayList<String> matches = null;

    if (matches == null)
      matches = serializedExtractor.expandPlaylist(uri);

    if (matches == null)
      matches = httpM3uExtractor.expandPlaylist(uri); //8-bit ascii

    if (matches == null)
      matches = httpHtmlExtractor.expandPlaylist(uri, (String) null); //utf8

    if (matches == null)
      matches = contentM3uExtractor.expandPlaylist(uri); //utf8

    if (matches == null)
      matches = fileM3uExtractor.expandPlaylist(uri); //utf8

    if (matches == null)
      matches = directoryExtractor.expandPlaylist(uri);

    if (matches == null)
      matches = recursiveDirectoryExtractor.expandPlaylist(uri);

    if (matches != null) {
      playlists.add(uri);

      int index = 0;
      ArrayList<String> nested;

      while (index < matches.size()) {
        uri = matches.get(index);
        if (playlists.contains(uri)) {
          // prevent infinite recursion by disallowing circular references
          matches.remove(index);
          continue;
        }

        nested = extractPlaylists(uri, playlists);
        if (nested != null) {
          // replace uri at position 'index' with nested playlist
          matches.remove(index);
          matches.addAll(index, nested);
          continue;
        }

        // keep uri at position 'index' and advance to next uri in list
        index++;
      }
    }

    return matches;
  }

  private void addItems(
    PlayerManager playerManager,
    NetworkingService service,
    ArrayList<String> matches,
    String uri,
    String caption,
    String referer,
    HashMap<String, String> reqHeadersMap,
    boolean useCache,
    float startPosition,
    float stopPosition,
    String drm_scheme,
    String drm_license_server,
    HashMap<String, String> drmHeadersMap,
    boolean remove_previous_items
  ) {
    final Handler  handler  = new Handler(mainLooper);
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        String playUrl;

        if (matches == null) {
          playerManager.addItem(uri, caption, referer, reqHeadersMap, useCache, startPosition, stopPosition, drm_scheme, drm_license_server, drmHeadersMap, remove_previous_items);

          playUrl = uri;
        }
        else {
          Log.d(tag, "count of URLs in playlist: " + matches.size());
          String[] uris = matches.toArray(new String[matches.size()]);
          playerManager.addItems(uris, caption, referer, reqHeadersMap, useCache, startPosition, stopPosition, drm_scheme, drm_license_server, drmHeadersMap, remove_previous_items);
          playUrl = uris[0];
        }

        if (remove_previous_items && MediaTypeUtils.isVideoFileUrl(playUrl)) {
          startVideoPlayerActivity(service);
        }
      }
    };

    handler.post(runnable);
  }

  // ===========================================================================
  // External Storage Permissions
  // ===========================================================================

  private boolean requiresExternalStoragePermission(NetworkingService service, Message msg, String playUrl, String textUrl) {
    boolean requiresPermission = ExternalStorageUtils.isFileUri(playUrl) || ExternalStorageUtils.isFileUri(textUrl);

    if (requiresPermission)
      requiresPermission = !ExternalStorageUtils.has_permission(service);

    if (requiresPermission) {
      externalStorageMessages.add(msg);
      startRuntimePermissionsRequestActivity(service, Constant.PermissionRequestCode.READ_EXTERNAL_STORAGE);
    }
    return requiresPermission;
  }

  private void handleExternalStorageMessages() {
    Log.d(tag, "READ_EXTERNAL_STORAGE permission granted. Count of Messages to process: " + externalStorageMessages.size());

    final Handler  handler  = new Handler(mainLooper);
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        if (!externalStorageMessages.isEmpty()) {
          handleMessage(externalStorageMessages.remove(0));
          handler.postDelayed(this, 1000l);
        }
      }
    };

    handler.post(runnable);
  }

}
