package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import com.fzm.walletmodule.R
import android.text.TextUtils
import androidx.core.widget.doOnTextChanged
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.event.CheckMnemEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.LimitEditText
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.isFastClick
import kotlinx.android.synthetic.main.activity_check_mnem.*
import kotlinx.android.synthetic.main.view_import0.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.litepal.LitePal
import org.litepal.extension.count
import walletapi.Walletapi
/**
 * 忘记密码时验证助记词页面
 */
class CheckMnemActivity : BaseActivity() {
    private var walletId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_mnem)
        walletId = intent.getLongExtra(PWallet.PWALLET_ID, -1)
        tvTitle.text ="校验助记词"
        et_mnem.setRegex(LimitEditText.REGEX_CHINESE_ENGLISH)
        et_mnem.doOnTextChanged { text, start, count, after ->
            val lastString = text.toString()
            if (!TextUtils.isEmpty(lastString)) {
                val first = lastString.substring(0, 1)
                if (first.matches(LimitEditText.REGEX_CHINESE.toRegex())) {
                    val afterString = lastString.replace(" ".toRegex(), "")
                    if (!TextUtils.isEmpty(afterString)) {
                        val stringBuffer = StringBuffer()
                        for (i in afterString.indices) {
                            if ((i + 1) % 3 == 0) {
                                stringBuffer.append(afterString[i])
                                if (i != afterString.length - 1) {
                                    stringBuffer.append(" ")
                                }
                            } else {
                                stringBuffer.append(afterString[i])
                            }
                        }
                        if (!TextUtils.equals(lastString, stringBuffer)) {
                            et_mnem.setText(stringBuffer.toString())
                            et_mnem.setSelection(stringBuffer.toString().length)
                        }
                    }
                }
            }
        }

        btn_check.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            val mnem = et_mnem.text.toString()
            if(mnem.isEmpty()) {
                return@setOnClickListener
            }
            lateinit var newMnem: String
            val first = mnem.substring(0, 1)
            if (first.matches(LimitEditText.REGEX_CHINESE.toRegex())) {
                newMnem = getChineseMnem(mnem)
            } else {
                newMnem = mnem
            }
            doAsync {
                val hdWallet = GoWallet.getHDWallet(Walletapi.TypeBtyString, newMnem)
                runOnUiThread {
                    if (null == hdWallet) {
                        ToastUtils.show(this@CheckMnemActivity,getString(R.string.my_import_backup_none))
                        return@runOnUiThread
                    }
                    val pubkeyStr = GoWallet.encodeToStrings(hdWallet.newKeyPub(0))
                    val count = LitePal.where("pubkey = ? and pwallet_id = ?", pubkeyStr,"$walletId").count<Coin>()
                    if (count > 0) {
                        ToastUtils.show(this@CheckMnemActivity,"校验成功")
                        startActivity<MnemPasswordActivity>(PWallet.PWALLET_MNEM to newMnem, PWallet.PWALLET_ID to walletId)
                    } else {
                        ToastUtils.show(this@CheckMnemActivity,"校验失败")
                    }
                }
            }
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    private fun getChineseMnem(mnem: String): String {
        val afterString = mnem.replace(" ", "")
        val afterString2 = afterString.replace("\n", "")
        val value = afterString2.replace("", " ").trim()
        return value
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChooseChainEvent(event: CheckMnemEvent) {
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

}
