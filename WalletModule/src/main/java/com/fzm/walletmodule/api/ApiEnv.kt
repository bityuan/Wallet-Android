package com.fzm.walletmodule.api

import android.text.TextUtils
import com.fzm.walletmodule.utils.MMkvUtil.decodeString

class ApiEnv {

    companion object{

        const val ONLINE = 0
        const val DEBUG = 1
        const val GO_URL = "go_url"
        const val GO_URL_DEBUG = "https://183.129.226.77:8083"
        const val GO_URL_ONLINE = "https://go.biqianbao.net"
        private const val goEnv = ONLINE
        @JvmStatic
        fun getGoURL(): String? {
            return if (goEnv == ONLINE) {
                val urls =
                    decodeString(GO_URL)
                if (!TextUtils.isEmpty(urls)) {
                    urls
                } else GO_URL_ONLINE
            } else {
                GO_URL_DEBUG
            }
        }
    }
}