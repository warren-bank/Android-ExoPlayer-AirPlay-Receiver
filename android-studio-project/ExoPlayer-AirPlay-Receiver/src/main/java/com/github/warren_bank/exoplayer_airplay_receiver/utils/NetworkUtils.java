package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetworkUtils {

  public synchronized static Inet4Address getLocalIpAddress() {
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface
          .getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
            .hasMoreElements();) {
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

  public synchronized static String[] getMACAddress(InetAddress ia) throws Exception {
    //Obtain the network interface object (that is, the network card), and get the mac address. The mac address exists in a byte array.
    byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();

    //The following code assembles the mac address into a String
    String[] str_array = new String[2];
    StringBuffer sb1 = new StringBuffer();
    StringBuffer sb2 = new StringBuffer();

    for (int i = 0; i < mac.length; i++) {
      if (i != 0) {
        sb1.append(":");
      }
      //mac[i] & 0xFF ..to convert bytes into positive integers
      String s = Integer.toHexString(mac[i] & 0xFF);
      sb1.append(s.length() == 1 ? 0 + s : s);
      sb2.append(s.length() == 1 ? 0 + s : s);
    }
    //Change all lowercase letters of the string to regular mac addresses and return
    str_array[0] = sb1.toString();
    str_array[1] = sb2.toString();
    return str_array;
    //return sb1.toString().toUpperCase();
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
