package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

/*
 * based on:
 *   https://github.com/androidx/media/blob/1.2.0/demos/main/src/main/java/androidx/media3/demo/main/DemoDownloadService.java
 */

import static com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.ExoPlayerUtils.DOWNLOAD_NOTIFICATION_CHANNEL_ID;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.ExoPlayerUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ResourceUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.NotificationUtil;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadNotificationHelper;
import androidx.media3.exoplayer.offline.DownloadService;
import androidx.media3.exoplayer.scheduler.PlatformScheduler;
import androidx.media3.exoplayer.scheduler.Requirements;
import androidx.media3.exoplayer.scheduler.Scheduler;

import android.app.Notification;
import android.content.Context;

import java.util.List;

/** A service for downloading media. */
public class MyDownloadService extends DownloadService {

  public MyDownloadService() {
    super(
        ResourceUtils.getInteger(R.integer.NOTIFICATION_ID_EXOPLAYER_DOWNLOADS),
        DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
        DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        R.string.exo_download_notification_channel_name,  // https://github.com/androidx/media/blob/1.2.0/libraries/exoplayer/src/main/res/values/strings.xml#L20
        /* channelDescriptionResourceId= */ 0
    );
  }

  @Override
  @NonNull
  protected DownloadManager getDownloadManager() {
    // This will only happen once, because getDownloadManager is guaranteed to be called only once in the life cycle of the process.
    DownloadManager downloadManager = ExoPlayerUtils.getDownloadManager(/* context= */ this);
    DownloadNotificationHelper downloadNotificationHelper = ExoPlayerUtils.getDownloadNotificationHelper(/* context= */ this);
    downloadManager.addListener(
        new TerminalStateNotificationHelper(
          this,
          downloadNotificationHelper,
          ResourceUtils.getInteger(/* context= */ this, R.integer.NOTIFICATION_ID_EXOPLAYER_DOWNLOADS) + 1
        )
    );
    return downloadManager;
  }

  @Override
  protected Scheduler getScheduler() {
    return Util.SDK_INT >= 21
        ? new PlatformScheduler(this, ResourceUtils.getInteger(/* context= */ this, R.integer.PLATFORM_SCHEDULER_JOB_ID_EXOPLAYER_DOWNLOAD_SERVICE))
        : null;
  }

  @Override
  @NonNull
  protected Notification getForegroundNotification(List<Download> downloads, @Requirements.RequirementFlags int notMetRequirements) {
    return ExoPlayerUtils.getDownloadNotificationHelper(/* context= */ this)
        .buildProgressNotification(
            /* context= */ this,
            R.drawable.exoplayer_notification_icon_download_active,
            /* contentIntent= */ null,
            /* message= */ null,
            downloads,
            notMetRequirements
        );
  }

  // --------------------------------------------------------------------------- class: TerminalStateNotificationHelper

  /**
   * Creates and displays notifications for downloads when they complete or fail.
   *
   * <p>This helper will outlive the lifespan of a single instance of {@link MyDownloadService}.
   * It is static to avoid leaking the first {@link MyDownloadService} instance.
   */
  private static final class TerminalStateNotificationHelper implements DownloadManager.Listener {

    private final Context context;
    private final DownloadNotificationHelper notificationHelper;

    private int nextNotificationId;

    public TerminalStateNotificationHelper(Context context, DownloadNotificationHelper notificationHelper, int firstNotificationId) {
      this.context = context.getApplicationContext();
      this.notificationHelper = notificationHelper;
      nextNotificationId = firstNotificationId;
    }

    // DownloadManager.Listener implementation.

    @Override
    public void onDownloadChanged(DownloadManager downloadManager, Download download, @Nullable Exception finalException) {
      Notification notification;
      if (download.state == Download.STATE_COMPLETED) {
        notification = notificationHelper.buildDownloadCompletedNotification(
            context,
            R.drawable.exoplayer_notification_icon_download_complete,
            /* contentIntent= */ null,
            Util.fromUtf8Bytes(download.request.data)
        );
      }
      else if (download.state == Download.STATE_FAILED) {
        notification = notificationHelper.buildDownloadFailedNotification(
            context,
            R.drawable.exoplayer_notification_icon_download_complete,
            /* contentIntent= */ null,
            Util.fromUtf8Bytes(download.request.data)
        );
      }
      else {
        return;
      }

      NotificationUtil.setNotification(context, nextNotificationId, notification);
      nextNotificationId++;
    }

  }

  // ---------------------------------------------------------------------------

}
