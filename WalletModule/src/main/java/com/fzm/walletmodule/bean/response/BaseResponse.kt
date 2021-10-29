package com.fzm.walletmodule.bean.response

import java.io.Serializable

open class BaseResponse<T> : Serializable {
     var id = 0
     var error: String? = null
     var result: T? = null

}