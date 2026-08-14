package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

/*
 * based on:
 *   https://github.com/androidx/media/blob/1.11.0/demos/main/src/main/java/androidx/media3/demo/main/DownloadTracker.java
 */

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.MyDownloadService;

import static androidx.media3.common.util.Assertions.checkNotNull;
import static androidx.media3.common.util.Assertions.checkState;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.drm.DrmSession;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.OfflineLicenseHelper;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadCursor;
import androidx.media3.exoplayer.offline.DownloadHelper;
import androidx.media3.exoplayer.offline.DownloadHelper.LiveContentUnsupportedException;
import androidx.media3.exoplayer.offline.DownloadIndex;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadRequest;
import androidx.media3.exoplayer.offline.DownloadService;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
  private final DataSource.Factory dataSourceFactory;
  private final DownloadManager downloadManager;
  private final CopyOnWriteArraySet<Listener> listeners;
  private final HashMap<Uri, Download> downloads;
  private final DownloadIndex downloadIndex;

  // --------------------------------------------------------------------------- public API:

  public DownloadTracker(
      Context context,
      DataSource.Factory dataSourceFactory,
      DownloadManager downloadManager
  ) {
    this.context = context.getApplicationContext();
    this.dataSourceFactory = dataSourceFactory;
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
    Uri uri = checkNotNull(mediaItem.localConfiguration).uri;
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
    @Nullable Download download = downloads.get(checkNotNull(mediaItem.localConfiguration).uri);
    startDownload(download, mediaItem, renderersFactory, null);
  }

  public void startDownload(Download download, MediaItem mediaItem, RenderersFactory renderersFactory, TrackSelectionParameters trackSelectionParameters) {
    if (download == null || download.state == Download.STATE_FAILED) {
      if (trackSelectionParameters == null) {
        trackSelectionParameters = DownloadHelper.getDefaultTrackSelectorParameters(context);
      }
      DownloadHelper downloadHelper = DownloadHelper.forMediaItem(mediaItem, trackSelectionParameters, renderersFactory, dataSourceFactory);
      new StartDownloadHelper(downloadHelper, mediaItem);
    }
  }

  public void stopDownload(MediaItem mediaItem) {
    @Nullable Download download = downloads.get(checkNotNull(mediaItem.localConfiguration).uri);
    stopDownload(download);
  }

  public void stopDownload(Download download) {
    if (download != null && download.state != Download.STATE_FAILED) {
      DownloadService.sendRemoveDownload(context, MyDownloadService.class, download.request.id, /* foreground= */ false);
    }
  }

  public void toggleDownload(MediaItem mediaItem, RenderersFactory renderersFactory, TrackSelectionParameters trackSelectionParameters) {
    @Nullable Download download = downloads.get(checkNotNull(mediaItem.localConfiguration).uri);
    if (download != null && download.state != Download.STATE_FAILED) {
      stopDownload(download);
    }
    else {
      startDownload(download, mediaItem, renderersFactory, trackSelectionParameters);
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
        DownloadManager downloadManager,
        Download download,
        @Nullable Exception finalException
    ) {
      downloads.put(download.request.uri, download);
      for (Listener listener : listeners) {
        listener.onDownloadsChanged();
      }
    }

    @Override
    public void onDownloadRemoved(
        DownloadManager downloadManager,
        Download download
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

    private boolean tracksInfoAvailable;
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
        widevineOfflineLicenseFetchTask.cancel();
      }
    }

    // DownloadHelper.Callback implementation.

    @Override
    public void onPrepared(DownloadHelper helper, boolean tracksInfoAvailable) {
      this.tracksInfoAvailable = tracksInfoAvailable;
      @Nullable Format format = getFirstFormatWithDrmInitData(helper);
      if (format == null) {
        onDownloadPrepared(helper);
        return;
      }

      // The content is DRM protected. We need to acquire an offline license.

      // TODO(internal b/163107948): Support cases where DrmInitData are not in the manifest.
      if (!hasNonNullWidevineSchemaData(format.drmInitData)) {
        Toast.makeText(context, R.string.toast_downloadtracker_error_download_start_offline_license, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Downloading content where DRM scheme data is not located in the manifest is not supported");
        return;
      }

      widevineOfflineLicenseFetchTask = new WidevineOfflineLicenseFetchTask(
          format,
          mediaItem.localConfiguration.drmConfiguration,
          dataSourceFactory,
          /* callback= */ this,
          helper
      );

      widevineOfflineLicenseFetchTask.execute();
    }

    @Override
    public void onPrepareError(DownloadHelper helper, IOException e) {
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
      if (!tracksInfoAvailable) {
        return null;
      }
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
     * Returns whether any {@link DrmInitData.SchemeData} that {@linkplain
     * DrmInitData.SchemeData#matches(UUID) matches} {@link C#WIDEVINE_UUID} has non-null {@link
     * DrmInitData.SchemeData#data}.
     */
    private boolean hasNonNullWidevineSchemaData(DrmInitData drmInitData) {
      for (int i = 0; i < drmInitData.schemeDataCount; i++) {
        DrmInitData.SchemeData schemeData = drmInitData.get(i);
        if (schemeData.matches(C.WIDEVINE_UUID) && schemeData.hasData()) {
          return true;
        }
      }
      return false;
    }

    private void startDownload() {
      startDownload(buildDownloadRequest());
    }

    private void startDownload(DownloadRequest downloadRequest) {
      if ((downloadRequest == null) || downloadRequest.streamKeys.isEmpty())
        return;

      DownloadService.sendAddDownload(context, MyDownloadService.class, downloadRequest, /* foreground= */ false);
    }

    private DownloadRequest buildDownloadRequest() {
      String uri = checkNotNull(checkNotNull(mediaItem.localConfiguration).uri).toString();

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
  private static final class WidevineOfflineLicenseFetchTask {

    public interface Callback {
        void onOfflineLicenseFetched(DownloadHelper helper, byte[] keySetId);
        void onOfflineLicenseFetchedError(DrmSession.DrmSessionException e);
    }

    private final Format format;
    private final MediaItem.DrmConfiguration drmConfiguration;
    private final DataSource.Factory dataSourceFactory;
    private final WidevineOfflineLicenseFetchTask.Callback callback;
    private final DownloadHelper downloadHelper;
    private final ExecutorService executorService;

    @Nullable private Future<?> future;
    @Nullable private byte[] keySetId;
    @Nullable private DrmSession.DrmSessionException drmSessionException;

    public WidevineOfflineLicenseFetchTask(
        Format format,
        MediaItem.DrmConfiguration drmConfiguration,
        DataSource.Factory dataSourceFactory,
        WidevineOfflineLicenseFetchTask.Callback callback,
        DownloadHelper downloadHelper
    ) {
      checkState(drmConfiguration.scheme.equals(C.WIDEVINE_UUID));

      this.format = format;
      this.drmConfiguration = drmConfiguration;
      this.dataSourceFactory = dataSourceFactory;
      this.callback = callback;
      this.downloadHelper = downloadHelper;
      this.executorService = Executors.newSingleThreadExecutor();
    }

    public void cancel() {
      if (future != null) {
        future.cancel(/* mayInterruptIfRunning= */ false);
      }
    }

    public void execute() {
      future = executorService.submit(() -> {
        OfflineLicenseHelper offlineLicenseHelper = OfflineLicenseHelper.newWidevineInstance(
          drmConfiguration,
          dataSourceFactory,
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

          new Handler(Looper.getMainLooper()).post(() -> {
            if (drmSessionException != null) {
              callback.onOfflineLicenseFetchedError(drmSessionException);
            }
            else {
              callback.onOfflineLicenseFetched(downloadHelper, checkNotNull(keySetId));
            }
          });
        }
      });
    }
  }

  // ---------------------------------------------------------------------------

}
