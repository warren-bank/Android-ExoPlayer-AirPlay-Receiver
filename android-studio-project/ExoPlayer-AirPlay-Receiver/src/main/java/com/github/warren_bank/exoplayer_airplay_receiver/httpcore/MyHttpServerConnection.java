package com.github.warren_bank.exoplayer_airplay_receiver.httpcore;

import repackaged.org.apache.http.HttpRequestFactory;
import repackaged.org.apache.http.impl.DefaultHttpServerConnection;
import repackaged.org.apache.http.impl.io.HttpRequestParser;
import repackaged.org.apache.http.io.HttpMessageParser;
import repackaged.org.apache.http.io.SessionInputBuffer;
import repackaged.org.apache.http.params.HttpParams;

import android.util.Log;

import java.net.Socket;

public class MyHttpServerConnection extends DefaultHttpServerConnection {

  private static final String tag = MyHttpServerConnection.class.getSimpleName();

  public MyHttpServerConnection() {
    super();
  }

  public Socket getCurrentSocket() {
    return super.getSocket();
  }

  protected HttpMessageParser createRequestParser(final SessionInputBuffer buffer, final HttpRequestFactory requestFactory, final HttpParams params) {
    Log.d(tag, "airplay in MyHttpServerConnection ");

    //Need to add a custom requestFactory, deal with HTTP1.1 200 OK of Reverse request
    return new HttpRequestParser(buffer, new MyLineParser(), new MyHttpRequestFactory(), params);
  }
}
