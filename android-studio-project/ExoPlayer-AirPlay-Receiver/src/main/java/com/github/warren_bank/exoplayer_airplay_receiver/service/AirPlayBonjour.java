package com.github.warren_bank.exoplayer_airplay_receiver.service;

import com.github.warren_bank.exoplayer_airplay_receiver.utils.NetworkUtils;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers airplay/airtunes service mdns
 */
public class AirPlayBonjour {

  private static final String tag                   = AirPlayBonjour.class.getSimpleName();
  private static final String AIRPLAY_SERVICE_TYPE  = "._airplay._tcp.local";
  private static final String AIRTUNES_SERVICE_TYPE = "._raop._tcp.local";

  private final String serverName;

  private JmDNS airPlayJmDNS;
  private JmDNS airTunesJmDNS;

  private ServiceInfo airPlayService;
  private ServiceInfo airTunesService;

  public AirPlayBonjour(String serverName) {
    this.serverName = serverName;

    airPlayJmDNS    = null;
    airTunesJmDNS   = null;
    airPlayService  = null;
    airTunesService = null;
  }

  public void start(InetAddress localAddress, int airPlayPort, int airTunesPort) throws Exception {
    stop();

    String[] macAddress = NetworkUtils.getMACAddress(localAddress);

    airPlayJmDNS  = JmDNS.create(localAddress, "localhost", 0l);
    airTunesJmDNS = JmDNS.create(localAddress, "localhost", 0l);

    airPlayService = ServiceInfo.create((serverName + AIRPLAY_SERVICE_TYPE), serverName, airPlayPort, 0, 0, airPlayMDNSProps(macAddress[0]));
    airPlayJmDNS.registerService(airPlayService);
    Log.d(tag, String.format("'%s' service is registered on port %s", serverName + AIRPLAY_SERVICE_TYPE, airPlayPort));

    String airTunesServerName = macAddress[1] + "@" + serverName;
    airTunesService = ServiceInfo.create((airTunesServerName + AIRTUNES_SERVICE_TYPE), airTunesServerName, airTunesPort, 0, 0, airTunesMDNSProps());
    airTunesJmDNS.registerService(airTunesService);
    Log.d(tag, String.format("'%s' service is registered on port %s", airTunesServerName + AIRTUNES_SERVICE_TYPE, airTunesPort));
  }

  public void stop() {
    if (airPlayJmDNS != null) {
      try {
        airPlayJmDNS.unregisterService(airPlayService);
        airPlayJmDNS.close();
        Log.d(tag, String.format("'%s' service is unregistered", airPlayService.getName()));
      }
      catch (IOException e) {
        Log.e(tag, String.format("problem shutting down '%s' service", airPlayService.getName()), e);
      }
      finally {
        airPlayJmDNS   = null;
        airPlayService = null;
      }
    }

    if (airTunesJmDNS != null) {
      try {
        airTunesJmDNS.unregisterService(airTunesService);
        airTunesJmDNS.close();
        Log.d(tag, String.format("'%s' service is unregistered", airTunesService.getName()));
      }
      catch (IOException e) {
        Log.e(tag, String.format("problem shutting down '%s' service", airTunesService.getName()), e);
      }
      finally {
        airTunesJmDNS   = null;
        airTunesService = null;
      }
    }
  }

  private Map<String, String> airPlayMDNSProps(String macAddress) {
    HashMap<String, String> airPlayMDNSProps = new HashMap<>();
    airPlayMDNSProps.put("deviceid", macAddress);
    airPlayMDNSProps.put("features", "0x5A7FFFF7,0x1E");
    airPlayMDNSProps.put("srcvers", "220.68");
    airPlayMDNSProps.put("flags", "0x4");
    airPlayMDNSProps.put("vv", "2");
    airPlayMDNSProps.put("model", "AppleTV2,1");
    airPlayMDNSProps.put("rhd", "5.6.0.0");
    airPlayMDNSProps.put("pw", "false");
    airPlayMDNSProps.put("pk", "b07727d6f6cd6e08b58ede525ec3cdeaa252ad9f683feb212ef8a205246554e7");
    airPlayMDNSProps.put("pi", "2e388006-13ba-4041-9a67-25dd4a43d536");
    return airPlayMDNSProps;
  }

  private Map<String, String> airTunesMDNSProps() {
    HashMap<String, String> airTunesMDNSProps = new HashMap<>();
    airTunesMDNSProps.put("ch", "2");
    airTunesMDNSProps.put("cn", "0,1,2,3");
    airTunesMDNSProps.put("da", "true");
    airTunesMDNSProps.put("et", "0,3,5");
    airTunesMDNSProps.put("vv", "2");
    airTunesMDNSProps.put("ft", "0x5A7FFFF7,0x1E");
    airTunesMDNSProps.put("am", "AppleTV2,1");
    airTunesMDNSProps.put("md", "0,1,2");
    airTunesMDNSProps.put("rhd", "5.6.0.0");
    airTunesMDNSProps.put("pw", "false");
    airTunesMDNSProps.put("sr", "44100");
    airTunesMDNSProps.put("ss", "16");
    airTunesMDNSProps.put("sv", "false");
    airTunesMDNSProps.put("tp", "UDP");
    airTunesMDNSProps.put("txtvers", "1");
    airTunesMDNSProps.put("sf", "0x4");
    airTunesMDNSProps.put("vs", "220.68");
    airTunesMDNSProps.put("vn", "65537");
    airTunesMDNSProps.put("pk", "b07727d6f6cd6e08b58ede525ec3cdeaa252ad9f683feb212ef8a205246554e7");
    return airTunesMDNSProps;
  }
}
