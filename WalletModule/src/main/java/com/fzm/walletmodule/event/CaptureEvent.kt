package com.fzm.walletmodule.event

import android.text.TextUtils
import android.util.Log
import com.fzm.walletmodule.base.Constants

class CaptureEvent(requstCode: Int, type: Int, text: String) {
    var type = 0
    var text: String = ""
    var requstCode = 0

    init {
        this.requstCode = requstCode
        this.type = type
        this.text = text
    }

}