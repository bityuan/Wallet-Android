package com.fzm.walletmodule.utils;

import android.text.TextUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * Created by ZX on 2018/6/12.
 */

public class DecimalUtils {

/*    public static final int ROUND_UP = 0;

    直接截断
    public static final int ROUND_DOWN = 1;

    public static final int ROUND_CEILING = 2;

    public static final int ROUND_FLOOR = 3;

    四舍五入
    public static final int ROUND_HALF_UP = 4;

    四舍五入
    public static final int ROUND_HALF_DOWN = 5;

    public static final int ROUND_HALF_EVEN = 6;

    public static final int ROUND_UNNECESSARY = 7;

    BigDecimal add(BigDecimal val) //BigDecimal 加法

    BigDecimal subtract (BigDecimal val) //BigDecimal 减法

    BigDecimal multiply (BigDecimal val)  //BigDecimal 乘法

    BigDecimal divide (BigDecimal val,RoundingMode mode)  除法*/


    public static BigDecimal format(int newScale, String value, int roundingMode) {
        if (!TextUtils.isEmpty(value)) {
            double dValue = Double.parseDouble(value);
            return format(newScale, dValue, roundingMode);
        }
        return null;
    }

    public static BigDecimal format(int newScale, long value, int roundingMode) {
        BigDecimal bigDecimal = new BigDecimal(value).setScale(newScale, roundingMode);
        return bigDecimal;
    }

    public static BigDecimal format(int newScale, double value, int roundingMode) {
        //这里要注意必须要BigDecimal bigDecimal = new BigDecimal(value).setScale(newScale, roundingMode);
        //不能
        // BigDecimal bigDecimal = new BigDecimal(value);
        // bigDecimal.setScale(newScale, roundingMode);
        BigDecimal bigDecimal = new BigDecimal(value).setScale(newScale, roundingMode);
        return bigDecimal;
    }

    public static BigDecimal format(int newScale, float value, int roundingMode) {
        BigDecimal bigDecimal = new BigDecimal(value).setScale(newScale, roundingMode);
        return bigDecimal;
    }

    public static BigDecimal format(int newScale, int value, int roundingMode) {
        BigDecimal bigDecimal = new BigDecimal(value).setScale(newScale, roundingMode);
        return bigDecimal;
    }


    public static String subZero(String s){
        if(s.indexOf(".") > 0){
            s = s.replaceAll("0+?$", "");//去掉多余的0
            s = s.replaceAll("[.]$", "");//如最后一位是.则去掉
        }
        return s;
    }


    public static String subZeroAndDot(String s) {
        if (TextUtils.isEmpty(s)) {
            return subZeroAndDot(0);
        }
//        if(s.indexOf(".") > 0){
//            s = s.replaceAll("0+?$", "");//去掉多余的0
//            s = s.replaceAll("[.]$", "");//如最后一位是.则去掉
//        }
//        return s;
        double count = 0;
        try {
            count = Double.parseDouble(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subZeroAndDot(count);
    }

    public static String subZeroAndDot(double s) {
        return subWithNum(s, 4);
    }

    public static String subZeroAndDot(float s) {
        return subWithNum(s, 4);
    }

    /**
     * 保留几位小数
     *
     * @param s
     * @param longNum
     * @return
     */
    public static String subWithNum(float s, int longNum) {
        BigDecimal bigDecimal = new BigDecimal(s + "");//一定要字符串入参，不然53031.586变成53031.5859
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(longNum);
        // 如果不需要四舍五入，可以使用RoundingMode.DOWN
        nf.setRoundingMode(RoundingMode.DOWN);
        nf.setGroupingUsed(false);
        String num = nf.format(bigDecimal);
        return num;
    }

    /**
     * 保留几位小数
     *
     * @param s
     * @param longNum
     * @return
     */
    public static String subWithNum(double s, int longNum) {
        BigDecimal bigDecimal = new BigDecimal(s + "");//一定要字符串入参，不然53031.586变成53031.5859
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(longNum);
        // 如果不需要四舍五入，可以使用RoundingMode.DOWN
        nf.setRoundingMode(RoundingMode.DOWN);
        nf.setGroupingUsed(false);
        String num = nf.format(bigDecimal);
        return num;
    }


    /**
     * 描述:  double类型是科学计数法不会影响计算 , 只是展示的时候要用string展示时需要去掉科学计数法,
     * 所以只要做double转String的去掉科学技术法就可以了
     * <p>
     * double类型转String的时候去掉科学计数法方法
     *
     * @param fee 手续费
     * @return
     */
    public static String formatDouble(double fee) {
        NumberFormat nf = NumberFormat.getInstance();
        //设置数的小数部分所允许的最大位数，避免小数位被舍掉
        nf.setMaximumFractionDigits(15);
        //设置数的小数部分所允许的最小位数，避免小数位有多余的0
        nf.setMinimumFractionDigits(0);
        //去掉科学计数法显示，避免显示为111,111,111,111
        nf.setGroupingUsed(false);
        String format = nf.format(fee);
        return format;
    }

}
