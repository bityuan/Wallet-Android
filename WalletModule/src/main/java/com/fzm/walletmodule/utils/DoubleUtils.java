package com.fzm.walletmodule.utils;

import java.math.BigDecimal;

/**
 * Created by ZX on 2018/6/14.
 */

public class DoubleUtils {
    public static int dotLength(String doubleValue) {
        String dotEnd = doubleValue.substring(doubleValue.indexOf(".") + 1, doubleValue.length());
        return dotEnd.length();
    }


    public static int doubleToInt(String doubleValue, int length) {
        double value = Double.parseDouble(doubleValue);
        double pow = Math.pow(10, length);
        double v = mul(value, pow);
        int intValue = (int) v;
        return intValue;
    }

    public static double mul(double d1, double d2) {
        BigDecimal bd1 = new BigDecimal(Double.toString(d1));
        BigDecimal bd2 = new BigDecimal(Double.toString(d2));
        return bd1.multiply(bd2).doubleValue();
    }

    public static double intToDouble(int intValue, int length) {
        double pow = Math.pow(10, length);
        double value = intValue / pow;
        return value;
    }

}
