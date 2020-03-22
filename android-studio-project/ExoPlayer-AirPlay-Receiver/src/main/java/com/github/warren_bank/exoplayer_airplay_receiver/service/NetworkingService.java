package com.github.warren_bank.exoplayer_airplay_receiver.service;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.RequestListenerThread;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.DirectoryIndexMediaPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.FileM3uPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.HttpHtmlPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors.HttpM3uPlaylistExtractor;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.ImageActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.VideoPlayerActivity;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.NetworkUtils;

public class NetworkingService extends Service {
  private static final int NOTIFICATION_ID = 1;
  private static final String ACTION_STOP  = "STOP";

  private static final String tag = NetworkingService.class.getSimpleName();
  private static final String airplayType = "._airplay._tcp.local";
  private static final String raopType = "._raop._tcp.local";

  private String airplayName = "ExoPlayer AirPlay Receiver";
  private ServiceHandler handler;
  private InetAddress localAddress;
  private JmDNS jmdnsAirplay = null;
  private JmDNS jmdnsRaop;
  private ServiceInfo airplayService = null;
  private ServiceInfo raopService;

  private HashMap<String, String> values = new HashMap<String, String>();
  private String preMac;

  private RequestListenerThread thread;

  private WifiManager.MulticastLock lock;

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(tag, "register service onCreate");

    WifiManager wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
    lock = wifi.createMulticastLock("mylockthereturn");
    lock.setReferenceCounted(true);
    lock.acquire();

    // run ServiceHandler in a separate Thread to avoid NetworkOnMainThreadException
    HandlerThread handlerThread = new HandlerThread("ServiceHandlerThread");
    handlerThread.start();

    handler = new ServiceHandler(handlerThread.getLooper(), NetworkingService.this);
    MainApp.registerHandler(NetworkingService.class.getName(), handler);

    Toast toast = android.widget.Toast.makeText(getApplicationContext(), "Registering Airplay service...", android.widget.Toast.LENGTH_SHORT);
    toast.setGravity(Gravity.CENTER, 0, 0);
    toast.show();
    showNotification();

    airplayName = Build.MODEL + "@" + airplayName;

