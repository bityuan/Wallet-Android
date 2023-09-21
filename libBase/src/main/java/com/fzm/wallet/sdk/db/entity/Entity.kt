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
    companion object {
        val NICK_NAME = "nickName"
        val SIMPLE_NAME = "simpleName"
    }


    override fun getIndex(): String {
        return sortLetters
    }

    lateinit var nickName: String
    lateinit var phone: String
    lateinit var sortLetters: String
    lateinit var addressList: List<Address>
    var status: Int = 0
}


class Address : KTBaseBean() {
    lateinit var contacts: Contacts
    lateinit var cointype: String
    var platform: String = ""
    var address: String = ""

    //0未选中  1选中
    @Expose(serialize = false)
    var isChecked: Int = 0

    @Column(ignore = true)
    var isAdd:Boolean = false
}









