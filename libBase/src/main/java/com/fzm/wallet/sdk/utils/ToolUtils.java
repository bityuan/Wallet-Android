package com.fzm.wallet.sdk.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.tencent.mmkv.MMKV;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhengfan on 2016/6/29.
 * Explain
 */
public class ToolUtils {

    private final static int MB = 1024 * 1024;
    public final static int FREE_SD_SPACE_NEEDED_TO_CACHE = 10;

    public static DecimalFormat mformat_1 = new DecimalFormat("##0.1");
    public static DecimalFormat mformat_2 = new DecimalFormat("##0.01");
    public static DecimalFormat mformat_3 = new DecimalFormat("##0.001");

    //计算百分比
    private static DecimalFormat mformat_0 = new DecimalFormat("##0");

    //计算日月
    private static DecimalFormat mformat_00 = new DecimalFormat("##00");

    public static String getString0(float f_data) {
        String s_data = mformat_0.format(f_data);
        return s_data;
    }

    public static String getString00(float f_data) {
        String s_data = mformat_00.format(f_data);
        return s_data;
    }

    public static String getString1(float f_data) {
        String s_data = mformat_1.format(f_data);
        return s_data;
    }

    public static String getString2(float f_data) {
        String s_data = mformat_2.format(f_data);
        return s_data;
    }

    public static String getString3(float f_data) {
        String s_data = mformat_3.format(f_data);
        return s_data;
    }

