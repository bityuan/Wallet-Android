package com.fzm.wallet.sdk.api

open class ApiEnv {

    companion object {

        var ONLINE = 0
        var DEBUG = 1
        var GO_URL = "go_url"
        var GO_URL_DEBUG = "http://172.16.100.116:8083"
        var GO_URL_ONLINE = "https://go.biqianbao.net"
        var GO_URL_ONLINE_IP = "https://47.242.7.153:8083"
        var BASE_URL = "https://www.bitfeel.cn"
        //审核管理后台
        const val EXCHANGE_MANAGER = "http://13.114.69.134:8888"
        //真正闪兑的功能模块
        const val EXCHANGE_DO = "https://159.138.88.29:18084"
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