package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

public class NetworkUtils {

  public static boolean isWifiConnected(Context context) {
    try {
      ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

      //Get status
      NetworkInfo.State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();

      //Determine the conditions of wifi connection
      if (wifi != NetworkInfo.State.CONNECTED)
        throw new Exception("not connected");

      return true;
    }
    catch(Exception e) {
      return false;
    }
  }

  public static String getLocalIp(Context context) {
    int ip = getWifiIpAddress(context);
    return (ip > 0)
      ? formatIpAddress(ip)
      : null;
  }

  public synchronized static Inet4Address getLocalIpAddress(Context context) {
    try {
      int ip = getWifiIpAddress(context);
      byte[] address = convertIpAddress(ip);
      if (address == null) throw new Exception("no IP is assigned for WiFi network");

      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();

        if (!intf.supportsMulticast())
          continue;

        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();

          if (!inetAddress.isLoopbackAddress()) {
            if (inetAddress instanceof Inet4Address) {
              if (sameIpAddress(address, (Inet4Address) inetAddress)) {
                return ((Inet4Address) inetAddress);
              }
            }
          }
        }
      }
    }
    catch (SocketException ex) {
    }
    catch (Exception ex) {
    }
    return null;
  }

  public synchronized static String[] getMACAddress(InetAddress ia) {
    byte[] mac = null;
    try {
      //Obtain the network interface object (that is, the network card), and get the mac address. The mac address exists in a byte array.
      mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();

      if ((mac == null) || (mac.length < 6))
        throw new Exception("");
    }
    catch (Exception e) {
      mac = new byte[6];
      Random random = new Random();
      random.nextBytes(mac);
    }

    //The following code assembles the mac address into a String
    String[] macAddress = new String[2];
    StringBuffer sb = new StringBuffer();
    String s;

    for (int i = 0; i < mac.length; i++) {
      if (i != 0) {
        sb.append(":");
      }
      //mac[i] & 0xFF ..to convert bytes into positive integers
      s = Integer.toHexString(mac[i] & 0xFF);
      sb.append(s.length() == 1 ? "0" + s : s);
    }
    //Change all lowercase letters of the string to regular mac addresses
    s = sb.toString().toUpperCase();

    macAddress[0] = s;
    macAddress[1] = s.replace(":", "");
    return macAddress;
  }

  private static int getWifiIpAddress(Context context) {
    try {
      //Get wifi service
      WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

      //Determine if wifi is on
      if (!wifiManager.isWifiEnabled()) {
        wifiManager.setWifiEnabled(true);
      }

      WifiInfo wifiInfo = wifiManager.getConnectionInfo();
      int ip = wifiInfo.getIpAddress();
      return ip;
    }
    catch(Exception e) {
      return 0;
    }
  }

  private static String formatIpAddress(int ip) {
    if (ip <= 0) return null;

    byte[] address = convertIpAddress(ip);

    return (address[0] & 0xFF) + "." +
           (address[1] & 0xFF) + "." +
           (address[2] & 0xFF) + "." +
           (address[3] & 0xFF);
  }

  private static byte[] convertIpAddress(int ip) {
    byte[] address = new byte[4];
    address[0] = Integer.valueOf((ip      ) & 0xFF).byteValue();
    address[1] = Integer.valueOf((ip >>  8) & 0xFF).byteValue();
    address[2] = Integer.valueOf((ip >> 16) & 0xFF).byteValue();
    address[3] = Integer.valueOf((ip >> 24) & 0xFF).byteValue();
    return address;
  }

  private static boolean sameIpAddress(byte[] a, Inet4Address b) {
    return (b != null) && sameIpAddress(a, b.getAddress());
  }

  private static boolean sameIpAddress(byte[] a, byte[] b) {
    return
      (a != null)     &&
      (b != null)     &&
      (a.length == 4) &&
      (b.length == 4) &&
      (a[0] == b[0])  &&
      (a[1] == b[1])  &&
      (a[2] == b[2])  &&
      (a[3] == b[3]);
  }
}
