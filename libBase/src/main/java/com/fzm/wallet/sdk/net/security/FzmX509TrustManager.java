package com.fzm.wallet.sdk.net.security;

import android.annotation.SuppressLint;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * @author zhengjy
 * @date 2018/08/14
 * Description:
 */
public class FzmX509TrustManager implements X509TrustManager {

    private X509TrustManager standardTrustManager;


    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'
    };

    public FzmX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keystore);
        TrustManager[] trustManagers = factory.getTrustManagers();
        if (trustManagers.length == 0) {
            throw new NoSuchAlgorithmException("no trust manager found");
        }
        this.standardTrustManager = (X509TrustManager) trustManagers[0];
    }

    @Override
    @SuppressLint("TrustAllX509TrustManager")
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // 用于服务端校验客户端证书
    }

    /**
     * 校验服务端证书的接口
     *
     * @param chain    证书链  自己网站的证书A在第一位，签署A的证书B在第二位，签署B的证书在第三位，以此类推，直到根证书
     * @param authType
     * @throws CertificateException
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        return;
   /*     if (BuildConfig.DEBUG) {
            // 调试环境不做证书验证
            return;
        }
        // 如果证书链长度只有1， 说明没有被可信ca签署过
        if (chain == null || chain.length == 1) {
            throw new CertificateException("服务端证书不可信，请关闭代理");
        }
        // 最后再校验一下证书链
        standardTrustManager.checkServerTrusted(chain, authType);*/
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.standardTrustManager.getAcceptedIssuers();
    }

    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_DIGITS[(b & 0xf0) >> 4]);
            sb.append(HEX_DIGITS[b & 0x0f]);
        }
        return sb.toString();
    }
}
