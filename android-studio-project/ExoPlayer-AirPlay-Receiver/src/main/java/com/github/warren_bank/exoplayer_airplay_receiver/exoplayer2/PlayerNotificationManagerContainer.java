package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.MyPlayerNotificationManager;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.MediaTypeUtils;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.ResourceUtils;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback;
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter;
import com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import java.net.URI;

public class PlayerNotificationManagerContainer implements SetPlayer {
  private Context context;
  private PlayerManager playerManager;
  private Class<?> pendingIntentActivityClass;

  private MyPlayerNotificationManager playerNotificationManager;
  private MediaSessionCompat mediaSession;
  private MediaSessionConnector mediaSessionConnector;

  public PlayerNotificationManagerContainer(Context context, PlayerManager playerManager, @Nullable Class<?> pendingIntentActivityClass) {
    this.context                    = context;
    this.playerManager              = playerManager;
    this.pendingIntentActivityClass = pendingIntentActivityClass;

    playerNotificationManager = MyPlayerNotificationManager.createWithNotificationChannel(
      playerManager,
      context,
      /* channelId=               */ "playback_channel",
      /* channelName=             */ R.string.app_name,
      /* channelDescription=      */ 0,
      /* notificationId=          */ ResourceUtils.getInteger(context, R.integer.NOTIFICATION_ID_EXOPLAYER_CONTROLS),
      /* mediaDescriptionAdapter= */ new MediaDescriptionAdapter()
      {
        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player){
          if (pendingIntentActivityClass == null) return null;

          Intent intent = new Intent(context, pendingIntentActivityClass);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

          PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
          return pendingIntent;
        }

        @Override
        public String getCurrentContentTitle(Player player) {
          int currentItemIndex = player.getCurrentWindowIndex();
          return getMediaItemTitle(currentItemIndex);
        }

        @Nullable
        @Override
        public String getCurrentContentText(Player player) {
          int currentItemIndex = player.getCurrentWindowIndex();
          return getMediaItemDescription(currentItemIndex);
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, BitmapCallback callback) {
          int currentItemIndex = player.getCurrentWindowIndex();
          return getMediaItemBitmap(currentItemIndex);
        }
      }
    );

