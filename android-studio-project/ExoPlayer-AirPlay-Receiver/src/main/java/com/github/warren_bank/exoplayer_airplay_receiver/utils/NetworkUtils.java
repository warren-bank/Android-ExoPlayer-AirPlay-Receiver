package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetworkUtils {

  public synchronized static Inet4Address getLocalIpAddress() {
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();

        if (!intf.supportsMulticast())
          continue;

        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();

          if (!inetAddress.isLoopbackAddress()) {
            if (inetAddress instanceof Inet4Address) {
              return ((Inet4Address) inetAddress);
            }
          }
        }
      }
    }
    catch (SocketException ex) {
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

  public static String getLocalIp(Context context) {
    //Get wifi service
    WifiManager wifiManager = (WifiManager) context
        .getSystemService(Context.WIFI_SERVICE);
    //Determine if wifi is on
    if (!wifiManager.isWifiEnabled()) {
      wifiManager.setWifiEnabled(true);
    }
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    int ipAddress = wifiInfo.getIpAddress();
    String ip = intToIp(ipAddress);

    return ip;
  }

  public static boolean isWifiConnected(Context context) {
    ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    //Get status
    NetworkInfo.State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
    //Determine the conditions of wifi connection
    if (wifi == NetworkInfo.State.CONNECTED)
      return true;
    else
      return false;
  }

  private static String intToIp(int i) {
    return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
  }
}
