package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;

import android.util.Log;

import java.util.HashMap;

public class BplistParser {
  private static final String tag = BplistParser.class.getSimpleName();

  public static HashMap parse(byte[] plistbytes) {
    HashMap<String, Object> map = new HashMap<String, Object>();

    try {
      NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(plistbytes);

      String[] keys = rootDict.allKeys();

      for (int i = 0; i < keys.length; i++) {
        Log.d(tag, "airplay parser entity key is=" + keys[i]);

        NSObject object = rootDict.objectForKey(keys[i]);
        if (object.getClass().equals(NSNumber.class)) {
          NSNumber num = (NSNumber) object;
          Log.d(tag, "airplay parser value Type is" + num.type() + " and value is=" + rootDict.objectForKey(keys[i]));
          switch (num.type()) {
            case NSNumber.BOOLEAN : {
              boolean bool = num.boolValue();
              map.put(keys[i], new Boolean(bool));
              break;
            }
            case NSNumber.INTEGER : {
              long l = num.longValue();
              map.put(keys[i], new Long(l));
              break;
            }
            case NSNumber.REAL : {
              double d = num.doubleValue();
              map.put(keys[i], new Double(d));
              break;
            }
          }
        }
        else if (object.getClass().equals(NSString.class)) {
          map.put(keys[i], object.toString());
          Log.d(tag, "airplay parser Value is Type of String and values is =" + rootDict.objectForKey(keys[i]));
        }
        else {
          map.put(keys[i], object);
          Log.d(tag, "airplay parser values is Type of =" + rootDict.objectForKey(keys[i]));
        }
      }
    }
    catch (Exception e) {
      Log.e(tag, "problem in airplay parser", e);
      return null;
    }

    return map;
  }
}
