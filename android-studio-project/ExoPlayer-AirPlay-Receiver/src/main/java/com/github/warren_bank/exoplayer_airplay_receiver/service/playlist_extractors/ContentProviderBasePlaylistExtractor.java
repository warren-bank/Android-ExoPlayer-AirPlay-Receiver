package com.github.warren_bank.exoplayer_airplay_receiver.service.playlist_extractors;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.ExternalStorageUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public abstract class ContentProviderBasePlaylistExtractor extends BasePlaylistExtractor {

  private Context context;

  public ContentProviderBasePlaylistExtractor(Context context) {
    this.context = context;
  }

  protected abstract boolean isParserForUri(Uri uri);

  protected abstract void parseLine(String line, Uri context, ArrayList<String> matches);

  protected void preParse(Uri context) {}

  protected void postParse(Uri context, ArrayList<String> matches) {}

  protected String resolveM3uPlaylistItem(Uri context, String relative) {
    return resolveM3uPlaylistItem(
      ((context != null) ? context.toString() : ""),
      relative
    );
  }

  public ArrayList<String> expandPlaylist(String strUrl) {
    if (!ExternalStorageUtils.isContentUri(strUrl))
      return null;

    Uri uri = Uri.parse(strUrl);

    if (uri == null)
      return null;

    if (!isParserForUri(uri))
      return null;

    ArrayList<String> matches = new ArrayList<String>();
    BufferedReader in = null;

    try {
      String line;

      // read the content of the text file
      ContentResolver   cr  = context.getContentResolver();
      InputStream       is  = cr.openInputStream(uri);
      InputStreamReader isr = new InputStreamReader(is);
      in = new BufferedReader(isr);

      preParse(uri);
      while ((line = in.readLine()) != null) {
        line = normalizeLine(line);
        if (line == null) continue;

        parseLine(line, uri, matches);
      }
      postParse(uri, matches);
    }
    catch (Exception e) {
    }
    finally {
      if (in != null) {
        try {
          in.close();
        }
        catch(Exception e) {}
      }

      // normalize that non-null return value must include matches
      if (matches.isEmpty())
        matches = null;
    }

    return matches;
  }

}
