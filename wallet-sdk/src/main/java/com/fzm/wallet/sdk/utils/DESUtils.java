package com.fzm.wallet.sdk.utils;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * Created by miracle on 2017/8/10.
 */

public class DESUtils {


    public final static String DES_KEY = "008f80e79e6b8c6a500e54e216e38ac2";
    public final static String DES_KEY_HOST = "90hgk0e79e6b8c6a500e54e2168jyhek";

    //加密
    public static String encrypt(String message) {
        try {
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

            DESKeySpec desKeySpec = new DESKeySpec(DES_KEY.getBytes("UTF-8"));

            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
            IvParameterSpec iv = new IvParameterSpec("33878402".getBytes("UTF-8"));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            return encodeBase64(cipher.doFinal(message.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    //加密
    public static String encrypt(String deckey, String message) {
        try {
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

            DESKeySpec desKeySpec = new DESKeySpec(deckey.getBytes("UTF-8"));

            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
            IvParameterSpec iv = new IvParameterSpec("33878402".getBytes("UTF-8"));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            return encodeBase64(cipher.doFinal(message.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    //解密
    public static String decrypt(String message) {
        try {

            byte[] bytesrc = decodeBase64(message);//convertHexString(message);
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            DESKeySpec desKeySpec = new DESKeySpec(DES_KEY.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
            IvParameterSpec iv = new IvParameterSpec("33878402".getBytes("UTF-8"));

            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            byte[] retByte = cipher.doFinal(bytesrc);
            return new String(retByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    //解密
    public static String decrypt(String decKey, String message) {
        try {

            byte[] bytesrc = decodeBase64(message);//convertHexString(message);
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            DESKeySpec desKeySpec = new DESKeySpec(decKey.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
            IvParameterSpec iv = new IvParameterSpec("33878402".getBytes("UTF-8"));

            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            byte[] retByte = cipher.doFinal(bytesrc);
            return new String(retByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static byte[] convertHexString(String ss) {
        byte digest[] = new byte[ss.length() / 2];
        for (int i = 0; i < digest.length; i++) {
            String byteString = ss.substring(2 * i, 2 * i + 2);
            int byteValue = Integer.parseInt(byteString, 16);
            digest[i] = (byte) byteValue;
        }

        return digest;
    }

    public static String toHexString(byte b[]) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            String plainText = Integer.toHexString(0xff & b[i]);
            if (plainText.length() < 2)
                plainText = "0" + plainText;
            hexString.append(plainText);
        }

        return hexString.toString();
    }


    public static String encodeBase64(byte[] b) {
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public static byte[] decodeBase64(String base64String) {
        return Base64.decode(base64String, Base64.DEFAULT);
    }

    public static void main(String[] args) throws Exception {
        String s = "5Df8$&@S";
        String ec = DESUtils.decrypt("XuqnSzQJraJ8ZTOuCZv38JmePWOOlWID9DLbyTJNK9OFIgpSHGEOM6r5R0aB3xuTR5xLQpgxTMVMgrlusoGqgJKX6f6BB3a2wdQ2EbQZI41ohRHHnq0pqXlhTZVuRsSL8jmsSVqWFiOplcT3M7YaTfyKeuzYWpbtpIVBgrpkblGJzjt0WXKZbONW44r87opnW629Og2AXDzAl48D/sHpyZuDKUJcB/1zSSA1XfPeFHkogyclNPU+GXI1MgcjTiFVCD7bNj7i9YLsQPYNxH4gKm+JK9xHBBGUH1kEYlK0JIRUSrYhQGI0pMrJ9Oqtz0cqMbhmKf0GvZSoYGu6PAI63VqB7D/nvsg80W0t0kb1vyQKyB7iUvcA0HoXqcXzb1XENBFLcCLcW184F3Lp1nlHTdJ1QIiLIjdI5nUzKsfmdulEgfd0Gh6sG3xSdYyjryHfKn6eanU3kZu2YAq/");
        System.out.println(ec);
    }

}
