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

  public void setPlayerManager(PlayerManager pm) {
    playerManager = pm;
  }

  // ===========================================================================
  // https://github.com/google/ExoPlayer/blob/r2.15.1/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L1171
  // https://github.com/google/ExoPlayer/blob/r2.15.1/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L1215
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
    VideoSource sample   = (playerManager == null) ? null : playerManager.getItem(currentItemIndex);

    return (sample == null)
      ? null
      : super.createNotification(player, builder, ongoing, largeIcon);
  }

  // ===========================================================================
  // https://github.com/google/ExoPlayer/blob/r2.15.1/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L308
  // https://github.com/google/ExoPlayer/blob/r2.15.1/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L351
  // https://github.com/google/ExoPlayer/blob/r2.15.1/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L557
  // ===========================================================================
  // * class Builder{...}
  // ===========================================================================

  public static class Builder extends PlayerNotificationManager.Builder {
    public Builder(Context context, int notificationId, String channelId) {
      super(context, notificationId, channelId);
    }

    @Override
    public MyPlayerNotificationManager build() {
      if (channelNameResourceId != 0) {
        NotificationUtil.createNotificationChannel(
          context,
          channelId,
          channelNameResourceId,
          channelDescriptionResourceId,
          channelImportance);
      }

      return new MyPlayerNotificationManager(
        context,
        channelId,
        notificationId,
        mediaDescriptionAdapter,
        notificationListener,
        customActionReceiver,
        smallIconResourceId,
        playActionIconResourceId,
        pauseActionIconResourceId,
        stopActionIconResourceId,
        rewindActionIconResourceId,
        fastForwardActionIconResourceId,
        previousActionIconResourceId,
        nextActionIconResourceId,
        groupKey
      );
    }
  }

  // ===========================================================================
  // https://github.com/google/ExoPlayer/blob/r2.15.1/library/ui/src/main/java/com/google/android/exoplayer2/ui/PlayerNotificationManager.java#L710
  // ===========================================================================
  // * PlayerNotificationManager(...)
  //   - constructor
  // ===========================================================================

  protected MyPlayerNotificationManager(
    Context context,
    String channelId,
    int notificationId,
    MediaDescriptionAdapter mediaDescriptionAdapter,
    @Nullable NotificationListener notificationListener,
    @Nullable CustomActionReceiver customActionReceiver,
    int smallIconResourceId,
    int playActionIconResourceId,
    int pauseActionIconResourceId,
    int stopActionIconResourceId,
    int rewindActionIconResourceId,
    int fastForwardActionIconResourceId,
    int previousActionIconResourceId,
    int nextActionIconResourceId,
    @Nullable String groupKey
  ) {
    super(
      context,
      channelId,
      notificationId,
      mediaDescriptionAdapter,
      notificationListener,
      customActionReceiver,
      smallIconResourceId,
      playActionIconResourceId,
      pauseActionIconResourceId,
      stopActionIconResourceId,
      rewindActionIconResourceId,
      fastForwardActionIconResourceId,
      previousActionIconResourceId,
      nextActionIconResourceId,
      groupKey
    );
  }

  // ===========================================================================
  // convenience method
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
    MyPlayerNotificationManager.Builder builder = new MyPlayerNotificationManager.Builder(
      context,
      notificationId,
      channelId
    );

    builder
      .setChannelNameResourceId(channelName)
      .setChannelDescriptionResourceId(channelDescription)
      .setMediaDescriptionAdapter(mediaDescriptionAdapter)
    ;

    MyPlayerNotificationManager myPlayerNotificationManager = builder.build();
    myPlayerNotificationManager.setPlayerManager(playerManager);

    return myPlayerNotificationManager;
  }

}
