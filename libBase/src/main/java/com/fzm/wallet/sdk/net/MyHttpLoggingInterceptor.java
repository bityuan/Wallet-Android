package com.fzm.wallet.sdk.net;

import android.util.Log;


import com.fzm.wallet.sdk.BuildConfig;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;

/**
 * Created by ZX on 2018/6/9.
 */

public class MyHttpLoggingInterceptor implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String TAG = "mydao";


    private int noticeCount = 0;
    private int walletCoinCount = 0;
    private int logLength = 1000;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        RequestBody requestBody = request.body();
        Map<String, List<String>> stringListMap = request.headers().toMultimap();
        String headers = "";
        for (String key : stringListMap.keySet()) {
            headers += ("\n{" + key + " : " + request.headers().toMultimap().get(key) + "}");
        }

        String requestBodyStr = configRequestBody(requestBody);
        Response response = chain.proceed(request);
        String url = request.toString();
        String reUrl = request.url().toString();
        String responseBodyStr = bufferBody(response);
        if (BuildConfig.DEBUG) {
            //app上面配置的域名
            if (reUrl.contains("notice")) {
                noticeCount++;
                Log.w(TAG, "===========通知============" + noticeCount);
            } else if (reUrl.contains("wallet-coin")) {
                walletCoinCount++;
                Log.w(TAG, "===========首页数据========" + walletCoinCount);
            } else {
                String jsonResult = JsonFormatUtils.formatJson(responseBodyStr);
                String result = "";
                if (jsonResult.length() > logLength) {
                    result = jsonResult.substring(0, logLength);
                } else {
                    result = jsonResult;
                }
                Log.w(TAG,
                        "\nheader: "
                                + headers
                                + "\ncode: "
                                + response.code()
                                + "\nmessage: "
                                + response.message()
                                + "\nrequestBodyStr: "
                                + requestBodyStr
                                + "\n url: "
                                + url
                                + "\n result: "
                                + result
                );
            }
        }
        return response.newBuilder().build();
    }

    private String configRequestBody(RequestBody requestBody) {
        if (null == requestBody) {
            return "";
        }
        Charset charset = UTF8;
        MediaType contentType = requestBody.contentType();
        if (contentType != null) {
            charset = contentType.charset(UTF8);
        }

        Buffer buffer = new Buffer();
        try {
            requestBody.writeTo(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String requestBodyStr = buffer.readString(charset);

        return requestBodyStr;
    }


    //我的解决日志乱码问题
    //https://blog.csdn.net/big_sea_m/article/details/90573484
    private String bufferBody(Response response) throws IOException {
        Headers headers = response.headers();
        ResponseBody responseBody = response.body();

        BufferedSource source = responseBody.source();
        source.request(Long.MAX_VALUE); // Buffer the entire body.
        Buffer buffer = source.getBuffer();
        // 判断是否有压缩
        if ("gzip".equalsIgnoreCase(headers.get("Content-Encoding"))) {
            GzipSource gzippedResponseBody = null;
            try {
                gzippedResponseBody = new GzipSource(buffer.clone());
                buffer = new Buffer();
                buffer.writeAll(gzippedResponseBody);
            } finally {
                if (gzippedResponseBody != null) {
                    gzippedResponseBody.close();
                }
            }
        }

        return buffer.clone().readString(Charset.forName("UTF-8"));
    }


}

