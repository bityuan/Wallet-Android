package com.fzm.walletmodule.utils;

import java.util.List;


public class ListUtils {

    public static <V> boolean isEquals(String str, List<V> sourceList) {
        if (!isEmpty(sourceList)) {
            for (int i = 0; i < sourceList.size(); i++) {
                if (sourceList.get(i).equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }


    public static <V> boolean isEmpty(List<V> sourceList) {
        return (sourceList == null || sourceList.size() == 0);
    }
}
