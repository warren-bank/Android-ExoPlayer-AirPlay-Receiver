package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

/*
 * based on:
 *   https://github.com/google/ExoPlayer/blob/r2.15.1/demos/main/src/main/java/com/google/android/exoplayer2/demo/DownloadTracker.java
 */

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.MyDownloadService;

import static com.google.android.exoplayer2.util.Assertions.checkNotNull;
import static com.google.android.exoplayer2.util.Assertions.checkStateNotNull;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionEventListener;
import com.google.android.exoplayer2.drm.OfflineLicenseHelper;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadCursor;
import com.google.android.exoplayer2.offline.DownloadHelper;
import com.google.android.exoplayer2.offline.DownloadHelper.LiveContentUnsupportedException;
import com.google.android.exoplayer2.offline.DownloadIndex;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/** Tracks media that has been downloaded. */
public class DownloadTracker {

  /** Listens for changes in the tracked downloads. */
  public interface Listener {

    /** Called when the tracked downloads changed. */
    void onDownloadsChanged();
  }

  public interface AllDownloadsRemovedCallback {
    void onAllDownloadsRemoved();
  }

  private static final String TAG = "DownloadTracker";

  private final Context context;
  private final HttpDataSource.Factory httpDataSourceFactory;
  private final DownloadManager downloadManager;
  private final CopyOnWriteArraySet<Listener> listeners;
  private final HashMap<Uri, Download> downloads;
  private final DownloadIndex downloadIndex;

  // --------------------------------------------------------------------------- public API:

  public DownloadTracker(
      Context context,
      HttpDataSource.Factory httpDataSourceFactory,
      DownloadManager downloadManager
  ) {
    this.context = context.getApplicationContext();
    this.httpDataSourceFactory = httpDataSourceFactory;
    this.downloadManager = downloadManager;
    listeners = new CopyOnWriteArraySet<>();
    downloads = new HashMap<>();
    downloadIndex = downloadManager.getDownloadIndex();
    downloadManager.addListener(new DownloadManagerListener());
    loadDownloads();
  }

