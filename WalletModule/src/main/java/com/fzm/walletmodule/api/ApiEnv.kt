package com.fzm.walletmodule.api

import android.text.TextUtils
import android.util.Log
import com.fzm.walletmodule.utils.MMkvUtil.decodeString

class ApiEnv {

    companion object {

        const val ONLINE = 0
        const val DEBUG = 1
        const val GO_URL = "go_url"
        const val GO_URL_DEBUG = "http://172.16.100.116:8083"
        const val GO_URL_ONLINE = "https://go.biqianbao.net"
        const val BASE_URL = "https://www.bitfeel.cn"
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