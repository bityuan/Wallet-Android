package com.fzm.walletmodule.utils;

import android.util.Log;

import com.fzm.wallet.sdk.db.entity.PWallet;
import com.fzm.wallet.sdk.utils.MMkvUtil;
import com.fzm.walletmodule.event.MainCloseEvent;

import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;

/**
 * @author zhengjy
 * @since 2022/01/07
 * Description:
 */
public class WalletUtils {
    public static long getUsingWalletId() {
        long id = MMkvUtil.INSTANCE.decodeLong(PWallet.PWALLET_ID);
        PWallet mPWallet;
        mPWallet = LitePal.find(PWallet.class, id);
        if (null == mPWallet) {
            mPWallet = LitePal.findFirst(PWallet.class);
            if (mPWallet != null) {
                setUsingWallet(mPWallet);
            } else {
                mPWallet = new PWallet();
                Log.e("nyb", "getUsingWalletId:post:MainCloseEvent");
                EventBus.getDefault().post(new MainCloseEvent());
            }
        }
        return mPWallet.getId();
    }

    public static long getUseWalletId() {
        long id = MMkvUtil.INSTANCE.decodeLong(PWallet.PWALLET_ID);
        PWallet mPWallet;
        mPWallet = LitePal.find(PWallet.class, id);
        if (null == mPWallet) {
            mPWallet = LitePal.findFirst(PWallet.class);
            if (mPWallet != null) {
                setUsingWallet(mPWallet);
            } else {
                mPWallet = new PWallet();
                mPWallet.setType(PWallet.TYPE_NONE);
            }
        }
        return mPWallet.getId();
    }

    public static PWallet getUsingWallet() {
        long id = MMkvUtil.INSTANCE.decodeLong(PWallet.PWALLET_ID);
        PWallet mPWallet;
        mPWallet = LitePal.find(PWallet.class, id);
        if (null == mPWallet) {
            mPWallet = LitePal.findFirst(PWallet.class);
            if (mPWallet != null) {
                setUsingWallet(mPWallet);
            } else {
                mPWallet = new PWallet();
                EventBus.getDefault().post(new MainCloseEvent());
            }
        }
        return mPWallet;
    }

    public static PWallet getUseWallet() {
        long id = MMkvUtil.INSTANCE.decodeLong(PWallet.PWALLET_ID);
        PWallet mPWallet;
        mPWallet = LitePal.find(PWallet.class, id);
        if (null == mPWallet) {
            mPWallet = LitePal.findFirst(PWallet.class);
            if (mPWallet != null) {
                setUsingWallet(mPWallet);
            } else {
                mPWallet = new PWallet();
                mPWallet.setType(PWallet.TYPE_NONE);
            }
        }
        return mPWallet;
    }

    public static void setUsingWallet(PWallet pWallet) {
        if (pWallet != null) {
            MMkvUtil.INSTANCE.encode(PWallet.PWALLET_ID, pWallet.getId());
        }
    }
}
