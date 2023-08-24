package com.fzm.walletdemo.web3.util;


import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.RawRes;

import org.web3j.utils.Numeric;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import timber.log.Timber;

public class Utils {
    public static String loadFile(Context context, @RawRes int rawRes) {
        byte[] buffer = new byte[0];
        try {
            InputStream in = context.getResources().openRawResource(rawRes);
            buffer = new byte[in.available()];
            int len = in.read(buffer);
            if (len < 1) {
                throw new IOException("Nothing is read.");
            }
        } catch (Exception ex) {
            Timber.tag("READ_JS_TAG").d(ex, "Ex");
        }

        try {
            Timber.tag("READ_JS_TAG").d("HeapSize:%s", Runtime.getRuntime().freeMemory());
            return new String(buffer);
        } catch (Exception e) {
            Timber.tag("READ_JS_TAG").d(e, "Ex");
        }
        return "";
    }


    public static BigInteger stringToBigInteger(String value)
    {
        if (TextUtils.isEmpty(value)) return BigInteger.ZERO;
        try
        {
            if (Numeric.containsHexPrefix(value))
            {
                return Numeric.toBigInt(value);
            }
            else
            {
                return new BigInteger(value);
            }
        }
        catch (NumberFormatException e)
        {
            Timber.e(e);
            return BigInteger.ZERO;
        }
    }
}
