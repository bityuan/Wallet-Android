package com.fzm.walletmodule.api

import android.text.TextUtils
import com.fzm.walletmodule.utils.MMkvUtil.decodeString

open class ApiEnv {

    companion object {

        var ONLINE = 0
        var DEBUG = 1
        var GO_URL = "go_url"
        var GO_URL_DEBUG = "http://172.16.100.116:8083"
        var GO_URL_ONLINE = "https://go.biqianbao.net"
        var GO_URL_ONLINE_IP = "https://47.242.7.153:8083"
        var BASE_URL = "https://www.bitfeel.cn"
        var goEnv = ONLINE

        @JvmStatic
        fun getGoURL(): String {
            return if (goEnv == ONLINE) {
                GO_URL_ONLINE_IP
            } else {
                GO_URL_DEBUG
            }
        }
    }
}