  public void addListener(Listener listener) {
    checkNotNull(listener);
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  public void addAllDownloadsRemovedCallback(AllDownloadsRemovedCallback callback) {
    checkNotNull(callback);
    if (downloadManager.isIdle()) {
      callback.onAllDownloadsRemoved();
    }
    else {
      downloadManager.addListener(new DownloadManager.Listener() {
        @Override
        public void onIdle(DownloadManager dm) {
          downloadManager.removeListener(this);
          callback.onAllDownloadsRemoved();
        }
      });
    }
  }

  public boolean isDownloaded(MediaItem mediaItem) {
    Uri uri = checkNotNull(mediaItem.playbackProperties).uri;
    return isDownloaded(uri);
  }

  public boolean isDownloaded(String uri) {
    try {
      return isDownloaded(Uri.parse(uri));
    }
    catch(Exception e) {
      return false;
    }
  }

  public boolean isDownloaded(Uri uri) {
    @Nullable Download download = downloads.get(uri);
    return download != null && download.state != Download.STATE_FAILED;
  }

  @Nullable
  public DownloadRequest getDownloadRequest(Uri uri) {
    @Nullable Download download = downloads.get(uri);
    return download != null && download.state != Download.STATE_FAILED ? download.request : null;
  }

  public void startDownloadService() {
    // Starting the service in the foreground causes notification flicker if there is no scheduled action.
    // Starting it in the background throws an exception if the app is in the background too (e.g. if device screen is locked).
    try {
      DownloadService.start(context, MyDownloadService.class);
    }
    catch (IllegalStateException e) {
      DownloadService.startForeground(context, MyDownloadService.class);
    }
  }

  public void startDownload(MediaItem mediaItem, RenderersFactory renderersFactory) {
    @Nullable Download download = downloads.get(checkNotNull(mediaItem.playbackProperties).uri);
    startDownload(download, mediaItem, renderersFactory);
  }

  public void startDownload(Download download, MediaItem mediaItem, RenderersFactory renderersFactory) {
    if (download == null || download.state == Download.STATE_FAILED) {
      new StartDownloadHelper(
          DownloadHelper.forMediaItem(context, mediaItem, renderersFactory, httpDataSourceFactory),
          mediaItem
      );
    }
  }

  public void stopDownload(MediaItem mediaItem) {
    @Nullable Download download = downloads.get(checkNotNull(mediaItem.playbackProperties).uri);
    stopDownload(download);
  }

  public void stopDownload(Download download) {
    if (download != null && download.state != Download.STATE_FAILED) {
      DownloadService.sendRemoveDownload(context, MyDownloadService.class, download.request.id, /* foreground= */ false);
    }
  }

  public void toggleDownload(MediaItem mediaItem, RenderersFactory renderersFactory) {
    @Nullable Download download = downloads.get(checkNotNull(mediaItem.playbackProperties).uri);
    if (download != null && download.state != Download.STATE_FAILED) {
      stopDownload(download);
    }
    else {
      startDownload(download, mediaItem, renderersFactory);
    }
  }

  public void removeAllDownloads() {
    DownloadService.sendRemoveAllDownloads(context, MyDownloadService.class, /* foreground= */ false);
  }

  // --------------------------------------------------------------------------- internal:

  private void loadDownloads() {
    try (DownloadCursor loadedDownloads = downloadIndex.getDownloads()) {
      while (loadedDownloads.moveToNext()) {
        Download download = loadedDownloads.getDownload();
        downloads.put(download.request.uri, download);
      }
    }
    catch (IOException e) {
      Log.w(TAG, "Failed to query downloads", e);
    }
  }

  // --------------------------------------------------------------------------- class: DownloadManagerListener

  private class DownloadManagerListener implements DownloadManager.Listener {

    @Override
    public void onDownloadChanged(
        @NonNull DownloadManager downloadManager,
        @NonNull Download download,
        @Nullable Exception finalException
    ) {
      downloads.put(download.request.uri, download);
      for (Listener listener : listeners) {
        listener.onDownloadsChanged();
      }
    }

    @Override
    public void onDownloadRemoved(
        @NonNull DownloadManager downloadManager,
        @NonNull Download download
    ) {
      downloads.remove(download.request.uri);
      for (Listener listener : listeners) {
        listener.onDownloadsChanged();
      }
    }

  }

  // --------------------------------------------------------------------------- class: StartDownloadHelper

  private final class StartDownloadHelper implements DownloadHelper.Callback, WidevineOfflineLicenseFetchTask.Callback {

    private final DownloadHelper downloadHelper;
    private final MediaItem mediaItem;

    private MappedTrackInfo mappedTrackInfo;
    private WidevineOfflineLicenseFetchTask widevineOfflineLicenseFetchTask;
    @Nullable private byte[] keySetId;

    public StartDownloadHelper(DownloadHelper downloadHelper, MediaItem mediaItem) {
      this.downloadHelper = downloadHelper;
      this.mediaItem = mediaItem;
      downloadHelper.prepare(this);
    }

    public void release() {
      downloadHelper.release();

      if (widevineOfflineLicenseFetchTask != null) {
        widevineOfflineLicenseFetchTask.cancel(false);
      }
    }

    // DownloadHelper.Callback implementation.

    @Override
    public void onPrepared(@NonNull DownloadHelper helper) {
      @Nullable Format format = getFirstFormatWithDrmInitData(helper);
      if (format == null) {
        onDownloadPrepared(helper);
        return;
      }

      // The content is DRM protected. We need to acquire an offline license.
      if (Util.SDK_INT < 18) {
        Toast.makeText(context, R.string.toast_downloadtracker_error_drm_unsupported_before_api_18, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Downloading DRM protected content is not supported on API versions below 18");
        return;
      }

      // TODO(internal b/163107948): Support cases where DrmInitData are not in the manifest.
      if (!hasSchemaData(format.drmInitData)) {
        Toast.makeText(context, R.string.toast_downloadtracker_error_download_start_offline_license, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Downloading content where DRM scheme data is not located in the manifest is not supported");
        return;
      }

      widevineOfflineLicenseFetchTask = new WidevineOfflineLicenseFetchTask(
          format,
          mediaItem.playbackProperties.drmConfiguration,
          httpDataSourceFactory,
          /* callback= */ this,
          helper
      );

      widevineOfflineLicenseFetchTask.execute();
    }

    @Override
    public void onPrepareError(@NonNull DownloadHelper helper, @NonNull IOException e) {
      boolean isLiveContent = e instanceof LiveContentUnsupportedException;
      int toastStringId = isLiveContent ? R.string.toast_downloadtracker_error_download_live_unsupported : R.string.toast_downloadtracker_error_download_start;
      String logMessage = isLiveContent ? "Downloading live content unsupported" : "Failed to start download";
      Toast.makeText(context, toastStringId, Toast.LENGTH_LONG).show();
      Log.e(TAG, logMessage, e);
    }

    // WidevineOfflineLicenseFetchTask.Callback implementation.

    @Override
    public void onOfflineLicenseFetched(DownloadHelper helper, byte[] keySetId) {
      this.keySetId = keySetId;
      onDownloadPrepared(helper);
    }

    @Override
    public void onOfflineLicenseFetchedError(DrmSession.DrmSessionException e) {
      Toast.makeText(context, R.string.toast_downloadtracker_error_download_start_offline_license, Toast.LENGTH_LONG).show();
      Log.e(TAG, "Failed to fetch offline DRM license", e);
    }

    // Internal methods.

    /**
     * Returns the first {@link Format} with a non-null {@link Format#drmInitData} found in the
     * content's tracks, or null if none is found.
     */
    @Nullable
    private Format getFirstFormatWithDrmInitData(DownloadHelper helper) {
      for (int periodIndex = 0; periodIndex < helper.getPeriodCount(); periodIndex++) {
        MappedTrackInfo mappedTrackInfo = helper.getMappedTrackInfo(periodIndex);
        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
          TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
          for (int trackGroupIndex = 0; trackGroupIndex < trackGroups.length; trackGroupIndex++) {
            TrackGroup trackGroup = trackGroups.get(trackGroupIndex);
            for (int formatIndex = 0; formatIndex < trackGroup.length; formatIndex++) {
              Format format = trackGroup.getFormat(formatIndex);
              if (format.drmInitData != null) {
                return format;
              }
            }
          }
        }
      }
      return null;
    }

    private void onDownloadPrepared(DownloadHelper helper) {
      Log.d(TAG, "Downloading entire stream.");
      startDownload();
      downloadHelper.release();
      return;
    }

    /**
     * Returns whether any the {@link DrmInitData.SchemeData} contained in {@code drmInitData} has
     * non-null {@link DrmInitData.SchemeData#data}.
     */
    private boolean hasSchemaData(DrmInitData drmInitData) {
      for (int i = 0; i < drmInitData.schemeDataCount; i++) {
        if (drmInitData.get(i).hasData()) {
          return true;
        }
      }
      return false;
    }

    private void startDownload() {
      startDownload(buildDownloadRequest());
    }

    private void startDownload(DownloadRequest downloadRequest) {
      DownloadService.sendAddDownload(context, MyDownloadService.class, downloadRequest, /* foreground= */ false);
    }

    private DownloadRequest buildDownloadRequest() {
      String uri = checkNotNull(checkNotNull(mediaItem.playbackProperties).uri).toString();

      if (uri.length() > 40)
        uri = uri.substring(0, 40);

      return downloadHelper
          .getDownloadRequest(
            Util.getUtf8Bytes(uri)
          )
          .copyWithKeySetId(keySetId);
    }

  }

  // --------------------------------------------------------------------------- class: WidevineOfflineLicenseFetchTask

  /** Downloads a Widevine offline license in a background thread. */
  @RequiresApi(18)
  private static final class WidevineOfflineLicenseFetchTask extends AsyncTask<Void, Void, Void> {

    public interface Callback {
        void onOfflineLicenseFetched(DownloadHelper helper, byte[] keySetId);
        void onOfflineLicenseFetchedError(DrmSession.DrmSessionException e);
    }

    private final Format format;
    private final MediaItem.DrmConfiguration drmConfiguration;
    private final HttpDataSource.Factory httpDataSourceFactory;
    private final WidevineOfflineLicenseFetchTask.Callback callback;
    private final DownloadHelper downloadHelper;

    @Nullable private byte[] keySetId;
    @Nullable private DrmSession.DrmSessionException drmSessionException;

    public WidevineOfflineLicenseFetchTask(
        Format format,
        MediaItem.DrmConfiguration drmConfiguration,
        HttpDataSource.Factory httpDataSourceFactory,
        WidevineOfflineLicenseFetchTask.Callback callback,
        DownloadHelper downloadHelper
    ) {
      this.format = format;
      this.drmConfiguration = drmConfiguration;
      this.httpDataSourceFactory = httpDataSourceFactory;
      this.callback = callback;
      this.downloadHelper = downloadHelper;
    }

    @Override
    protected Void doInBackground(Void... voids) {
      OfflineLicenseHelper offlineLicenseHelper = OfflineLicenseHelper.newWidevineInstance(
          drmConfiguration.licenseUri.toString(),
          drmConfiguration.forceDefaultLicenseUri,
          httpDataSourceFactory,
          drmConfiguration.requestHeaders,
          new DrmSessionEventListener.EventDispatcher()
      );

      try {
        keySetId = offlineLicenseHelper.downloadLicense(format);
      }
      catch (DrmSession.DrmSessionException e) {
        drmSessionException = e;
      }
      finally {
        offlineLicenseHelper.release();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (drmSessionException != null) {
        callback.onOfflineLicenseFetchedError(drmSessionException);
      }
      else {
        callback.onOfflineLicenseFetched(downloadHelper, checkStateNotNull(keySetId));
      }
    }
  }

  // ---------------------------------------------------------------------------

}
