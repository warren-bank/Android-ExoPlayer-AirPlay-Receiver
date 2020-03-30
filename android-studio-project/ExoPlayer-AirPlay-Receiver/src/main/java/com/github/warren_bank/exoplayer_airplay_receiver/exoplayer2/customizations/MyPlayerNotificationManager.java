package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations;

import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.PlayerManager;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.VideoSource;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter;
import com.google.android.exoplayer2.util.NotificationUtil;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import android.content.Context;
import android.graphics.Bitmap;

public class MyPlayerNotificationManager extends PlayerNotificationManager {
  private PlayerManager playerManager;

  // ===========================================================================
  // https://github.com/google/ExoPlayer/blob/r2.11.3/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L989
  // https://github.com/google/ExoPlayer/blob/r2.11.3/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L1037
  // ===========================================================================
  // * startOrUpdateNotification(...)
  //   - calls createNotification(...)
  //   - hides the Notification when the return value is null
  // * createNotification(...)
  //   - returns null due to player.stop() having previously been called
  // * PlayerManager never calls player.stop()
  //   - this subclass provides a workaround
  //   - overrides createNotification(...) to return null under different conditions
  // ===========================================================================

  @Override
  protected NotificationCompat.Builder createNotification(
    Player player,
    @Nullable NotificationCompat.Builder builder,
    boolean ongoing,
    @Nullable Bitmap largeIcon
  ) {
    int currentItemIndex = player.getCurrentWindowIndex();
    VideoSource sample   = playerManager.getItem(currentItemIndex);

    return (sample == null)
      ? null
      : super.createNotification(player, builder, ongoing, largeIcon);
  }

  // ===========================================================================
  // https://github.com/google/ExoPlayer/blob/r2.11.3/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L447
  // https://github.com/google/ExoPlayer/blob/r2.11.3/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L524
  // ===========================================================================
  // * createWithNotificationChannel(...)
  //   - static method that performs a task before calling the class constructor
  // ===========================================================================

  public static MyPlayerNotificationManager createWithNotificationChannel(
    PlayerManager playerManager,
    Context context,
    String channelId,
    @StringRes int channelName,
    @StringRes int channelDescription,
    int notificationId,
    MediaDescriptionAdapter mediaDescriptionAdapter
  ) {
    NotificationUtil.createNotificationChannel(context, channelId, channelName, channelDescription, NotificationUtil.IMPORTANCE_LOW);

    return new MyPlayerNotificationManager(playerManager, context, channelId, notificationId, mediaDescriptionAdapter);
  }

  public MyPlayerNotificationManager(
    PlayerManager playerManager,
    Context context,
    String channelId,
    int notificationId,
    MediaDescriptionAdapter mediaDescriptionAdapter
  ) {
    super(
      context,
      channelId,
      notificationId,
      mediaDescriptionAdapter,
      /* notificationListener= */ null,
      /* customActionReceiver= */ null
    );

    this.playerManager = playerManager;
  }

}
