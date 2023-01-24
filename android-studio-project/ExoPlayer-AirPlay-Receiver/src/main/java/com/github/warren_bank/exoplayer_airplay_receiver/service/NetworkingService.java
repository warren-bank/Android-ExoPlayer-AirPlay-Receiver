package com.github.warren_bank.exoplayer_airplay_receiver.service;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.PlayerNotificationManagerContainer;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.PlayerManager;
import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.RequestListenerThread;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.VideoPlayerActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.NetworkUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ResourceUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.WakeLockMgr;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Locale;

public class NetworkingService extends Service implements RequestListenerThread.Callback {
  private static final String tag          = NetworkingService.class.getSimpleName();
  private static final String ACTION_STOP  = "STOP";
  public  static final String ACTION_PLAY  = "PLAY";

  private static PlayerManager playerManager = null;

  private PlayerNotificationManagerContainer playerNotificationManager;
  private MyPlaybackInfoSource playbackInfoSource;
  private String airplayName;
  private InetAddress localAddress;
  private AirPlayBonjour airPlayBonjour;
  private MyMessageHandler handler;
  private RequestListenerThread thread;

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(tag, "onCreate");

    playerManager             = PlayerManager.createPlayerManager(     /* context= */ NetworkingService.this);
    playerNotificationManager = new PlayerNotificationManagerContainer(/* context= */ NetworkingService.this, playerManager, /* pendingIntentActivityClass= */ VideoPlayerActivity.class);
    playbackInfoSource        = new MyPlaybackInfoSource(playerManager);
    airplayName               = Build.MODEL + "@" + getString(R.string.app_name);
    localAddress              = null;
    airPlayBonjour            = null;

    WakeLockMgr.acquire(/* context= */ NetworkingService.this);

