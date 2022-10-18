package javax.jmdns.impl;

/*
 * references:
 *   https://github.com/jmdns/jmdns/blob/3.5.8/src/main/java/javax/jmdns/JmDNS.java
 *   https://github.com/jmdns/jmdns/blob/3.5.8/src/main/java/javax/jmdns/impl/JmDNSImpl.java
 */

import javax.jmdns.impl.HostInfo;
import javax.jmdns.impl.JmDNSImpl;

import java.io.IOException;
import java.net.InetAddress;

public class MyJmDNSImpl extends JmDNSImpl {
  public MyJmDNSImpl(InetAddress address, String name, long threadSleepDurationMs) throws IOException {
    super(address, name, threadSleepDurationMs);

    if (address != null) {
      HostInfo localHost = getLocalHost();
      localHost._name = address.getHostAddress();
    }
  }
}
