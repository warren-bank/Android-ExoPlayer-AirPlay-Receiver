package com.github.warren_bank.exoplayer_airplay_receiver.ui;

import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.RuntimePermissionUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import java.util.ArrayList;

public class RuntimePermissionsRequestActivity extends Activity implements RuntimePermissionUtils.RuntimePermissionListener {

  private ArrayList<Integer> grantedCodes;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    grantedCodes = new ArrayList<Integer>();

    processIntent(getIntent());
  }

  @Override
  public void onNewIntent (Intent intent) {
    super.onNewIntent(intent);

    processIntent(intent);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    int requestCode;
    Message msg;

    if (!grantedCodes.isEmpty()) {
      for (Integer grantedCode : grantedCodes) {
        requestCode = grantedCode.intValue();

        msg = Message.obtain();
        msg.what = Constant.Msg.Msg_Runtime_Permissions_Granted;
        msg.obj  = requestCode;
        MainApp.broadcastMessage(msg);
      }
      grantedCodes.clear();
    }
  }

  private void processIntent(Intent intent) {
    int requestCode = intent.getIntExtra(Constant.Extra.PERMISSION_REQUEST_CODE, -1);

    if (requestCode >= 0)
      processRequestCode(requestCode);
    else
      finish();
  }

  private void processRequestCode(int requestCode) {
    switch(requestCode) {
      case Constant.PermissionRequestCode.MANAGE_EXTERNAL_STORAGE : {
        if (RuntimePermissionUtils.hasFilePermissions())
          finish();
        else
          RuntimePermissionUtils.showFilePermissions(RuntimePermissionsRequestActivity.this, requestCode);
        break;
      }
      case Constant.PermissionRequestCode.DRAW_OVERLAY : {
        if (RuntimePermissionUtils.hasDrawOverlayPermissions(RuntimePermissionsRequestActivity.this))
          finish();
        else
          RuntimePermissionUtils.showDrawOverlayPermissions(RuntimePermissionsRequestActivity.this, requestCode);
        break;
      }
      default : {
        RuntimePermissionUtils.requestPermissions(RuntimePermissionsRequestActivity.this, RuntimePermissionsRequestActivity.this, requestCode);
        break;
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    RuntimePermissionUtils.onRequestPermissionsResult(RuntimePermissionsRequestActivity.this, requestCode, permissions, grantResults);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    RuntimePermissionUtils.onActivityResult(RuntimePermissionsRequestActivity.this, requestCode, resultCode, data);
  }

  // ---------------------------------------------------------------------------
  // implementation: RuntimePermissionUtils.RuntimePermissionListener

  @Override
  public void onRequestPermissionsGranted(int requestCode, Object passthrough) {
    grantedCodes.add(Integer.valueOf(requestCode));

    if (requestCode == Constant.PermissionRequestCode.READ_EXTERNAL_STORAGE)
      processRequestCode(Constant.PermissionRequestCode.MANAGE_EXTERNAL_STORAGE);
    else
      finish();
  }

  @Override
  public void onRequestPermissionsDenied(int requestCode, Object passthrough, String[] missingPermissions) {
    finish();
  }

  @Override
  public void onAllRequestsCompleted(Exception exception, Object passthrough) {
  }

}
