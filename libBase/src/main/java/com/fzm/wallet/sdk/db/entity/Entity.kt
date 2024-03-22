package com.fzm.wallet.sdk.db.entity

import com.fzm.wallet.sdk.widget.sidebar.Indexable
import com.google.gson.annotations.Expose
import org.litepal.annotation.Column
import org.litepal.crud.LitePalSupport
import java.io.Serializable


open class KTBaseBean : LitePalSupport(), Serializable {
    val id: Long = 0
}

class Contacts : Indexable, KTBaseBean() {
    override fun getIndex(): String {
        return sortLetters
    }

    var nickName: String = ""
    var phone: String = ""
    var sortLetters: String = ""
    var addressList: List<Address> = mutableListOf()
    var status: Int = 0
}


class Address : KTBaseBean() {
    var contacts: Contacts? = null
    var name: String = ""
    var platform: String = ""
    var nickName: String = ""
    var address: String = ""

    //0未选中  1选中
    @Expose(serialize = false)
    var isChecked: Int = 0

    @Column(ignore = true)
    var isAdd:Boolean = false
}

class Node {
    var name: String? = ""
    var rpcUrl: String? = ""
    var chainId: Long? = 0
    var symbol: String? = ""
    var browser: String? = ""
}









