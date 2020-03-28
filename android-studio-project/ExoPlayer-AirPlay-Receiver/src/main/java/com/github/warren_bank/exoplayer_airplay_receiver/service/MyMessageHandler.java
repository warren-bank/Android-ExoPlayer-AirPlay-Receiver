package com.github.warren_bank.exoplayer_airplay_receiver.service;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.PlayerManager;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.DirectoryIndexMediaPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.DirectoryIndexRecursiveMediaPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.FileM3uPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.HttpHtmlPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.HttpM3uPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.ImageViewerActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.RuntimePermissionsRequestActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.VideoPlayerActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

final class MyMessageHandler extends Handler {
  private static final String tag = NetworkingService.class.getSimpleName();

  private Looper                                        mainLooper;
  private WeakReference<NetworkingService>              weakReference;

  private ArrayList<Message>                            externalStorageMessages;

  private HandlerThread                                 networkingHandlerThread;
  private HttpM3uPlaylistExtractor                      httpM3uExtractor;
  private HttpHtmlPlaylistExtractor                     httpHtmlExtractor;
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

    httpM3uExtractor            = new HttpM3uPlaylistExtractor();
    httpHtmlExtractor           = new HttpHtmlPlaylistExtractor();
    fileM3uExtractor            = new FileM3uPlaylistExtractor();
    directoryExtractor          = new DirectoryIndexMediaPlaylistExtractor();
    recursiveDirectoryExtractor = new DirectoryIndexRecursiveMediaPlaylistExtractor();
  }

  @Override
  public void handleMessage(Message msg) {
    super.handleMessage(msg);

    PlayerManager playerManager = NetworkingService.getPlayerManager();
    if (playerManager == null)
      return;

    NetworkingService service = weakReference.get();
    if (service == null)
      return;

    switch (msg.what) {

      // =======================================================================
      // Bonjour service registration:
      // =======================================================================

      case Constant.Register.OK : {
        Toast toast = Toast.makeText(service.getApplicationContext(), service.getText(R.string.toast_registration_success), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        break;
      }

      case Constant.Register.FAIL : {
        Toast toast = Toast.makeText(service.getApplicationContext(), service.getText(R.string.toast_registration_failure), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        service.stopSelf();
        Process.killProcess(Process.myPid()); //Quit the program completely
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
      // Add media URLs to ExoPlayer queue.
      // Display a video player when starting playback of a video URL,
      //   or explicitly requested to do so.
      // =======================================================================

      case Constant.Msg.Msg_Video_Play  :
      case Constant.Msg.Msg_Video_Queue : {
        HashMap<String, String> map = (HashMap) msg.obj;
        String playUrl  = map.get(Constant.PlayURL);
        String textUrl  = map.get(Constant.CaptionURL);
        String referUrl = map.get(Constant.RefererURL);
        String startPos = map.get(Constant.Start_Pos);

        // normalize empty data fields to: null
        if (TextUtils.isEmpty(playUrl))
          playUrl = null;
        if (TextUtils.isEmpty(textUrl))
          textUrl = null;
        if (TextUtils.isEmpty(referUrl))
          referUrl = null;
        if (TextUtils.isEmpty(startPos))
          startPos = "0";

        // ignore bad requests
        if (playUrl == null)
          break;

        Log.d(tag, ((msg.what == Constant.Msg.Msg_Video_Play) ? "play" : "queue") + " media: url = " + playUrl + "; position = " + startPos + "; captions = " + textUrl + "; referer = " + referUrl);

        if (requiresExternalStoragePermission(service, msg, playUrl, textUrl))
          break;

        // normalize references to external storage by converting absolute filesystem paths to file: URIs
        if (ExternalStorageUtils.isFileUri(playUrl))
          playUrl = ExternalStorageUtils.normalizeFileUri(playUrl);
        if (ExternalStorageUtils.isFileUri(textUrl))
          textUrl = ExternalStorageUtils.normalizeFileUri(textUrl);

        // offload to a worker Thread
        extractPlaylists(
          playerManager,
          service,
          /* uri=           */          playUrl,
          /* caption=       */          textUrl,
          /* referer=       */          referUrl,
          /* startPosition= */          Float.valueOf(startPos),
          /* remove_previous_items= */  (msg.what == Constant.Msg.Msg_Video_Play)
        );
        break;
      }

      case Constant.Msg.Msg_Show_Player : {
        startVideoPlayerActivity(service);
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

      // =======================================================================
      // Runtime Permissions:
      // =======================================================================

      case Constant.Msg.Msg_Runtime_Permissions_Granted : {
        handleExternalStorageMessages();
        break;
      }

    }
  }

  private void startImageViewerActivity(NetworkingService service, byte[] pic) {
    Log.d(tag, "starting ImageViewerActivity");
    Intent intent = new Intent(service, ImageViewerActivity.class);
    intent.putExtra("picture", pic);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    service.startActivity(intent);
  }

  private void startVideoPlayerActivity(NetworkingService service) {
    Log.d(tag, "starting VideoPlayerActivity");
    Intent intent = new Intent(service, VideoPlayerActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    service.startActivity(intent);
  }

  private void startRuntimePermissionsRequestActivity(NetworkingService service) {
    Log.d(tag, "starting RuntimePermissionsRequestActivity");
    Intent intent = new Intent(service, RuntimePermissionsRequestActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    service.startActivity(intent);
  }

  // ===========================================================================
  // Network Requests (in separate Thread to avoid NetworkOnMainThreadException)
  // ===========================================================================

  private void extractPlaylists(
    PlayerManager playerManager,
    NetworkingService service,
    String uri,
    String caption,
    String referer,
    float startPosition,
    boolean remove_previous_items
  ) {
    final Handler  handler  = new Handler(networkingHandlerThread.getLooper());
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        ArrayList<String> matches = null;

        if (matches == null)
          matches = httpM3uExtractor.expandPlaylist(uri); //8-bit ascii

        if (matches == null)
          matches = httpHtmlExtractor.expandPlaylist(uri, (String) null); //utf8

        if (matches == null)
          matches = fileM3uExtractor.expandPlaylist(uri); //utf8

        if (matches == null)
          matches = directoryExtractor.expandPlaylist(uri);

        if (matches == null)
          matches = recursiveDirectoryExtractor.expandPlaylist(uri);

        addItems(playerManager, service, matches, uri, caption, referer, startPosition, remove_previous_items);
      }
    };

    handler.post(runnable);
  }

  private void addItems(
    PlayerManager playerManager,
    NetworkingService service,
    ArrayList<String> matches,
    String uri,
    String caption,
    String referer,
    float startPosition,
    boolean remove_previous_items
  ) {
    final Handler  handler  = new Handler(mainLooper);
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        String playUrl;

        if (matches == null) {
          playerManager.addItem(uri, caption, referer, startPosition, remove_previous_items);

          playUrl = uri;
        }
        else {
          Log.d(tag, "count of URLs in playlist: " + matches.size());

          String[] uris;
          uris = new String[matches.size()];
          uris = matches.toArray(uris);

          playerManager.addItems(uris, caption, referer, startPosition, remove_previous_items);

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
      startRuntimePermissionsRequestActivity(service);
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