    ComponentName mbrComponent = new ComponentName(context, MediaButtonReceiver.class);
    mediaSession = new MediaSessionCompat(context, /* tag= */ context.getPackageName(), mbrComponent, /* mbrIntent= */ (PendingIntent) null);
    mediaSession.setActive(true);
    playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());

    mediaSessionConnector = new MediaSessionConnector(mediaSession);
    mediaSessionConnector.setQueueNavigator(new TimelineQueueNavigator(mediaSession) {
      @Override
      public MediaDescriptionCompat getMediaDescription(Player player, int windowIndex) {
        int currentItemIndex = player.getCurrentWindowIndex();
        return getMediaDesc(currentItemIndex);
      }
    });

    // triggers a callback that passes the singleton instance of SimpleExoPlayer
    playerManager.setPlayer((SetPlayer) this);
  }

  @Override
  public void setPlayer(@Nullable Player player) {
    playerNotificationManager.setPlayer(player);
    mediaSessionConnector.setPlayer(player);
  }

  // ===========================================================================
  // public API
  // ===========================================================================

  public PlayerNotificationManager getPlayerNotificationManager() {
    return playerNotificationManager;
  }

  public void release() {
    setPlayer(null);
    mediaSession.release();
  }

  // ===========================================================================
  // internal
  // ===========================================================================

  private URI getMediaItemUri(int currentItemIndex) {
    VideoSource sample = playerManager.getItem(currentItemIndex);
    if (sample == null) return null;

    try {
      // "sample.uri" is always encoded; no need to re-encode with: "UriUtils.parseURI(sample.uri)"
      URI uri = new URI(sample.uri);
      return uri;
    }
    catch(Exception e) {
      return null;
    }
  }

  // ===========================================================================

  private String getMediaItemTitle(int currentItemIndex) {
    return getMediaItemTitle(currentItemIndex, getMediaItemUri(currentItemIndex));
  }

  private String getMediaItemTitle(int currentItemIndex, URI uri) {
    if (uri == null) return null;

    boolean isAudio = MediaTypeUtils.isAudioFileUrl(uri.toString());
    return getMediaItemTitle(currentItemIndex, uri, isAudio);
  }

  private String getMediaItemTitle(int currentItemIndex, URI uri, boolean isAudio) {
    if (uri == null) return null;

    int position_current, position_last;
    String queuePosition, mimeType, title;

    position_current = currentItemIndex + 1;
    position_last    = playerManager.getMediaQueueSize();
    queuePosition    = String.format("[%d/%d]", position_current, position_last);

    mimeType = isAudio
      ? MediaTypeUtils.get_audio_mimeType(uri.toString())
      : MediaTypeUtils.get_video_mimeType(uri.toString());

    title = queuePosition + " " + mimeType;
    return title;
  }

  // ===========================================================================

  private String getMediaItemDescription(int currentItemIndex) {
    return getMediaItemDescription(getMediaItemUri(currentItemIndex));
  }

  private String getMediaItemDescription(URI uri) {
    if (uri == null) return null;

    boolean isAudio = MediaTypeUtils.isAudioFileUrl(uri.toString());
    return getMediaItemDescription(uri, isAudio);
  }

  private String getMediaItemDescription(URI uri, boolean isAudio) {
    if (uri == null) return null;

    boolean isStream;
    String video_mimeType;

    if (isAudio) {
      isStream = false;
    }
    else {
      video_mimeType = MediaTypeUtils.get_video_mimeType(uri.toString());

      switch (video_mimeType) {
        case "application/x-mpegURL" :
        case "application/dash+xml" :
        case "application/vnd.ms-sstr+xml" :
          isStream = true;
          break;
        default:
          isStream = false;
          break;
      }
    }

    return isStream
      ? uri.getHost()
      : getMediaItemFilename(uri);
  }

  private String getMediaItemFilename(URI uri) {
    return (uri == null)
      ? null
      : getMediaItemFilename(uri.getPath());
  }

  // ===================================
  // examples:
  // ===================================
  // * in  = /path/to/directory/file
  //   out = /directory/file
  // * in  = /directory/file
  //   out = /directory/file
  // * in  = /file
  //   out = /file
  // * in  = file
  //   out = file
  // ===================================
  private String getMediaItemFilename(String path) {
    if (path == null) return null;

    int index_filename, index_dirname;

    index_filename = path.lastIndexOf('/');
    if (index_filename == -1)
      return path;

    index_dirname = path.lastIndexOf('/', (index_filename - 1));
    if (index_dirname == -1)
      return path;

    return path.substring(index_dirname);
  }

  // ===========================================================================

  private Bitmap getMediaItemBitmap(int currentItemIndex) {
    return getMediaItemBitmap(getMediaItemUri(currentItemIndex));
  }

  private Bitmap getMediaItemBitmap(URI uri) {
    if (uri == null) return null;

    boolean isAudio = MediaTypeUtils.isAudioFileUrl(uri.toString());
    return getMediaItemBitmap(uri, isAudio);
  }

  private Bitmap getMediaItemBitmap(URI uri, boolean isAudio) {
    if (uri == null) return null;

    int id = isAudio
      ? R.drawable.exoplayer_notification_icon_audio     // https://material.io/resources/icons/?icon=audiotrack
      : R.drawable.exoplayer_notification_icon_video;    // https://material.io/resources/icons/?icon=ondemand_video

    return ResourceUtils.getBitmap(context, id);
  }

  // ===========================================================================

  private MediaDescriptionCompat getMediaDesc(int currentItemIndex) {
    URI uri = getMediaItemUri(currentItemIndex);
    if (uri == null) return null;

    boolean isAudio = MediaTypeUtils.isAudioFileUrl(uri.toString());

    String mediaId     = uri.toString();                                    // URL
    String title       = getMediaItemTitle(currentItemIndex, uri, isAudio); // [index/total] mime-type
    String description = getMediaItemDescription(uri, isAudio);             // hostname (stream) or dirname/filename (non-stream)
    Bitmap bitmap      = getMediaItemBitmap(uri, isAudio);                  // material icon to indicate audio or video

    Bundle extras = new Bundle();
    extras.putParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,    bitmap);
    extras.putParcelable(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap);

    return new MediaDescriptionCompat.Builder()
      .setMediaId(mediaId)
      .setTitle(title)
      .setDescription(description)
      .setIconBitmap(bitmap)
      .setExtras(extras)
      .build();
  }

}
