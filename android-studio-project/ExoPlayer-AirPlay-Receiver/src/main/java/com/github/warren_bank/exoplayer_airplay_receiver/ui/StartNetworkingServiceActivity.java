package com.github.warren_bank.exoplayer_airplay_receiver.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.service.NetworkingService;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.NetworkUtils;

import java.io.File;

public class StartNetworkingServiceActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!NetworkUtils.isWifiConnected(MainApp.getInstance())) {
      finish();
      return;
    }

    startNetworkingService();
    finish();
  }

  private void startNetworkingService() {
    Intent intent = new Intent(getApplicationContext(), NetworkingService.class);
    forwardMedia(intent);
    MainApp.getInstance().startService(intent);
    finish();
  }

  private void forwardMedia(Intent newIntent) {
    Intent oldIntent = getIntent();
    String action    = oldIntent.getAction();
    Uri data         = null;
    String type;

    if (action == null) return;

    try {
      switch(action) {
        case Intent.ACTION_VIEW : {
          data = oldIntent.getData();
          type = oldIntent.getType();

          if (type != null) {
            type = type.toLowerCase();

            switch(type) {
              case "application/octet-stream" :
              case "resource/folder" : {
                data = filterDirectory(data);
                break;
              }
            }
          }
          break;
        }
        case Intent.ACTION_SEND : {
          data = (Uri) oldIntent.getParcelableExtra(Intent.EXTRA_STREAM);
          data = filterDirectory(data);
          break;
        }
      }
    }
    catch(Exception e) {
      data = null;
    }

    if (data != null) {
      newIntent.setAction(NetworkingService.ACTION_PLAY);
      newIntent.replaceExtras(oldIntent);
      newIntent.putExtra(Constant.PlayURL, data.toString());
    }
  }

  private Uri filterDirectory(Uri data) {
    Uri filtered = data;
    String scheme, path, uri;
    File file;

    try {
      if (data == null)
        throw new Exception();

      scheme = data.getScheme();
      if (scheme == null)
        throw new Exception();
      if (!scheme.toLowerCase().equals("file"))
        throw new Exception();

      path = data.getPath();
      file = new File(path);
      if (!file.isDirectory())
        throw new Exception();

      // normalize uri, such that directory path always ends with '/' separator
      uri = data.toString();
      if (uri.charAt(uri.length() - 1) != '/') {
        uri += "/";
        filtered = Uri.parse(uri);
      }
    }
    catch (Exception e) {
      filtered = null;
    }

    return filtered;
  }
}
