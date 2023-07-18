package com.fzm.wallet.sdk.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import java.util.Locale;

public class LocalManageUtil {

    public static final String LANGUAGE = "language";

    public static final int CHINA = 1;
    public static final int ENGLISH = 2;
    public static final int JAPANESE = 3;
    public static final int KOREAN = 4;


    public static int getLanguage() {
       return MMkvUtil.INSTANCE.decodeInt(LANGUAGE);
    }

    public static String getSelectLanguage() {
        int index = getLanguage();
        switch (index) {
            case CHINA:
                return "zh-CN";
            case ENGLISH:
                return "en-US";
            case JAPANESE:
                return "ja";
            case KOREAN:
                return "ko";
            default:
                return "zh-CN";
        }
    }



    public static void setApplicationLanguage(Context context, int index) {
        Locale locale = getLocale(index);
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        resources.updateConfiguration(config, dm);
        MMkvUtil.INSTANCE.encode(LANGUAGE,index);
    }

    private static Locale getLocale(int index) {

        switch (index) {
            case CHINA:
                return Locale.CHINA;
            case ENGLISH:
                return Locale.ENGLISH;
            case JAPANESE:
                return Locale.JAPANESE;
            case KOREAN:
                return Locale.KOREAN;
        }
        return Locale.CHINA;
    }

}
