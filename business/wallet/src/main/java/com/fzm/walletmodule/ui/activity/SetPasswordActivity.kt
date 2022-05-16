package com.fzm.walletmodule.ui.activity


import android.os.Bundle
import android.text.TextUtils
import android.view.View
import com.fzm.walletmodule.R
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.AppUtils
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.isFastClick
import kotlinx.android.synthetic.main.activity_set_password.*
import org.jetbrains.anko.doAsync
import org.litepal.LitePal.find

/**
 * 设置密码页面
 */
class SetPasswordActivity : BaseActivity() {
    private var mPWalletId: Long = 0
    private var mPWallet: PWallet? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_password)
        tvTitle.text = getString(R.string.title_set_pwd)
        initIntent()
        initListener()
    }

    override fun initIntent() {
        mPWalletId = intent.getLongExtra(SetPasswordActivity.PWALLET_ID, 0)
        mPWallet = find(PWallet::class.java, mPWalletId)
    }

    override fun initListener() {
        btn_sure.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            setPassword()
        }
    }

    /**
     * 设置密码
     */
    private fun setPassword() {
        val newPassword: String = et_password.text.toString().trim { it <= ' ' }
        val passwordAgain: String = et_password_again.text.toString().trim { it <= ' ' }
        if (checked(newPassword,passwordAgain)){
            showLoading()
            doAsync {
                val pWallet = PWallet()
                val encPasswd: ByteArray? = GoWallet.encPasswd(newPassword)
                val passwdHash: String? = GoWallet.passwdHash(encPasswd!!)
                pWallet.password = passwdHash
                //同时更改助记词的加密
                val bOldPassword = GoWallet.encPasswd(mPWallet!!.password)
                val mnem: String = GoWallet.decMenm(bOldPassword!!, mPWallet!!.mnem)
                val encMenm: String? = GoWallet.encMenm(encPasswd, mnem)
                pWallet.mnem = encMenm
                pWallet.isPutpassword = true
                pWallet.update(mPWalletId)
                runOnUiThread {
                    dismiss()
                    ToastUtils.show(this@SetPasswordActivity, getString(R.string.my_set_password))
                    finish()
                }
            }
        }
    }

    /**
     * 校验密码
     */
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


    companion object {
        const val PWALLET_ID = "pwallet_id"
    }
}