package com.github.warren_bank.exoplayer_airplay_receiver;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.TLSSocketFactory;

import android.app.Application;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.HttpsURLConnection;

public class MainApp extends Application {
  private static MainApp instance;

  private ConcurrentHashMap<String, Handler> mHandlerMap = new ConcurrentHashMap<String, Handler>();

  public static MainApp getInstance() {
    return instance;
  }

  public static void registerHandler(String name, Handler handler) {
    getInstance().getHandlerMap().put(name, handler);
  }

  public static void unregisterHandler(String name) {
    getInstance().getHandlerMap().remove(name);
  }

  public static void broadcastMessage(Message msg) {
    for (Handler handler : getInstance().getHandlerMap().values()) {
      handler.sendMessage(Message.obtain(msg));
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;

    if (
      (Build.VERSION.SDK_INT >= 16) &&
      (Build.VERSION.SDK_INT <  20)
    ) {
      try {
        TLSSocketFactory socketFactory = new TLSSocketFactory();

        HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
      }
      catch(Exception e) {}
    }
  }

  public ConcurrentHashMap<String, Handler> getHandlerMap() {
    return mHandlerMap;
  }
}
