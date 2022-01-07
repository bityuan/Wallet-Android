package com.fzm.wallet.sdk.db.entity

import com.google.gson.annotations.SerializedName
import org.litepal.crud.LitePalSupport
import java.io.Serializable

open class BaseBean : LitePalSupport(), Serializable {
    //随意命名，为了不和后台的id有冲突
    @SerializedName("lid")
    var id: Long = 0 //（数据的自增长id）
}