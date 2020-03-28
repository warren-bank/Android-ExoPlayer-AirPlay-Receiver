package com.github.warren_bank.exoplayer_airplay_receiver.ui;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;

public class RuntimePermissionsRequestActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ExternalStorageUtils.request_permission(this);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (ExternalStorageUtils.is_permission_granted(this, requestCode, grantResults)) {
      Message msg = Message.obtain();
      msg.what = Constant.Msg.Msg_Runtime_Permissions_Granted;
      MainApp.broadcastMessage(msg);
    }

    finish();
  }

}
