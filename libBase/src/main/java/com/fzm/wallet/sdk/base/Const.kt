package com.fzm.wallet.sdk.base

import android.util.Log
import com.fzm.wallet.sdk.BuildConfig
import com.fzm.wallet.sdk.utils.MMkvUtil

const val REGEX_CHINESE = "[\u4e00-\u9fa5]+"

const val LIVE_KEY_CHOOSE_CHAIN = "live_key_choose_chain"
const val LIVE_KEY_SCAN = "live_key_scan"
const val LIVE_KEY_WALLET = "live_key_wallet"
const val LIVE_KEY_FEE = "live_key_fee"
const val PRE_X_RECOVER = "X_RECOVER"
const val LIVE_WC_STATUS = "live_wc_status"
const val LIVE_WC_MODEL = "live_wc_model"
const val FEE_CUSTOM_POSITION = 33

fun logDebug(log: String) {
    if (BuildConfig.DEBUG) {
        Log.v("dao", log)
    }
}

class MyWallet {
    companion object {
        //当前钱包的ID
        private const val ID_KEY = "id_key"
        const val ID_DEFAULT: Long = -1
        private var id: Long = ID_DEFAULT

        fun getId(): Long {
            return if (id != ID_DEFAULT) {
                id
            } else {
                MMkvUtil.decodeLong(ID_KEY)
            }
        }

        fun setId(id: Long) {
            MMkvUtil.encode(ID_KEY, id)
            this.id = id
        }
    }
}


