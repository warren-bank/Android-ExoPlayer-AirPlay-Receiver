package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class PermissiveHostnameVerifier implements HostnameVerifier {
    public boolean verify(String urlHostName, SSLSession session) {
        return true;
    }
}
