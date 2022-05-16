package com.fzm.wallet.sdk.net.security;

import android.annotation.SuppressLint;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

/**
 * 接口证书验证
 */
public class SSLSocketClient {

    private static final X509TrustManager DEFAULT_X509_TRUST_MANAGER = new X509TrustManager() {
        @Override
        @SuppressLint("TrustAllX509TrustManager")
        public void checkClientTrusted(X509Certificate[] chain, String authType) { }
        @Override
        @SuppressLint("TrustAllX509TrustManager")
        public void checkServerTrusted(X509Certificate[] chain, String authType) { }
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    /**
     * 获取这个SSLSocketFactory
     *
     * @return  SSLSocketFactory
     */
    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new X509TrustManager[]{getTrustManager()}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取X509TrustManager
     *
     * @return  X509TrustManager
     */
    public static X509TrustManager getTrustManager() {
        try {
            return new FzmX509TrustManager(null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return DEFAULT_X509_TRUST_MANAGER;
    }

    /**
     * 获取HostnameVerifier
     *
      * @return HostnameVerifier
     */
    public static HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession sslSession) {
                // FIXME 按理是要比对所需的全部域名
                return true;
            }
        };
    }  
}  