    new Thread() {
      public void run() {
        try {
          thread = new RequestListenerThread();
          thread.setDaemon(false);
          thread.start();

          registerAirplay();
        }
        catch (IOException e) {
          e.printStackTrace();
          Message msg = Message.obtain();
          msg.what = Constant.Register.FAIL;
          MainApp.broadcastMessage(msg);
          return;
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
    Log.d(tag, "NetworkingService onDestroy");

    shutdown(false);
  }

  private void shutdown(boolean useForce) {
    if (lock == null) return;

    if (lock.isHeld()) lock.release();
    lock = null;

    MainApp.unregisterHandler(NetworkingService.class.getName());
    hideNotification();

    new Thread() {
      public void run() {
        try {
          if (thread != null) {
            thread.destroy();
            thread = null;
          }

          unregisterAirplay();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
        finally {
          if (useForce) stopSelf();
        }
      }
    }.start();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void registerAirplay() throws IOException {
    Message msg = Message.obtain();
    if (!getParams()) {
      msg.what = Constant.Register.FAIL;
    }
    else {
      register();
      msg.what = Constant.Register.OK;
      Log.d(tag, "airplay register airplay success");
    }
    MainApp.broadcastMessage(msg);
  }

  private void register() throws IOException {
    Log.d(tag, "airplay register");
    registerTcpLocal();
    registerRaopLocal();
  }

  private void registerTcpLocal() throws IOException {
    airplayService = ServiceInfo.create(airplayName + airplayType, airplayName, Constant.AIRPLAY_PORT, 0, 0, values);
    jmdnsAirplay = JmDNS.create(localAddress); //create must bind IP address (android 4.0+)
    jmdnsAirplay.registerService(airplayService);
  }

  private void registerRaopLocal() throws IOException {
    String raopName = preMac + "@" + airplayName;
    raopService = ServiceInfo.create(raopName + raopType, raopName, Constant.RAOP_PORT, "tp=UDP sm=false sv=false ek=1 et=0,1 cn=0,1 ch=2 ss=16 sr=44100 pw=false vn=3 da=true md=0,1,2 vs=103.14 txtvers=1");
    jmdnsRaop = JmDNS.create(localAddress);
    jmdnsRaop.registerService(raopService);
  }

  private boolean getParams() {
    String strMac = null;

    try {
      Thread.sleep(2 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (localAddress == null)
      localAddress = NetworkUtils.getLocalIpAddress(); //Get local IP object

    if (localAddress == null) {
      Log.d(tag, "local address = null");
      return false;
    }
    String[] str_Array = new String[2];
    try {
      str_Array = NetworkUtils.getMACAddress(localAddress);
      if (str_Array == null)
        return false;
      strMac = str_Array[0].toUpperCase(Locale.ENGLISH);
      preMac = str_Array[1].toUpperCase(Locale.ENGLISH);
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    Log.d(tag, "airplay registered. Airplay Mac address：" + strMac + "; preMac = " + preMac);

    values.put("deviceid", strMac);
    values.put("features", "0x297f");
    values.put("model", "AppleTV2,1");
    values.put("srcvers", "130.14");

    return true;
  }

  private void unregisterAirplay() {
    Log.d(tag, "un register airplay service");

    if (jmdnsAirplay != null) {
      try {
        jmdnsAirplay.unregisterService(airplayService);
        jmdnsAirplay.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      finally {
        jmdnsAirplay = null;
      }
    }

    if (jmdnsRaop != null) {
      try {
        jmdnsRaop.unregisterService(raopService);
        jmdnsRaop.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      finally {
        jmdnsRaop = null;
      }
    }
  }

  // -------------------------------------------------------------------------
  // foregrounding..

  private void showNotification() {
    Notification notification = getNotification();

    if (Build.VERSION.SDK_INT >= 5) {
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
      NM.cancel(NOTIFICATION_ID);
    }
  }

  private Notification getNotification() {
    Notification notification  = new Notification();
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

    RemoteViews contentView    = new RemoteViews(getPackageName(), R.layout.service_notification);
    contentView.setImageViewResource(R.id.notification_icon, R.drawable.launcher);
    contentView.setTextViewText(R.id.notification_text_line1, getString(R.string.notification_service_content_line1));
    contentView.setTextViewText(R.id.notification_text_line2, getNetworkAddress());
    contentView.setTextViewText(R.id.notification_text_line3, getString(R.string.notification_service_content_line3));
    notification.contentView   = contentView;

    return notification;
  }

  private PendingIntent getPendingIntent_StopService() {
    Intent intent = new Intent(NetworkingService.this, NetworkingService.class);
    intent.setAction(ACTION_STOP);

    return PendingIntent.getService(NetworkingService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private String getNetworkAddress() {
    if (localAddress == null)
      localAddress = NetworkUtils.getLocalIpAddress(); //Get local IP object

    return (localAddress == null)
      ? "[offline]"
      : localAddress.getHostAddress() + ":" + Constant.AIRPLAY_PORT;
  }

  // -------------------------------------------------------------------------
  // process inbound intents

  private void processIntent(Intent intent) {
    if (intent == null) return;

    String action = intent.getAction();
    if (action == ACTION_STOP)
      shutdown(true);
  }

  // -------------------------------------------------------------------------
  // message handler

  private static class ServiceHandler extends Handler {

    private class DelayedVideoPlayerIntentRunnable implements Runnable {
      private NetworkingService service;
      private String uri;
      private String referer;

      public DelayedVideoPlayerIntentRunnable(NetworkingService service, String uri, String referer) {
        this.service = service;
        this.uri     = uri;
        this.referer = referer;
      }

      @Override
      public void run() {
        sendVideoPlayerIntent(
          service,
          /* mode=          */ "queue",
          /* uri=           */ uri,
          /* caption=       */ "",
          /* referer=       */ referer,
          /* startPosition= */ 0d
        );
      }
    }

    private WeakReference<NetworkingService> weakReference;

    private HttpM3uPlaylistExtractor             httpM3uExtractor;
    private HttpHtmlPlaylistExtractor            httpHtmlExtractor;
    private FileM3uPlaylistExtractor             fileM3uExtractor;
    private DirectoryIndexMediaPlaylistExtractor directoryExtractor;

    public ServiceHandler(Looper looper, NetworkingService service) {
      super(looper);

      weakReference = new WeakReference<NetworkingService>(service);

      httpM3uExtractor   = new HttpM3uPlaylistExtractor();
      httpHtmlExtractor  = new HttpHtmlPlaylistExtractor();
      fileM3uExtractor   = new FileM3uPlaylistExtractor();
      directoryExtractor = new DirectoryIndexMediaPlaylistExtractor();
    }

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      NetworkingService service = weakReference.get();
      if (service == null)
        return;
      switch (msg.what) {
        case Constant.Register.OK : {
          Toast toast = Toast.makeText(service.getApplicationContext(), "Airplay registration success", android.widget.Toast.LENGTH_SHORT);
          toast.setGravity(Gravity.CENTER, 0, 0);
          toast.show();
        }
          break;
        case Constant.Register.FAIL : {
          Toast toast = Toast.makeText(service.getApplicationContext(), "Airplay registration failed", android.widget.Toast.LENGTH_SHORT);
          toast.setGravity(Gravity.CENTER, 0, 0);
          toast.show();
          service.stopSelf();
          android.os.Process.killProcess(android.os.Process.myPid()); //Quit the program completely
          break;
        }
        case Constant.Msg.Msg_Photo : {
          sendImageViewerIntent(
            service,
            /* pic= */ (byte[]) msg.obj
          );
          break;
        }
        case Constant.Msg.Msg_Video_Play  :
        case Constant.Msg.Msg_Video_Queue : {
          HashMap<String, String> map = (HashMap) msg.obj;
          String playUrl  = map.get(Constant.PlayURL);
          String textUrl  = map.get(Constant.CaptionURL);
          String referUrl = map.get(Constant.RefererURL);
          String startPos = map.get(Constant.Start_Pos);

          ArrayList<String> matches = null;

          if (matches == null)
            matches = httpM3uExtractor.expandPlaylist(playUrl); //8-bit ascii

          if (matches == null)
            matches = httpHtmlExtractor.expandPlaylist(playUrl, (String) null); //utf8

          if (matches == null)
            matches = fileM3uExtractor.expandPlaylist(playUrl); //utf8

          if (matches == null)
            matches = directoryExtractor.expandPlaylist(playUrl);

          if (matches != null) {
            for (int counter = 0; counter < matches.size(); counter++) {
              if (counter == 0) {
                sendVideoPlayerIntent(
                  service,
                  /* mode=          */ ((msg.what == Constant.Msg.Msg_Video_Play) ? "play" : "queue"),
                  /* uri=           */ matches.get(counter),
                  /* caption=       */ textUrl,
                  /* referer=       */ referUrl,
                  /* startPosition= */ Double.valueOf(startPos)
                );
              }
              else {
                ServiceHandler.this.postDelayed(
                  new DelayedVideoPlayerIntentRunnable(
                    service,
                    /* uri=     */ matches.get(counter),
                    /* referer= */ referUrl
                  ),
                  (1000l * counter)
                );
              }
            }
            break;
          }

          sendVideoPlayerIntent(
            service,
            /* mode=          */ ((msg.what == Constant.Msg.Msg_Video_Play) ? "play" : "queue"),
            /* uri=           */ playUrl,
            /* caption=       */ textUrl,
            /* referer=       */ referUrl,
            /* startPosition= */ Double.valueOf(startPos)
          );
          break;
        }

      }
    }

    private void sendVideoPlayerIntent(NetworkingService service, String mode, String uri, String caption, String referer, double startPosition) {
      Intent intent = new Intent(service, VideoPlayerActivity.class);
      intent.putExtra("mode",          mode);
      intent.putExtra("uri",           uri);
      intent.putExtra("caption",       caption);
      intent.putExtra("referer",       referer);
      intent.putExtra("startPosition", startPosition);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      service.startActivity(intent);
    }

    private void sendImageViewerIntent(NetworkingService service, byte[] pic) {
      Intent intent = new Intent(service, ImageActivity.class);
      intent.putExtra("picture", pic);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      service.startActivity(intent);
    }

  }
}