    /**
     * 得到当前应用版本名称的方法
     *
     * @param context :上下文
     * @throws Exception
     */
    public static int getVersionCode(Context context) throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        int versionCode = packInfo.versionCode;
        return versionCode;
    }

    /**
     * 得到当前应用版本名称的方法
     *
     * @param context :上下文
     * @throws Exception
     */
    public static String getVersionName(Context context) throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        String versionName = packInfo.versionName;
        return versionName;
    }


    /**
     * 判断qq是否可用
     *
     * @param context
     * @return
     */
    public static boolean isQQClientAvailable(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                String pn = pinfo.get(i).packageName;
                if (pn.equals("com.tencent.mobileqq")) {
                    return true;
                }
            }
        }
        return false;
    }


    // 判断是否为手机号
    public static boolean isPhone(String inputText) {
        Pattern pattern = Pattern.compile("1[0-9]{10}");
        Matcher matcher = pattern.matcher(inputText);
        if (matcher.matches()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 截取手机号后四位
     *
     * @param phoneNum
     * @return
     */
    public static String getPhoneShort(String phoneNum) {
        String phoneShort = phoneNum.substring(phoneNum.length() - 4, phoneNum.length());
        return phoneShort;
    }

    // 判断格式是否为email
    public static boolean isEmail(String email) {
        String str = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(email);
        return m.matches();
    }


    public static void setTextViewColor(Context context, TextView textView, int resourceId) {
        Resources resource = (Resources) context.getResources();
        ColorStateList csl = (ColorStateList) resource.getColorStateList(resourceId);
        if (csl != null) {
            textView.setTextColor(csl);
        }
    }

    public static void setRadioButtonColor(Context context, RadioButton radioButton, int resourceId) {
        Resources resource = (Resources) context.getResources();
        ColorStateList csl = (ColorStateList) resource.getColorStateList(resourceId);
        if (csl != null) {
            radioButton.setTextColor(csl);
        }
    }

    public static boolean stringNotEmpty(String string) {
        if (string != null && !string.equals("")) {
            return true;
        } else
            return false;
    }


    /**
     * 返回时间戳的时间
     *
     * @param time
     * @return
     */
    public static String getMilliToTimeLong(long time) {
        Date date = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.format(date);
    }


    /**
     * 返回时间戳的时间
     *
     * @param time
     * @return
     */
    public static String getMilliToTimeLongH_M(long time) {
        Date date = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        return formatter.format(date);
    }

    /**
     * 返回时间戳的日期
     *
     * @param time
     * @return
     */
    public static String getMilliToDateLong(long time) {
        Date date = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    /**
     * 返回时间戳的月和日
     *
     * @param time
     * @return
     */
    public static String getMilliToTimeLongM_D(long time) {
        Date date = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat("MM月dd日");
        return formatter.format(date);
    }

    /**
     * 返回时间戳的年月
     *
     * @param time
     * @return
     */
    public static String getMilliToDateLongY_M(long time) {
        Date date = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM");
        return formatter.format(date);
    }

    /**
     * 返回时间戳的月-日-时-分
     *
     * @param time
     * @return
     */
    public static String getMilliToDateLongM_D_H_M(long time) {
        Date date = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm");
        return formatter.format(date);
    }

    /**
     * 返回时间戳的时间和日期
     *
     * @param time
     * @return
     */
    public static String getMilliToDateAndTimeLong(long time) {
        Date date = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return formatter.format(date);
    }

    /**
     * 获取一年的年和月列表
     *
     * @param year  当前年，
     * @param mouth 当前月份
     * @return
     */
    public static ArrayList<String> getYearMouthList(int year, int mouth) {
        try {
            ArrayList<String> yearmouthList = new ArrayList<String>();
            for (int i = 0; i <= 12; i++) {
                if (mouth <= 0) {
                    mouth = 12;
                    year = year - 1;
                }
                StringBuilder stringBuilder = new StringBuilder(String.valueOf(year)).append("-").append(getString00(mouth));
                yearmouthList.add(stringBuilder.toString());
                mouth--;
            }
            return yearmouthList;
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 判断是否为今天(效率比较高)
     *
     * @param day 传入的 时间  "2016-06-28 10:10:30" "2016-06-28" 都可以
     * @return true今天 false不是
     * @throws ParseException
     */
    public static boolean IsToday(String day) throws ParseException {
        Calendar pre = Calendar.getInstance();
        Date predate = new Date(System.currentTimeMillis());
        pre.setTime(predate);

        Calendar cal = Calendar.getInstance();
        Date date = getDateFormat().parse(day);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) == (pre.get(Calendar.YEAR))) {
            int diffDay = cal.get(Calendar.DAY_OF_YEAR)
                    - pre.get(Calendar.DAY_OF_YEAR);
            if (diffDay == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为昨天(效率比较高)
     *
     * @param day 传入的 时间  "2016-06-28 10:10:30" "2016-06-28" 都可以
     * @return true今天 false不是
     * @throws ParseException
     */
    public static boolean IsYesterday(String day) throws ParseException {

        Calendar pre = Calendar.getInstance();
        Date predate = new Date(System.currentTimeMillis());
        pre.setTime(predate);

        Calendar cal = Calendar.getInstance();
        Date date = getDateFormat().parse(day);
        cal.setTime(date);

        if (cal.get(Calendar.YEAR) == (pre.get(Calendar.YEAR))) {
            int diffDay = cal.get(Calendar.DAY_OF_YEAR)
                    - pre.get(Calendar.DAY_OF_YEAR);

            if (diffDay == -1) {
                return true;
            }
        }
        return false;
    }


    public static SimpleDateFormat getDateFormat() {
        if (null == DateLocal.get()) {
            DateLocal.set(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA));
        }
        return DateLocal.get();
    }

    private static ThreadLocal<SimpleDateFormat> DateLocal = new ThreadLocal<SimpleDateFormat>();

    /**
     * 获取某年某个月有多少天
     *
     * @param year  当前年，
     * @param mouth 当前月份
     * @return
     */
    public static String getDayOfMouth(String year, String mouth) {
        String dayNum = "";
        if (mouth.equals("01") || mouth.equals("03") || mouth.equals("05") || mouth.equals("07") || mouth.equals("08") || mouth.equals("10") || mouth.equals("12")) {
            dayNum = "31";
        } else if (mouth.equals("04") || mouth.equals("06") || mouth.equals("09") || mouth.equals("11")) {
            dayNum = "30";
        } else if (mouth.equals("02")) {
            if (Integer.parseInt(year) % 100 == 0) {
                if (Integer.parseInt(year) % 400 == 0) {//取余
                    dayNum = "29";
                } else {
                    dayNum = "28";
                }
            } else {
                if (Integer.parseInt(year) % 4 == 0) {//取余
                    dayNum = "29";
                } else {
                    dayNum = "28";
                }
            }
        }
        return dayNum;
    }

    public static final void saveObject(String path, Object saveObject) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        File f = new File(path);
        try {
            fos = new FileOutputStream(f);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(saveObject);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {//finally就是无论程序是否正常结束，都会执行的代码
            try {
                if (oos != null) {//读写流必须关闭
                    oos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final Object restoreObject(String path) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Object object = null;
        File f = new File(path);
        if (!f.exists()) {
            return null;
        }
        try {
            fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);
            object = ois.readObject();
            return object;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return object;
    }


    /**
     * 获取系统的sdk版本
     *
     * @return
     */
    public static int getAndroidSDKVersion() {
        int version = 0;
        try {
            version = Integer.valueOf(android.os.Build.VERSION.SDK);
        } catch (NumberFormatException e) {

        }
        return version;
    }


    /**
     * 字符串截取小数点后number位
     *
     * @param charSequence
     * @param number
     * @return
     */
    public static CharSequence checkString(final EditText editText, CharSequence charSequence, int number) {
        if (charSequence.toString().contains(".")) {
            if (charSequence.length() - 1 - charSequence.toString().indexOf(".") > number) {
                charSequence = charSequence.toString().subSequence(0,
                        charSequence.toString().indexOf(".") + number + 1);
                editText.setText(charSequence);
                editText.setSelection(charSequence.length());
            }
        }
        return charSequence;
    }

    public static String getMobileRecord(String ifsub) {
        if (!stringNotEmpty(ifsub)) {
            return "";
        }
        String subString = "";
        switch (ifsub) {
            case "0":
                subString = "待付款";
                break;
            case "1":
                subString = "充值已完成";
                break;
            case "2":
                subString = "已退款";
                break;
            case "3":
                subString = "待退款";
                break;
            case "4":
                subString = "充值处理中";
                break;
            case "5":
                subString = "已撤单";
                break;
            case "6":
                subString = "已过期";
                break;
            default:
                break;
        }
        return subString;
    }


    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean saveMyBitmap(Bitmap bmp, String path) {
        File f = new File(path);
        try {
            f.createNewFile();
            FileOutputStream fOut = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            return true;
        } catch (Exception e) {
// TODO: handle exception
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断SD卡上是否有足够的剩余空间
     *
     * @return
     */
    public static boolean isEnoughFreeSpaceOnSD() {
        if (FREE_SD_SPACE_NEEDED_TO_CACHE > freeSpaceOnSD()) {
            return false;
        }
        return true;
    }

    /**
     * 计算sdcard上的剩余空间
     *
     * @return
     */
    public static int freeSpaceOnSD() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
                .getPath());
        double sdFreeMB = ((double) stat.getAvailableBlocks() * (double) stat
                .getBlockSize()) / MB;
        return (int) sdFreeMB;
    }


    /**
     * 计算缩放比例
     *
     * @param count
     * @return
     */
    public static float culcMaxscale(float count) {
        float max = 1;
        max = count / 127 * 3;
        return max;
    }


    /**
     * vol等级
     *
     * @param num
     * @return
     */
    public static String getVolUnit(float num) {

        //10的e次方
        int e = (int) Math.floor(Math.log10(num));
        if (e >= 8) {
            return "亿";
        } else if (e >= 4) {
            return "万";
        } else {
            return "";
        }
    }


    /**
     * 给提币地址加密
     */
    public static String encryptCoinAddress(String coinAddress) {
        if (TextUtils.isEmpty(coinAddress)) {
            return "";
        }
        int length = coinAddress.length();
        if (length > 20) {
            StringBuilder sb = new StringBuilder();
            sb.append(coinAddress.substring(0, 10));
            for (int i = 0; i < 6; i++) {
                sb.append("*");
            }
            sb.append(coinAddress.substring(length - 10, length));
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(coinAddress.substring(0, length - 1));
            for (int i = 0; i < 6; i++) {
                sb.append("*");
            }
            return sb.toString();
        }
    }

    /**
     * 给提币地址加密
     */
    public static String encryptBankcardCode(String bankcardCode) {
        if (TextUtils.isEmpty(bankcardCode)) {
            return "";
        }
        int length = bankcardCode.length();
        StringBuilder sb = new StringBuilder();
        sb.append(bankcardCode.substring(0, 4));
        for (int i = 4; i < length - 4; i++) {
            sb.append("*");
        }
        sb.append(bankcardCode.substring(length - 4, length));
        return sb.toString();
    }

    /**
     * 删除传入字符串的非数字字母字符
     */
    public static CharSequence checkNumberLetterString(EditText et, CharSequence s) {
        boolean flag = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= 48 && c <= 57) && !(c >= 65 && c <= 90) && !(c >= 97 && c <= 122)) {
                flag = true;
            }
        }
        if (flag) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if ((c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122)) {
                    sb.append(c);
                }
            }
            String s_result = sb.toString();
            et.setText(s_result);
            return s_result;
        } else {
            return s;
        }
    }

    /**
     * 3位一组计数法
     */
    public static String countNumBy3DigitSet(String number) {
        StringBuilder sb = new StringBuilder();
        int length_int = -1;
        if (number.contains(".")) {
            length_int = number.substring(0, number.indexOf(".")).length();
        } else {
            length_int = number.length();
        }
        if (length_int >= 4) {
            for (int i = 0; i < length_int; i++) {
                sb.append(number.charAt(i));
                if (i % 3 == (length_int - 1) % 3 && i != length_int - 1) {
                    sb.append(",");
                }
            }
        }
        if (number.contains(".")) {
            sb.append(number.substring(number.indexOf("."), number.length()));
        }
        return sb.toString();
    }

    /**
     * 防止科学计数法 转换数字到字符串
     */
    public static String getPlainNumStr(double num) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(100);// 小数点后有100位才会转换为科学计数法
        return nf.format(num).replace(",", "");// 转换的字符串还带","分割，要去掉
    }

    /**
     * 防止科学计数法 转换数字到字符串
     */
    public static String getPlainNumStr(String numStr) {
        return getPlainNumStr(Double.parseDouble(numStr));
    }


    /**
     * 将手机号的“-”和“ ”去掉
     * 前边的区号去掉
     *
     * @param phoneNum
     * @return
     */
    public static String getPhoneNum(String phoneNum) {
        if (TextUtils.isEmpty(phoneNum)) {
            return "";
        }
        String subString = "";
        try {
            String phoneNumTemp = phoneNum.replace("-", "").replace(" ", "");
            subString = phoneNumTemp.substring(phoneNumTemp.length() - 11, phoneNumTemp.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subString;
    }


    public static String getUSDTString(String amount) {
        return "$ " + amount;
    }

    public static String getRMBString(String amount) {
        return " ≈ ￥ " + amount;
    }


}

