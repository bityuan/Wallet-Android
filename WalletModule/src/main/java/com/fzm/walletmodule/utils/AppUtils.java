package com.fzm.walletmodule.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;


import androidx.core.app.ActivityCompat;

import com.fzm.walletmodule.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AppUtils {
    private static final String TAG = AppUtils.class.getSimpleName();
    public static final String LAST_INSTALL_APK = "last_install_apk";
    public static String INTERFACE_VERSION_NEW = "2.0";
    public static String INTERFACE_VERSION_OLD = "1.0";


    public static String getAppVersion(Context ctx) {
        String appVersion;
        // 获取APP版本信息
        PackageManager packageManager = ctx.getPackageManager();
        PackageInfo packInfo;
        try {
            packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            appVersion = packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            appVersion = "2.0";
        }

        return appVersion;
    }

    public static int getVersionCode(Context ctx) {
        int versionCode = -1;
        PackageManager packageManager = ctx.getPackageManager();
        PackageInfo packInfo;
        try {
            packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            versionCode = packInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        return versionCode;
    }

    public static String getInterfaceAppVersion() {
        String appVersion = "2.0";

        return appVersion;
    }

    public static String getClient(Context ctx) {
        String client = "supercode," + getAppVersion(ctx) + ",android";
        return client;
    }

    public static String getToken(Context ctx) {
        // return PreferencesUtils.getString(ctx, GetLoginRespons.TOKEN);
        return "1dce448e-2b87-4804-a03c-213c1030adf0";
    }


    /*
     * check the app is installed
     */
    public static boolean isAviliblePackage(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        // 获取所有已安装程序的包信息
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pinfo.size(); i++) {
            if (pinfo.get(i).packageName.equalsIgnoreCase(packageName)) {
                return true;
            }
        }
        return false;
    }





    public static boolean isApkDebugable(Context ctx) {
        try {
            PackageInfo pkginfo;
            pkginfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            if (pkginfo != null) {
                ApplicationInfo info = pkginfo.applicationInfo;
                return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            }
        } catch (Exception e) {
            ;
        }
        return false;
    }


    public static boolean isTestApk(Context ctx) {
        if (isApkDebugable(ctx)) {
            return true;
        }
        if (!TextUtils.isEmpty(getAppVersion(ctx)) && getAppVersion(ctx).toLowerCase().contains("beta")) {
            return true;
        }
        return false;
    }

    public static int getScreenWidth(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    //禁止截屏
    public static void forbidScreenShot(Window window) {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }


    private static boolean checkPasswordLength(String password) {
        if (password.length() > 16 || password.length() < 8) {
            return false;
        }
        return true;
    }

    public static boolean ispassWord(String password) {
        if (!checkPasswordLength(password)) {
            return false;
        }
        Pattern p = Pattern.compile("^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{8,16}$");

        Matcher m = p.matcher(password);

        return m.matches();
    }

    //频繁请求处理
    // 两次点击按钮之间的点击间隔不能少于1000毫秒
    private static long lastClickTime;

    public static boolean isFastClick(int delayTime) {
        boolean flag = true;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= delayTime) {
            flag = false;
        }
        lastClickTime = curClickTime;
        return flag;
    }

    public static String subMobile(String mobile) {
        if (TextUtils.isEmpty(mobile)) {
            return "";
        }
        String subMobile = mobile.substring(0, 3) + "****" + mobile.substring(7, mobile.length());
        return subMobile;
    }

    private boolean isHttps(String url) {
        return url.startsWith("https://");
    }

    /**
     * 根据当前日期获得是星期几
     * time=yyyy-MM-dd
     *
     * @return
     */
    public static String getWeek(Context context, String time) {
        String Week = "";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(format.parse(time));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int wek = c.get(Calendar.DAY_OF_WEEK);

        if (wek == 1) {
            Week += context.getString(R.string.sunday);
        }
        if (wek == 2) {
            Week += context.getString(R.string.monday);
        }
        if (wek == 3) {
            Week += context.getString(R.string.tuesday);
        }
        if (wek == 4) {
            Week += context.getString(R.string.wednesday);
        }
        if (wek == 5) {
            Week += context.getString(R.string.thursday);
        }
        if (wek == 6) {
            Week += context.getString(R.string.friday);
        }
        if (wek == 7) {
            Week += context.getString(R.string.saturday);
        }
        return Week;
    }

    public static String getSHA1(Context context) {
        return getSign(context, "SHA1");
    }

    public static String getMD5(Context context) {
        return getSign(context, "MD5");
    }

    public static String getSHA256(Context context) {
        return getSign(context, "SHA256");
    }

    //代码获取sha1值
    private static String getSign(Context context, String algorithm) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length() - 1);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}
