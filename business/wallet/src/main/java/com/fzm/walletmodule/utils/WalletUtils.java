package com.fzm.walletmodule.utils;

import android.text.TextUtils;

import com.fzm.wallet.sdk.db.entity.PWallet;
import com.fzm.wallet.sdk.utils.MMkvUtil;
import com.fzm.walletmodule.event.MainCloseEvent;
import com.jeremyliao.liveeventbus.LiveEventBus;

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
        String user = MMkvUtil.INSTANCE.decodeString("CURRENT_USER", "");
        String idStr = MMkvUtil.INSTANCE.decodeString(user + PWallet.PWALLET_ID, "");
        long id;
        if (TextUtils.isEmpty(idStr)) {
            id = MMkvUtil.INSTANCE.decodeLong(PWallet.PWALLET_ID);
        } else {
            id = Long.parseLong(idStr);
        }
        PWallet mPWallet;
        mPWallet = LitePal.find(PWallet.class, id);
        if (null == mPWallet) {
            mPWallet = LitePal.findFirst(PWallet.class);
            if (mPWallet == null) {
                mPWallet = new PWallet();
                EventBus.getDefault().post(new MainCloseEvent());
            }
        }
        return mPWallet;
    }

}
