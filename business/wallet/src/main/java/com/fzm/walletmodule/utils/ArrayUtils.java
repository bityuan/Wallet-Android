package com.fzm.walletmodule.utils;

/**
 * Created by ZX on 2018/5/25.
 */

public class ArrayUtils {

    public static <V> boolean isEmpty(V[] sourceArray) {
        return (sourceArray == null || sourceArray.length == 0);
    }
}
