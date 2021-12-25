package com.fzm.walletmodule.base

import android.content.Context
import com.tencent.mmkv.MMKV
import org.litepal.LitePal

class WalletModuleApp {
    companion object {
        var context: Context? = null
        fun init(context: Context) {
            this.context = context
            MMKV.initialize(context)
            LitePal.initialize(context)

        }
    }

}