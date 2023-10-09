package com.fzm.wallet.sdk.utils;

import android.text.TextUtils;

import walletapi.Walletapi;

public class AddressCheckUtils {


    public static boolean check(String chain, String toAddress) {
        switch (chain) {
            case Walletapi.TypeBitcoinString:
                return isBTCAddress(toAddress);
            case Walletapi.TypeBtyString:
                return isBTYAddress(toAddress);
            case Walletapi.TypeETHString:
            case Walletapi.TypeBnbString:
                return isETHAddress(toAddress);
            case Walletapi.TypeDcrString:
                return isDCRAddress(toAddress);
            case Walletapi.TypeTrxString:
                return isTRXAddress(toAddress);
        }

        return true;
    }


    public static boolean isBTCAddress(String input) {
        if (TextUtils.isEmpty(input) || input.length() < 26 || input.length() > 35) {
            return false;
        }
        if (input.startsWith("0x") || input.startsWith("Ds")) {
            return false;
        }

        return true;
    }


    public static boolean isBTYAddress(String input) {
        if (TextUtils.isEmpty(input)) {
            return false;
        }
        return true;
    }


    public static boolean isETHAddress(String input) {
        if (TextUtils.isEmpty(input) || !input.startsWith("0x") || input.length() != 42) {
            return false;
        }
        return true;
    }


    public static boolean isDCRAddress(String input) {
        if (!input.startsWith("Ds") || input.length() < 20) {
            return false;
        }
        return true;
    }
    public static boolean isTRXAddress(String input) {
        if (!input.startsWith("T") || input.length() < 20) {
            return false;
        }
        return true;
    }

}
