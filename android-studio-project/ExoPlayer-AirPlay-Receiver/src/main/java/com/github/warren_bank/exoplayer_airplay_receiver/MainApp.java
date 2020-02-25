package com.github.warren_bank.exoplayer_airplay_receiver;

import java.util.concurrent.ConcurrentHashMap;

import android.app.Application;
import android.os.Handler;
import android.os.Message;

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
  }

  public ConcurrentHashMap<String, Handler> getHandlerMap() {
    return mHandlerMap;
  }
}
