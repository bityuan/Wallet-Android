package com.fzm.wallet.sdk.utils;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class GZipUtils {
    public static String compress(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = null;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (gzip != null) {
                try {
                    gzip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP);
        //return new String(Base64.getEncoder().encode(out.toByteArray()));
    }

    /**
     * 使用gzip解压缩
     *
     * @param compressedStr 压缩字符串
     */
    public static String uncompress(String compressedStr) {
        if (compressedStr == null) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = null;
        GZIPInputStream ginzip = null;
        byte[] compressed = null;
        String decompressed = null;
        try {
            // compressed = Base64.getDecoder().decode(compressedStr);
            compressed = android.util.Base64.decode(compressedStr, android.util.Base64.NO_WRAP);
            in = new ByteArrayInputStream(compressed);
            ginzip = new GZIPInputStream(in);
            byte[] buffer = new byte[1024];
            int offset = -1;
            while ((offset = ginzip.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
            decompressed = out.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ginzip != null) {
                try {
                    ginzip.close();
                } catch (IOException ignored) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
        return decompressed;
    }


    public static void test() {
        String str = "qwertyuiopasdfghjklzxcvbnm0123456789~!@#$%^&*()_+`¥……——+|《》？,./城市 姓名";
        byte[] byteStr = str.getBytes(StandardCharsets.UTF_8);
        String encode_DEFAULT = android.util.Base64.encodeToString(byteStr, android.util.Base64.DEFAULT);
        String encode_NO_PADDING = android.util.Base64.encodeToString(byteStr, android.util.Base64.NO_PADDING);
        String encode_NO_WRAP = android.util.Base64.encodeToString(byteStr, android.util.Base64.NO_WRAP);
        String encodeURL_SAFE = android.util.Base64.encodeToString(byteStr, android.util.Base64.URL_SAFE);


        Log.v("wlike","encode_DEFAULT = "+encode_DEFAULT);
        Log.v("wlike","encode_NO_PADDING = "+encode_NO_PADDING);
        Log.v("wlike","encode_NO_WRAP = "+encode_NO_WRAP);
        Log.v("wlike","encodeURL_SAFE = "+encodeURL_SAFE);


    }

}
