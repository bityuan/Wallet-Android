package com.fzm.wallet.sdk.utils;

import android.content.Context;

/**
 * Created by ljn on 2017/9/11.
 * Explain dp2px
 */
public class ScreenUtil {
    public static int dp2px(Context context, float dpValue) {
        float density = context.getResources().getDisplayMetrics().density;
        int pxValue = (int) (dpValue * density + 0.5f);
        return pxValue;
    }

    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
