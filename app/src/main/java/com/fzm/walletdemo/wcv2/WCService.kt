package com.fzm.walletdemo.wcv2

import android.app.Service
import android.content.Intent
import android.os.IBinder

class WCService: Service() {
    override fun onBind(p0: Intent?): IBinder? {
        // 实现绑定服务的逻辑
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 在服务启动时执行的逻辑
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        // 在服务销毁时执行的逻辑
        super.onDestroy()
    }
}