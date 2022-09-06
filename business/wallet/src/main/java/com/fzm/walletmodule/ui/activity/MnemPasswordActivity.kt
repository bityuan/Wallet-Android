package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils

import android.view.View
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.walletmodule.R
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.event.CheckMnemEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.AppUtils
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.isFastClick
import kotlinx.android.synthetic.main.activity_set_password.*


import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.doAsync

import org.jetbrains.anko.uiThread
import org.litepal.LitePal
import org.litepal.extension.find

class MnemPasswordActivity : BaseActivity() {
    private var mnem: String? = ""
    private var walletId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_password)
        mnem = intent.getStringExtra(PWallet.PWALLET_MNEM)
        walletId = intent.getLongExtra(PWallet.PWALLET_ID, -1)
        btn_sure.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            val newPassword = et_password.text.toString()
            val passwordAgain = et_password_again.text.toString()
            if (checked(newPassword, passwordAgain)) {
                showLoading()
                doAsync {
                    val pWallet = LitePal.find<PWallet>(MyWallet.getId())
                    val encPasswd = GoWallet.encPasswd(newPassword)
                    val passwdHash = GoWallet.passwdHash(encPasswd!!)
                    val encMnem = GoWallet.encMenm(encPasswd, mnem!!)
                    pWallet?.let {
                        it.password = passwdHash
                        it.mnem = encMnem
                        it.update(it.id)
                    }

                    uiThread {
                        dismiss()
                        ToastUtils.show(this@MnemPasswordActivity, "设置成功")
                        finish()
                        EventBus.getDefault().post(CheckMnemEvent())
                    }
                }

            }
        }

    }

    private fun checked(password: String, passwordAgain: String): Boolean {

        if (TextUtils.isEmpty(password) || !AppUtils.ispassWord(password)) {
            tv_number_and_letter.setTextColor(resources.getColor(R.color.red_common))
            return false
        } else {
            tv_number_and_letter.setTextColor(resources.getColor(R.color.gray_99))
        }


        if (TextUtils.isEmpty(passwordAgain) || !AppUtils.ispassWord(passwordAgain) || password != passwordAgain) {
            tv_different.visibility = View.VISIBLE
            return false
        } else {
            tv_different.visibility = View.INVISIBLE
        }

        return true
    }
}