    // Handler runs in UI Thread.
    // SimpleExoPlayer requires that all interaction occurs in the same Thread it was initialized.
    //   note: all network requests will need to occur in a separate Thread.
    handler = new MyMessageHandler(Looper.getMainLooper(), NetworkingService.this);
    MainApp.registerHandler(NetworkingService.class.getName(), handler);

    Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.toast_registration_started), Toast.LENGTH_SHORT);
    toast.setGravity(Gravity.CENTER, 0, 0);
    toast.show();
    showNotification();

    new Thread() {
      public void run() {
        try {
          thread = new RequestListenerThread(/* context= */ NetworkingService.this, /* RequestListenerThread.PlaybackInfoSource */ playbackInfoSource, /* RequestListenerThread.Callback */ NetworkingService.this);
          thread.setDaemon(false);
          thread.start();

          registerAirplay();
        }
        catch (IOException e) {
          Log.e(tag, "problem initializing HTTP server and Bonjour services", e);

          Message msg = Message.obtain();
          msg.what = Constant.Register.FAIL;
          MainApp.broadcastMessage(msg);
        }
      }
    }.start();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    onStart(intent, startId);
    return START_STICKY;
  }

  @Override
  public void onStart(Intent intent, int startId) {
    processIntent(intent);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(tag, "onDestroy");

    shutdown();
  }

  private void shutdown() {
    if (playerManager == null) return;

    MainApp.unregisterHandler(NetworkingService.class.getName());
    hide_player();
    hideNotification();
    playerNotificationManager.release();
    WakeLockMgr.release();

    new Thread() {
      public void run() {
        try {
          unregisterAirplay();

          if (thread != null) {
            thread.destroy();
            thread = null;
          }
        }
        catch (Exception e) {
          Log.e(tag, "problem shutting down HTTP server and Bonjour services", e);
        }
        finally {
          playerManager.release(/* shutdown= */ true, /* delay_ms= */ 250l);
          playerManager = null;
        }
      }
    }.start();
  }

  private void hide_player() {
    Message msg = Message.obtain();
    msg.what = Constant.Msg.Msg_Hide_Player;
    MainApp.broadcastMessage(msg);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void registerAirplay() throws IOException {
    Message msg = Message.obtain();
    try {
      Thread.sleep(2 * 1000);
    }
    catch (InterruptedException e) {
      Log.e(tag, "problem putting thread to sleep to allow HTTP server time to initialize prior to registering Bonjour services", e);
    }
    try {
      Log.d(tag, "Beginning registration of Bonjour services..");

      if (localAddress == null)
        localAddress = NetworkUtils.getLocalIpAddress(NetworkingService.this);

      if (localAddress == null) {
        Log.d(tag, "No local IP address found for any network interface that supports multicast");
        throw new Exception("");
      }

      if (airPlayBonjour == null) {
        airPlayBonjour = new AirPlayBonjour(airplayName);
        airPlayBonjour.start(localAddress, Constant.AIRPLAY_PORT, Constant.RAOP_PORT);
      }

      msg.what = Constant.Register.OK;
      Log.d(tag, "Bonjour services successfully registered");
    }
    catch(Exception e) {
      msg.what = Constant.Register.FAIL;
      Log.d(tag, "Bonjour services NOT successfully registered");
    }
    MainApp.broadcastMessage(msg);
  }

  private void unregisterAirplay() {
    Log.d(tag, "Unregistering Bonjour services");

    if (airPlayBonjour != null) {
      airPlayBonjour.stop();
      airPlayBonjour = null;
    }
  }

  // -------------------------------------------------------------------------
  // foregrounding..

  private String getNotificationChannelId() {
    return getPackageName();
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= 26) {
      String channelId       = getNotificationChannelId();
      NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      NotificationChannel NC = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH);

      NC.setDescription(channelId);
      NC.setSound(null, null);
      NM.createNotificationChannel(NC);
    }
  }

  private int getNotificationId() {
    return ResourceUtils.getInteger(NetworkingService.this, R.integer.NOTIFICATION_ID_NETWORKING_SERVICE);
  }

  private void showNotification() {
    Notification notification = getNotification();
    int NOTIFICATION_ID = getNotificationId();

    if (Build.VERSION.SDK_INT >= 5) {
      createNotificationChannel();
      startForeground(NOTIFICATION_ID, notification);
    }
    else {
      NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      NM.notify(NOTIFICATION_ID, notification);
    }
  }

  private void hideNotification() {
    if (Build.VERSION.SDK_INT >= 5) {
      stopForeground(true);
    }
    else {
      NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      int NOTIFICATION_ID    = getNotificationId();
      NM.cancel(NOTIFICATION_ID);
    }
  }

  private Notification getNotification() {
    Notification notification;

    if (Build.VERSION.SDK_INT >= 26) {
      Notification.Builder builder = new Notification.Builder(/* context= */ NetworkingService.this, /* channelId= */ getNotificationChannelId());

      if (Build.VERSION.SDK_INT >= 31) {
        builder.setContentTitle(getNetworkAddress());
        builder.setContentText(getString(R.string.notification_service_content_line3));
        builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
      }

      notification = builder.build();
    }
    else {
      notification = new Notification();
    }

    notification.when          = System.currentTimeMillis();
    notification.flags         = 0;
    notification.flags        |= Notification.FLAG_ONGOING_EVENT;
    notification.flags        |= Notification.FLAG_NO_CLEAR;
    notification.icon          = R.drawable.launcher;
    notification.tickerText    = getString(R.string.notification_service_ticker);
    notification.contentIntent = getPendingIntent_StopService();
    notification.deleteIntent  = getPendingIntent_StopService();

    if (Build.VERSION.SDK_INT >= 16) {
      notification.priority    = Notification.PRIORITY_HIGH;
    }
    else {
      notification.flags      |= Notification.FLAG_HIGH_PRIORITY;
    }

    if (Build.VERSION.SDK_INT >= 21) {
      notification.visibility  = Notification.VISIBILITY_PUBLIC;
    }

    RemoteViews contentView    = new RemoteViews(getPackageName(), R.layout.service_notification);
    contentView.setImageViewResource(R.id.notification_icon, R.drawable.launcher);
    contentView.setTextViewText(R.id.notification_text_line1, getString(R.string.notification_service_content_line1));
    contentView.setTextViewText(R.id.notification_text_line2, getNetworkAddress());
    contentView.setTextViewText(R.id.notification_text_line3, getString(R.string.notification_service_content_line3));

    if (Build.VERSION.SDK_INT < 31)
      notification.contentView = contentView;
    if (Build.VERSION.SDK_INT >= 16)
      notification.bigContentView = contentView;
    if (Build.VERSION.SDK_INT >= 21)
      notification.headsUpContentView = contentView;

    return notification;
  }

  private PendingIntent getPendingIntent_StopService() {
    Intent intent = new Intent(NetworkingService.this, NetworkingService.class);
    intent.setAction(ACTION_STOP);

    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
    if (Build.VERSION.SDK_INT >= 23)
      flags |= PendingIntent.FLAG_IMMUTABLE;

    return PendingIntent.getService(NetworkingService.this, 0, intent, flags);
  }

  private String getNetworkAddress() {
    if (localAddress == null)
      localAddress = NetworkUtils.getLocalIpAddress(NetworkingService.this);

    return (localAddress == null)
      ? "[offline]"
      : localAddress.getHostAddress() + ":" + Constant.AIRPLAY_PORT;
  }

  // -------------------------------------------------------------------------
  // process inbound intents

  private void processIntent(Intent intent) {
    if (intent == null) return;

    String action = intent.getAction();
    if (action == null) return;

    switch (action) {
      case ACTION_STOP : {
        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Exit_Service;
        MainApp.broadcastMessage(msg);
        break;
      }
      case ACTION_PLAY : {
        if (intent.hasExtra(Constant.PlayURL)) {

          HashMap<String, String> dataMap = new HashMap<String, String>();
          dataMap.put(Constant.PlayURL,    intent.getStringExtra(Constant.PlayURL)    );
          dataMap.put(Constant.CaptionURL, intent.getStringExtra(Constant.CaptionURL) );
          dataMap.put(Constant.RefererURL, intent.getStringExtra(Constant.RefererURL) );
          dataMap.put(Constant.UseCache,   intent.getStringExtra(Constant.UseCache)   );
          dataMap.put(Constant.Start_Pos,  intent.getStringExtra(Constant.Start_Pos)  );
          dataMap.put(Constant.Stop_Pos,   intent.getStringExtra(Constant.Stop_Pos)   );
          dataMap.put(Constant.DRM_Scheme, intent.getStringExtra(Constant.DRM_Scheme) );
          dataMap.put(Constant.DRM_URL,    intent.getStringExtra(Constant.DRM_URL)    );

          HashMap<String, String> reqHeadersMap = StringUtils.parseDuplicateKeyValues((String[]) getStringArrayExtra(intent, Constant.ReqHeader),  /* normalize_lowercase_keys= */ true);
          HashMap<String, String> drmHeadersMap = StringUtils.parseDuplicateKeyValues((String[]) getStringArrayExtra(intent, Constant.DRM_Header), /* normalize_lowercase_keys= */ true);

          HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
          map.put(Constant.Video_Source_Map.DATA,        dataMap);
          map.put(Constant.Video_Source_Map.REQ_HEADERS, reqHeadersMap);
          map.put(Constant.Video_Source_Map.DRM_HEADERS, drmHeadersMap);

          Message msg = Message.obtain();
          msg.what = Constant.Msg.Msg_Video_Play;
          msg.obj  = map;

          handler.handleMessage(msg);
        }
        break;
      }
      case Intent.ACTION_MEDIA_BUTTON : {
        if (intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
          try {
            KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (keyEvent == null) {
              int code = intent.getIntExtra(Intent.EXTRA_KEY_EVENT, -1);
              if (code >= 0) {
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, code);
              }
            }

            if (keyEvent != null) {
              boolean isHandled = false;

              if (!isHandled && (playerManager != null))
                isHandled = playerManager.dispatchKeyEvent(keyEvent);

              if (!isHandled) {
                switch(keyEvent.getKeyCode()) {

                  case KeyEvent.KEYCODE_TV_POWER : {
                    Message msg = Message.obtain();
                    msg.what = Constant.Msg.Msg_Exit_Service;
                    MainApp.broadcastMessage(msg);
                    isHandled = true;
                    break;
                  }

                  case KeyEvent.KEYCODE_WINDOW : {
                    if (VideoPlayerActivity.isVisible) {
                      boolean enterPipMode = !VideoPlayerActivity.isPipMode;
                      Log.d(tag, "restarting VideoPlayerActivity" + (enterPipMode ? " in PiP mode" : ""));

                      Intent intent2 = new Intent(NetworkingService.this, VideoPlayerActivity.class);
                      intent2.putExtra(Constant.Extra.ENTER_PIP_MODE, enterPipMode);
                      intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                      startActivity(intent2);
                      isHandled = true;
                    }
                    break;
                  }

                }
              }
            }
          }
          catch(Exception e) {}
        }
        break;
      }
    }
  }

  private String[] getStringArrayExtra(Intent intent, String key) {
    String[] extra = (String[]) intent.getStringArrayExtra(key);

    if (extra == null) {
      String value = (String) intent.getStringExtra(key);

      if (value != null) {
        extra = new String[] {value};
      }
    }

    return extra;
  }

  // -------------------------------------------------------------------------
  // provide VideoActivity access to PlayerManager

  public static PlayerManager getPlayerManager() {
    return playerManager;
  }

  // -------------------------------------------------------------------------
  // implement interface: RequestListenerThread.Callback

  public void onNewIpAddress() {
    showNotification();
  }

}
