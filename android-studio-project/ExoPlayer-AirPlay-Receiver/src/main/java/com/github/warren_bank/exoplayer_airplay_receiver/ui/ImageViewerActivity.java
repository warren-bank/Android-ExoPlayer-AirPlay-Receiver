package com.github.warren_bank.exoplayer_airplay_receiver.ui;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.MainApp;
import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.RequestListenerThread;

public class ImageViewerActivity extends Activity {
  private static final String tag = ImageViewerActivity.class.getSimpleName();
  private ImageView iv;
  private ImageHandler handler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_image);

    handler = new ImageHandler(ImageViewerActivity.this);
    MainApp.registerHandler(ImageViewerActivity.class.getName(), handler);

    initView();
  }

  private void initView() {
    iv = (ImageView) findViewById(R.id.image_view);
    Intent intent = getIntent();
    if (intent != null) {
      byte[] pic = intent.getByteArrayExtra("picture");
      this.showImage(pic);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent != null) {
      byte[] pic = intent.getByteArrayExtra("picture");
      this.showImage(pic);
    }
  }

  private void showImage(byte[] pic) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferQualityOverSpeed = true; //Improve picture quality

    int size = (options.outWidth * options.outHeight);
    int size_limit = 1920 * 1080 * 4;
    if (size > 1920 * 1080 * 4) {
      int zoomRate = (int) Math.ceil(size * 1.0 / size_limit);
      if (zoomRate <= 0)
        zoomRate = 1;
      options.inSampleSize = zoomRate;
    }

    if (!Thread.currentThread().isInterrupted()) {
      options.inJustDecodeBounds = false;
      Bitmap bitmap = BitmapFactory.decodeByteArray(pic, 0, pic.length, options);
      iv.setImageBitmap(bitmap);
    }
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(tag, "airplay ImageViewerActivity onDestroy");
    RequestListenerThread.photoCacheMaps.clear();
    MainApp.unregisterHandler(ImageViewerActivity.class.getName());
  }

  private static class ImageHandler extends Handler {
    private WeakReference<ImageViewerActivity> imageViewerActivityWeakReference;

    public ImageHandler(ImageViewerActivity activity) {
      imageViewerActivityWeakReference = new WeakReference<ImageViewerActivity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
      final ImageViewerActivity activity = this.imageViewerActivityWeakReference.get();

      if (activity == null)
        return;
      if (activity.isFinishing())
        return;

      switch (msg.what) {
        case Constant.Msg.Msg_Stop :
          activity.finish();
          break;
        case Constant.Msg.Msg_Video_Play :
          activity.finish();
          break;
      }
    }
  }
}
