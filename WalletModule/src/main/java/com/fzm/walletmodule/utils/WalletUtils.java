package com.fzm.walletmodule.utils;

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
@Deprecated
public class WalletUtils {

    @Deprecated
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

    @Deprecated
    public static void setUsingWallet(PWallet pWallet) {
        if (pWallet != null) {
            MMkvUtil.INSTANCE.encode(PWallet.PWALLET_ID, pWallet.getId());
        }
    }
}
