package com.github.warren_bank.exoplayer_airplay_receiver.httpcore;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.BplistParser;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.NetworkUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;  //org.apache.http.impl.DefaultConnectionReuseStrategy
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestListenerThread extends Thread {
  private static final String tag = RequestListenerThread.class.getSimpleName();

  public  static Map<String, byte[]> photoCacheMaps = Collections.synchronizedMap(new HashMap<String, byte[]>());
  private static Map<String, Socket> socketMaps     = Collections.synchronizedMap(new HashMap<String, Socket>());
  private static String localMac = null;

  private ServerSocket serversocket;
  private HttpParams params;
  private InetAddress localAddress;
  private MyHttpService httpService;

  public RequestListenerThread() {
  }

  public void run() {
    try {
      Thread.sleep(2 * 1000);
      initHttpServer();
    }
    catch (IOException e) {
      Log.e(tag, "problem initializing HTTP server", e);
    }
    catch (InterruptedException e) {
      Log.e(tag, "problem initializing HTTP server", e);
    }
    ExecutorService exec = Executors.newCachedThreadPool();

    while (!Thread.interrupted()) {
      try {
        if (serversocket == null)
          break;

        Socket socket = this.serversocket.accept();
        Log.d(tag, "airplay incoming connection from " + socket.getInetAddress() + "; socket id= [" + socket + "]");

        MyHttpServerConnection conn = new MyHttpServerConnection();
        conn.bind(socket, this.params);

        Thread thread = new WorkerThread(this.httpService, conn, socket);
        thread.setDaemon(true);
        exec.execute(thread);
      }
      catch (IOException e) {
        Log.e(tag, "problem accepting inbound HTTP connection", e);
        break;
      }
    }
    exec.shutdown();
    Log.d(tag, "exec shut down");
  }

  @Override
  public void destroy() {
    try {
      Log.d(tag, "serverSocket destroy");
      this.interrupt();
      this.serversocket.close();
      this.serversocket = null;
    }
    catch (Exception e) {
      Log.e(tag, "problem shutting down HTTP server", e);
    }
  }

  private void initHttpServer() throws IOException {
    Log.d(tag, "airplay init http server");

    localAddress = NetworkUtils.getLocalIpAddress();

    if (localAddress == null) {
      Thread.interrupted();
      return;
    }

    String[] str_Array = new String[2];
    try {
      str_Array = NetworkUtils.getMACAddress(localAddress);
      String strMac = str_Array[0];
      localMac = strMac.toUpperCase(Locale.ENGLISH);
      Log.d(tag, "airplay local mac = " + localMac);
    }
    catch (Exception e) {
      Log.e(tag, "problem determining MAC address of network interface", e);
    }

    serversocket = new ServerSocket(Constant.AIRPLAY_PORT, 2, localAddress);
    serversocket.setReuseAddress(true);

    params = new BasicHttpParams();
    params
      .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
      .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
      .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
      .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

    BasicHttpProcessor httpProcessor = new BasicHttpProcessor(); //http protocol handler
    httpProcessor.addInterceptor(new ResponseDate());            //http protocol interceptor, response date
    httpProcessor.addInterceptor(new ResponseServer());          //Response server
    httpProcessor.addInterceptor(new ResponseContent());         //Response content
    httpProcessor.addInterceptor(new ResponseConnControl());     //Responsive connection control

    //http request handler parser
    HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();

    //http request handler, HttpFileHandler inherits from HttpRequestHandler
    registry.register("*", new WebServiceHandler());

    httpService = new MyHttpService(
      httpProcessor,
      new NoConnectionReuseStrategy(), //DefaultConnectionReuseStrategy()
      new DefaultHttpResponseFactory()
    );
    httpService.setParams(this.params);
    httpService.setHandlerResolver(registry); //Set up a registered request handler for the http service.
  }

  private static class WorkerThread extends Thread {
    private static final String tag = WorkerThread.class.getSimpleName();

    private final MyHttpService httpService;
    private final MyHttpServerConnection conn;
    private final Socket socket;

    public WorkerThread(final MyHttpService httpService, final MyHttpServerConnection conn, final Socket socket) {
      super();
      this.httpService = httpService;
      this.conn = conn;
      this.socket = socket;
    }

    public void run() {
      Log.d(tag, "airplay create new connection thread id = " + this.getId() + " handler http client request, socket id = " + "[" + socket + "]");

      HttpContext context = new BasicHttpContext(null);

      try {
        while (!Thread.interrupted() && this.conn.isOpen()) {
          this.httpService.handleRequest(this.conn, context);
          Log.d(tag, "socket maps size = " + socketMaps.size());

          String needSendReverse = (String) context.getAttribute(Constant.Need_sendReverse);
          if (socketMaps.size() != 1)
            continue;

          String sessionId = (String) socketMaps.entrySet().iterator().next().getKey();
          Log.d(tag, "airplay need send reverse = " + needSendReverse + "; sessionId = " + sessionId);

          if (needSendReverse != null && sessionId != null) {
            if (socketMaps.containsKey(sessionId)) {
              Socket socket = (Socket) socketMaps.get(sessionId);
              String httpMsg = (String) context.getAttribute(Constant.ReverseMsg);
              Log.d(tag, "airplay sendReverseMsg: " + httpMsg + " on socket " + "[" + socket + "]" + "; sessionId = " + sessionId);

              sendReverseMsg(socket, httpMsg);

              context.removeAttribute(Constant.Need_sendReverse);
              context.removeAttribute(Constant.ReverseMsg);

              if (Constant.Status.Status_stop.equals(needSendReverse)) {
                if (socket != null && !socket.isClosed()) {
                  Log.d(tag, "airplay close socket");
                  socket.close();
                  socketMaps.clear();
                }
                this.conn.shutdown();
                this.interrupt();
              }
            }
          }
        }
      }
      catch (IOException e) {
        Log.e(tag, "problem handling HTTP request", e);
      }
      catch (HttpException e) {
        Log.e(tag, "problem handling HTTP request", e);
      }
      finally {
        try {
          Log.d(tag, "connection shutdown");
          this.conn.shutdown();
        }
        catch (IOException e) {
          Log.e(tag, "problem closing HTTP connection", e);
        }
      }
    }

    private void sendReverseMsg(Socket socket, String httpMsg) {
      if (socket == null || TextUtils.isEmpty(httpMsg))
        return;
      if (socket.isConnected()) {
        OutputStreamWriter osw;
        try {
          osw = new OutputStreamWriter(socket.getOutputStream());
          osw.write(httpMsg);
          osw.flush();
        }
        catch (IOException e) {
          Log.e(tag, "problem sending message over reverse HTTP connection", e);
        }
      }
    }
  }

  /**
   * all the protocol interactions between apple client (iphone ipad) and
   * Android device are processed here
   */
  private static class WebServiceHandler implements HttpRequestHandler {
    private static final String tag = WebServiceHandler.class.getSimpleName();

    public WebServiceHandler() {
      super();
    }

    //in this method, we can process the requested business logic
    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
      Log.d(tag, "airplay in WebServiceHandler");

      String method = httpRequest.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);

      // handle CORS pre-flight requests
      if (method.equals("OPTIONS")) {
        String origin  = "*";
        String headers = "*";
        String methods = "GET,POST"; //"GET,HEAD,PUT,POST,DELETE,PATCH"
        Header header  = null;

        header = httpRequest.getFirstHeader("origin");
        if (header != null) origin = header.getValue();

        header = httpRequest.getFirstHeader("access-control-request-headers");
        if (header != null) headers = header.getValue();

        httpResponse.setStatusCode(HttpStatus.SC_NO_CONTENT); //204 No Content
        httpResponse.setHeader("Date", new Date().toString());
        httpResponse.setHeader("Access-Control-Allow-Origin",  origin);
        httpResponse.setHeader("Access-Control-Allow-Headers", headers);
        httpResponse.setHeader("Access-Control-Allow-Methods", methods);
        httpResponse.setHeader("Allow", methods);
        httpResponse.setEntity(new StringEntity(methods, "UTF-8"));
        return;
      }

      MyHttpServerConnection currentConn = (MyHttpServerConnection) httpContext.getAttribute(ExecutionContext.HTTP_CONNECTION);

      String target = httpRequest.getRequestLine().getUri();
      Header typeHead = httpRequest.getFirstHeader("content-type");
      String contentType = "";
      if (null != typeHead)
        contentType = typeHead.getValue();
      Log.d(tag, "airplay  incoming HTTP  method = " + method + "; target = " + target + "; contentType = " + contentType);

      Header sessionHead = httpRequest.getFirstHeader("X-Apple-Session-ID");
      String sessionId = "";

      //IOS 8.4.1 When playing a video, only the sessionId is used when the target is play. Every command in the picture has a sessionId.

      if (sessionHead != null) {
        sessionId = sessionHead.getValue();
        Log.d(tag, "incoming HTTP airplay session id = " + sessionId);

        if (target.equals(Constant.Target.REVERSE) || target.equals(Constant.Target.PLAY)) {
          socketMaps.clear();
          Socket reverseSocket = currentConn.getCurrentSocket();
          if (!socketMaps.containsKey(sessionId)) {
            socketMaps.put(sessionId, reverseSocket);
            Log.d(tag, "airplay add sock to maps");
          }
        }
      }

      byte[] entityContent = null;
      if (httpRequest instanceof HttpEntityEnclosingRequest) {
        HttpEntity entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
        entityContent = EntityUtils.toByteArray(entity);
      }

      if (target.equals(Constant.Target.REVERSE)) {
        httpResponse.setStatusCode(HttpStatus.SC_SWITCHING_PROTOCOLS);

        /*
         * HTTP/1.1 101 Switching Protocols Date: Fri Jul 06 07:17:13
         * 2012 Upgrade: PTTH/1.0 Connection: Upgrade
         */
        httpResponse.setHeader("Upgrade", "PTTH/1.0");
        httpResponse.setHeader("Connection", "Upgrade");

        // Add a HashMap to keep this Socket, <Apple-SessionID> ---- <Socket>
        currentConn.setSocketTimeout(0);
        // Get the current socket
        Socket reverseSocket = currentConn.getCurrentSocket();

        if (null != sessionId) {
          if (!socketMaps.containsKey(sessionId)) {
            socketMaps.put(sessionId, reverseSocket);
            Log.d(tag, "airplay receive Reverse, keep Socket in HashMap, key=" + sessionId + "; value=" + reverseSocket + ";total Map=" + socketMaps);
          }
        }
      }
      else if (target.equals(Constant.Target.SERVER_INFO)) {
        setCommonHeaders(httpResponse, HttpStatus.SC_OK);

        String responseStr = Constant.getServerInfoResponse(localMac);
        httpResponse.setEntity(new StringEntity(responseStr));
      }
      else if (target.equals(Constant.Target.STOP)) { //Stop message
        setCommonHeaders(httpResponse, HttpStatus.SC_OK);

        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Stop;
        MainApp.broadcastMessage(msg);

        httpContext.setAttribute(Constant.Need_sendReverse, Constant.Status.Status_stop);
        httpContext.setAttribute(Constant.ReverseMsg, Constant.getStopEventMsg(0, sessionId, Constant.Status.Status_stop));

        photoCacheMaps.clear();
      }
      else if (target.equals(Constant.Target.PHOTO)) { //Pushed image
        setCommonHeaders(httpResponse, HttpStatus.SC_OK);

        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Photo;
        if (!httpRequest.containsHeader("X-Apple-AssetAction")) {
          Log.d(tag, "airplay display image" + "; assetKey = " + httpRequest.getFirstHeader("X-Apple-AssetKey"));
          msg.obj = entityContent;
          MainApp.broadcastMessage(msg);
        }
        else {
          String assetAction = httpRequest.getFirstHeader("X-Apple-AssetAction").getValue();
          String assetKey = httpRequest.getFirstHeader("X-Apple-AssetKey").getValue();
          if ("cacheOnly".equals(assetAction)) {
            Log.d(tag, "airplay cached image, assetKey = " + assetKey);

            if (assetKey != null & entityContent != null) {
              if (!photoCacheMaps.containsKey(assetKey)) {
                photoCacheMaps.put(assetKey, entityContent);
              }
            }
          }
          else if ("displayCached".equals(assetAction)) {
            Log.d(tag, "airplay display cached image, assetKey = " + assetKey);
            if (photoCacheMaps.containsKey(assetKey)) {
              byte[] pic = photoCacheMaps.get(assetKey);
              if (pic != null) {
                msg.obj = pic;
                MainApp.broadcastMessage(msg);
              }
            }
            else {
              httpResponse.setStatusCode(HttpStatus.SC_PRECONDITION_FAILED);
            }

          }
        }
      }
      else if (target.startsWith(Constant.Target.SCRUB)) { //When querystring contains "position", perform seek operation. Otherwise, return the position and duration of the current video.
        StringEntity returnBody = new StringEntity("");

        String position = StringUtils.getQueryStringValue(target, "?position=");
        if (!TextUtils.isEmpty(position)) {
          //perform seek operation
          try {
            float pos = Float.parseFloat(position);
            Log.d(tag, "airplay seek position = " + pos); //unit is seconds

            Message msg = Message.obtain();
            msg.what = Constant.Msg.Msg_Video_Seek;
            msg.obj = pos;
            MainApp.broadcastMessage(msg);
          }
          catch (NumberFormatException e) {
            setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
            return;
          }
        }
        else {
          //return the duration and position of the current video
          long duration = 0;
          long curPos = 0;

          if (!PlaybackStatusMonitor.isPlaybackFinished()) {
            duration = PlaybackStatusMonitor.getDuration();
            curPos = PlaybackStatusMonitor.getCurrentPosition();
            duration = duration < 0 ? 0 : duration;
            curPos = curPos < 0 ? 0 : curPos;
            Log.d(tag, "airplay get method scrub: duration = " + duration + "; position = " + curPos);

            //Milliseconds need to be converted to seconds
            DecimalFormat decimalFormat = new DecimalFormat(".000000");
            String strDuration = decimalFormat.format(duration / 1000f);
            String strCurPos = decimalFormat.format(curPos / 1000f);

            //must have space, duration: **.******, or else, apple client can not syn with android
            String returnStr = "duration: " + strDuration + "\nposition: " + strCurPos;
            Log.d(tag, "airplay return scrub message = " + returnStr);
            returnBody = new StringEntity(returnStr, "UTF-8");
          }
          else {
            //After the interface for playing videos exits, the mobile terminal must also exit.
            httpContext.setAttribute(Constant.Need_sendReverse, Constant.Status.Status_stop);
            httpContext.setAttribute(Constant.ReverseMsg, Constant.getStopEventMsg(1, sessionId, Constant.Status.Status_stop));
          }
        }

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
        httpResponse.setEntity(returnBody);
      }
      else if (target.startsWith(Constant.Target.RATE)) { //Set playback rate (special case: 0 is pause)
        String value = StringUtils.getQueryStringValue(target, "?value=");
        if (!TextUtils.isEmpty(value)) {
          try {
            float rate = Float.parseFloat(value);
            Log.d(tag, "airplay rate = " + rate);

            Message msg = Message.obtain();
            msg.what = Constant.Msg.Msg_Video_Rate;
            msg.obj = rate;
            MainApp.broadcastMessage(msg);
          }
          catch (NumberFormatException e) {
            setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
            return;
          }
        }

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      //IOS 8.4.1 Never send this command (Youku does not send, Tencent video sends)
      else if (target.equalsIgnoreCase(Constant.Target.PLAYBACK_INFO)) {
        Log.d(tag, "airplay received playback_info request");
        String playback_info = "";
        long duration = 0;
        long cacheDuration = 0;
        long curPos = 0;

        String status = Constant.Status.Status_stop;

        if (PlaybackStatusMonitor.isPlaybackFinished()) {
          Log.d(tag, " airplay video activity is finished");
          status = Constant.Status.Status_stop;

          httpContext.setAttribute(Constant.Need_sendReverse, status);
          httpContext.setAttribute(Constant.ReverseMsg, Constant.getStopEventMsg(1, sessionId, status));
        }
        else {
          curPos = PlaybackStatusMonitor.getCurrentPosition();
          duration = PlaybackStatusMonitor.getDuration();
          cacheDuration = curPos;
          if (curPos == -1 || duration == -1 || cacheDuration == -1) {
            status = Constant.Status.Status_load;
            playback_info = Constant.getPlaybackInfo(0, 0, 0, 0);
          }
          else {
            status = Constant.Status.Status_play;
            playback_info = Constant.getPlaybackInfo(duration / 1000f, cacheDuration / 1000f, curPos / 1000f, 1);
          }

          httpContext.setAttribute(Constant.Need_sendReverse, status);
          httpContext.setAttribute(Constant.ReverseMsg, Constant.getVideoEventMsg(sessionId, status));
        }

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
        httpResponse.setHeader("Content-Type", "text/x-apple-plist+xml");
        httpResponse.setEntity(new StringEntity(playback_info));
      }
      else if (target.equals("/fp-setup")) {
        Log.d(tag, "airplay setup content = " + new String(entityContent, "UTF-8"));

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      // =======================================================================
      // non-standard extended API methods:
      // =======================================================================
      else if (
        (entityContent != null) && (
          target.equals(Constant.Target.PLAY)  ||  //Clear queue and add new video
          target.equals(Constant.Target.QUEUE)     //Add video to end of queue
        )
      ) {
        String playUrl   = null;
        String textUrl   = null;
        String referUrl  = null;
        String useCache  = null;
        String startPos  = null;
        String stopPos   = null;
        String drmScheme = null;
        String drmUrl    = null;

        HashMap<String, String> reqHeadersMap = null;
        HashMap<String, String> drmHeadersMap = null;

        if (contentType.equalsIgnoreCase("application/x-apple-binary-plist")) {
          //<BINARY PLIST DATA>
          HashMap map = BplistParser.parse(entityContent);

          playUrl  = (String) map.get("Content-Location");
          startPos = Double.toString(
                      (Double) map.get("Start-Position")
                     );
        }
        else {
          String requestBody;
          requestBody = new String(entityContent);
          requestBody = StringUtils.convertEscapedLinefeeds(requestBody); //Not necessary; courtesy to curl users.
          Log.d(tag, " airplay play action request content = " + requestBody);

          HashMap<String, ArrayList<String>> map = StringUtils.parseRequestBody_allowDuplicateKeys(requestBody, /* normalize_lowercase_keys= */ true);

          playUrl   = (String) StringUtils.getLastListItem((ArrayList<String>) map.get("content-location"));
          textUrl   = (String) StringUtils.getLastListItem((ArrayList<String>) map.get("caption-location"));
          referUrl  = (String) StringUtils.getLastListItem((ArrayList<String>) map.get("referer"));
          useCache  = (String) StringUtils.getLastListItem((ArrayList<String>) map.get("use-cache"));
          startPos  = (String) StringUtils.getLastListItem((ArrayList<String>) map.get("start-position"));
          stopPos   = (String) StringUtils.getLastListItem((ArrayList<String>) map.get("stop-position"));
          drmScheme = (String) StringUtils.getLastListItem((ArrayList<String>) map.get("drm-license-scheme"));
          drmUrl    = (String) StringUtils.getLastListItem((ArrayList<String>) map.get("drm-license-server"));

          reqHeadersMap = StringUtils.parseDuplicateKeyValues((ArrayList<String>) map.get("req-header"), /* normalize_lowercase_keys= */ true);
          drmHeadersMap = StringUtils.parseDuplicateKeyValues((ArrayList<String>) map.get("drm-header"), /* normalize_lowercase_keys= */ true);
        }

        if (TextUtils.isEmpty(playUrl)) {
          if (target.equals(Constant.Target.PLAY)) {
            // treat an empty PLAY request as equivalent to Constant.Target.PLAYER_SHOW

            Message msg = Message.obtain();
            msg.what = Constant.Msg.Msg_Show_Player;
            MainApp.broadcastMessage(msg);

            setCommonHeaders(httpResponse, HttpStatus.SC_OK);
          }
          else {
            Log.d(tag, "airplay video URL missing");

            setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
          }
        }
        else {
          Log.d(tag, "Media URL = " + playUrl + "; Start At = " + startPos + "; Stop At = " + stopPos + "; Captions = " + textUrl + "; Referer = " + referUrl + "; DRM Scheme = " + drmScheme + "; DRM License URL = " + drmUrl);

          HashMap<String, String> dataMap = new HashMap<String, String>();
          dataMap.put(Constant.PlayURL,    playUrl);
          dataMap.put(Constant.CaptionURL, textUrl);
          dataMap.put(Constant.RefererURL, referUrl);
          dataMap.put(Constant.UseCache,   useCache);
          dataMap.put(Constant.Start_Pos,  startPos);
          dataMap.put(Constant.Stop_Pos,   stopPos);
          dataMap.put(Constant.DRM_Scheme, drmScheme);
          dataMap.put(Constant.DRM_URL,    drmUrl);

          HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
          map.put(Constant.Video_Source_Map.DATA,        dataMap);
          map.put(Constant.Video_Source_Map.REQ_HEADERS, reqHeadersMap);
          map.put(Constant.Video_Source_Map.DRM_HEADERS, drmHeadersMap);

          Message msg = Message.obtain();
          msg.what = target.equals(Constant.Target.PLAY)
            ? Constant.Msg.Msg_Video_Play
            : Constant.Msg.Msg_Video_Queue;
          msg.obj = map;
          MainApp.broadcastMessage(msg);

          setCommonHeaders(httpResponse, HttpStatus.SC_OK);
        }
      }
      else if ((entityContent != null) && target.equals(Constant.Target.TXT_LOAD)) { //Load new text captions for current video in queue
        String requestBody;
        requestBody = new String(entityContent);
        requestBody = StringUtils.convertEscapedLinefeeds(requestBody); //Not necessary; courtesy to curl users.
        Log.d(tag, " airplay load caption request content = " + requestBody);

        HashMap<String, ArrayList<String>> map = StringUtils.parseRequestBody_allowDuplicateKeys(requestBody, /* normalize_lowercase_keys= */ true);
        String textUrl = (String) StringUtils.getLastListItem((ArrayList<String>) map.get("caption-location"));

        if (!TextUtils.isEmpty(textUrl)) {
          Message msg = Message.obtain();
          msg.what = Constant.Msg.Msg_Text_Load;
          msg.obj = textUrl;
          MainApp.broadcastMessage(msg);

          setCommonHeaders(httpResponse, HttpStatus.SC_OK);
        }
        else {
          Log.d(tag, "airplay caption URL missing");

          setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
        }
      }
      else if (target.startsWith(Constant.Target.SCRUB_OFFSET)) { //perform seek operation relative to position of current video
        String value = StringUtils.getQueryStringValue(target, "?value=");
        if (!TextUtils.isEmpty(value)) {
          try {
            long offset = Long.parseLong(value, 10);
            Log.d(tag, "airplay scrub offset = " + offset);

            Message msg = Message.obtain();
            msg.what = Constant.Msg.Msg_Video_Seek_Offset;
            msg.obj = offset;
            MainApp.broadcastMessage(msg);
          }
          catch (NumberFormatException e) {
            setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
            return;
          }
        }
        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (target.equals(Constant.Target.NEXT)) { //skip forward to next video in queue
        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Video_Next;
        MainApp.broadcastMessage(msg);

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (target.equals(Constant.Target.PREVIOUS)) { //skip backward to previous video in queue
        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Video_Prev;
        MainApp.broadcastMessage(msg);

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (target.startsWith(Constant.Target.VOLUME)) { //set audio volume (special case: 0 is mute)
        String value = StringUtils.getQueryStringValue(target, "?value=");
        if (!TextUtils.isEmpty(value)) {
          try {
            float audioVolume = Float.parseFloat(value);
            Log.d(tag, "airplay volume = " + audioVolume);

            Message msg = Message.obtain();
            msg.what = Constant.Msg.Msg_Audio_Volume;
            msg.obj = audioVolume;
            MainApp.broadcastMessage(msg);
          }
          catch (NumberFormatException e) {
            setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
            return;
          }
        }
        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (target.startsWith(Constant.Target.TXT_SHOW)) { //toggle text captions on/off
        String value = StringUtils.getQueryStringValue(target, "?toggle=");
        if (!TextUtils.isEmpty(value)) {
          try {
            int toggleValue = Integer.parseInt(value, 10);
            Log.d(tag, "airplay captions = " + toggleValue);

            Message msg = Message.obtain();
            msg.what = Constant.Msg.Msg_Text_Show;
            msg.obj = (toggleValue != 0); //boolean whether to "show"
            MainApp.broadcastMessage(msg);
          }
          catch (NumberFormatException e) {
            setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
            return;
          }
        }
        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (
        target.startsWith(Constant.Target.TXT_SET_OFFSET) ||
        target.startsWith(Constant.Target.TXT_ADD_OFFSET)
      ) { //update time offset for text captions
        String value = StringUtils.getQueryStringValue(target, "?value=");
        if (!TextUtils.isEmpty(value)) {
          try {
            long offset = Long.parseLong(value, 10);
            Log.d(tag, "airplay text offset = " + offset);

            Message msg = Message.obtain();
            msg.what = target.startsWith(Constant.Target.TXT_SET_OFFSET) ? Constant.Msg.Msg_Text_Set_Time : Constant.Msg.Msg_Text_Add_Time;
            msg.obj = offset;
            MainApp.broadcastMessage(msg);
          }
          catch (NumberFormatException e) {
            setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
            return;
          }
        }
        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (
        (entityContent != null) &&
        target.equals(Constant.Target.TOAST_SHOW)
      ) {
        String requestBody;
        requestBody = new String(entityContent);
        requestBody = requestBody.trim();
        requestBody = StringUtils.convertEscapedLinefeeds(requestBody); //Not necessary; courtesy to curl users.

        if (!TextUtils.isEmpty(requestBody)) {
          Message msg = Message.obtain();
          msg.what = Constant.Msg.Msg_Show_Toast;
          msg.obj = requestBody;
          MainApp.broadcastMessage(msg);

          setCommonHeaders(httpResponse, HttpStatus.SC_OK);
        }
        else {
          Log.d(tag, "airplay Toast message missing");

          setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
        }
      }
      else if (target.equals(Constant.Target.PLAYER_SHOW)) {
        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Show_Player;
        MainApp.broadcastMessage(msg);

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (target.equals(Constant.Target.PLAYER_HIDE)) {
        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Hide_Player;
        MainApp.broadcastMessage(msg);

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (
        (entityContent != null) &&
        target.equals(Constant.Target.ACTIVITY_START)
      ) {
        String requestBody;
        requestBody = new String(entityContent);
        requestBody = StringUtils.convertEscapedLinefeeds(requestBody); //Not necessary; courtesy to curl users.

        HashMap<String, ArrayList<String>> map = StringUtils.parseRequestBody_allowDuplicateKeys(requestBody, /* normalize_lowercase_keys= */ false);

        if (
          /* explicit */ (map.containsKey("package") && map.containsKey("class")) ||
          /* implicit */  map.containsKey("action")
        ) {
          Message msg = Message.obtain();
          msg.what = Constant.Msg.Msg_Start_Activity;
          msg.obj = map;
          MainApp.broadcastMessage(msg);

          setCommonHeaders(httpResponse, HttpStatus.SC_OK);
        }
        else {
          Log.d(tag, "airplay Intent parameters missing");

          setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
        }
      }
      else if (target.equals(Constant.Target.VIDEO_SHARE)) {
        HashMap<String, String> map;

        if (entityContent != null) {
          String requestBody;
          requestBody = new String(entityContent);
          requestBody = StringUtils.convertEscapedLinefeeds(requestBody); //Not necessary; courtesy to curl users.

          map = StringUtils.parseRequestBody(requestBody, /* normalize_lowercase_keys= */ true);
        }
        else {
          map = new HashMap<String, String>();
        }

        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Video_Share;
        msg.obj = map;
        MainApp.broadcastMessage(msg);

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (target.equals(Constant.Target.CACHE_DELETE)) {
        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Delete_Cache;
        MainApp.broadcastMessage(msg);

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }
      else if (target.equals(Constant.Target.SERVICE_EXIT)) {
        Message msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Exit_Service;
        MainApp.broadcastMessage(msg);

        setCommonHeaders(httpResponse, HttpStatus.SC_OK);
      }

      // =======================================================================
      else {
        Log.d(tag, "airplay default not process");

        setCommonHeaders(httpResponse, HttpStatus.SC_BAD_REQUEST);
      }
    }

    private static void setCommonHeaders(HttpResponse httpResponse, int statusCode) {
      httpResponse.setStatusCode(statusCode);
      httpResponse.setHeader("Access-Control-Allow-Origin", "*");
      httpResponse.setHeader("Date", new Date().toString());
    }
  }
}
