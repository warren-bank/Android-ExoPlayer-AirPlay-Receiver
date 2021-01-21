package com.github.warren_bank.exoplayer_airplay_receiver.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class PermissiveSSLSocketFactory extends SSLSocketFactory {

    private boolean enableAllSupportedCipherSuites;
    private boolean enableAllSupportedProtocols;
    private boolean trustAllCertificates;
    private SSLSocketFactory internalSSLSocketFactory;

    public PermissiveSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException, GeneralSecurityException {
        this(
            /* enableAllSupportedCipherSuites= */ true,
            /* enableAllSupportedProtocols=    */ true,
            /* trustAllCertificates=           */ true
        );
    }

    public PermissiveSSLSocketFactory(boolean enableAllSupportedCipherSuites, boolean enableAllSupportedProtocols, boolean trustAllCertificates) throws KeyManagementException, NoSuchAlgorithmException, GeneralSecurityException {
        this.enableAllSupportedCipherSuites = enableAllSupportedCipherSuites;
        this.enableAllSupportedProtocols    = enableAllSupportedProtocols;
        this.trustAllCertificates           = trustAllCertificates;

        TrustManager[] tm   = null;
        SecureRandom random = null;

        if (trustAllCertificates) {
            tm = new TrustManager[] {
              new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
              }
            };

            random = new SecureRandom();
        }

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tm, random);
        this.internalSSLSocketFactory = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return this.enableAllSupportedCipherSuites
            ? internalSSLSocketFactory.getSupportedCipherSuites()
            : internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }
    
    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if(socket != null && (socket instanceof SSLSocket)) {
            if (this.enableAllSupportedProtocols) {
                SSLSocket sslsocket = (SSLSocket) socket;
                sslsocket.setEnabledProtocols(
                    sslsocket.getSupportedProtocols()
                );
            }
        }
        return socket;
    }
}
