package com.fzm.walletmodule.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.*
import com.snail.antifake.jni.EmulatorDetectUtil
import kotlinx.android.synthetic.main.activity_create_wallet.*
import org.litepal.LitePal

/**
 * 创建账户页面
 */
class CreateWalletActivity : BaseActivity() {
    private var viewHeight = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)
        initView()
        initListener()
    }

    override fun initView() {
        super.initView()
        et_name.setSelection(et_name.text.length)
        title = ""
        btn_create.viewTreeObserver.addOnPreDrawListener {
            viewHeight = btn_create.height
            true
        }
        setLineFocusChage(et_name, line_name)
        setLineFocusChage(et_password, line_password)
        setLineFocusChage(et_password_again, line_password_again)
    }

    private fun setLineFocusChage(editText: EditText, lineView: View) {
        editText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                lineView.setBackgroundResource(R.color.color_333649)
                val pra: ViewGroup.LayoutParams = line_name.layoutParams
                pra.height = ScreenUtils.dp2px(this, 1f)
                lineView.layoutParams = pra
            } else {
                lineView.setBackgroundResource(R.color.lineColor)
                val pra: ViewGroup.LayoutParams = line_name.layoutParams
                pra.height = 1
                lineView.layoutParams = pra
            }
            if (v.id == R.id.et_password) {
                tv_prompt.visibility = View.VISIBLE
            } else {
                tv_prompt.visibility = View.INVISIBLE
            }
        }
    }

    override fun initListener() {
        super.initListener()
        btn_create.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            hideKeyboard()
            gotoFinishTask()
        }
    }

    private fun gotoFinishTask() {
        if (EmulatorDetectUtil.isEmulator(this)) {
            ToastUtils.show(this, "检测到您使用模拟器创建账户，请切换到真机")
        } else {
            finishTask()
        }
    }

    private fun finishTask() {
        val name: String = et_name.text.toString().trim { it <= ' ' }
        val password: String = et_password.text.toString().trim { it <= ' ' }
        val passwordAgain: String = et_password_again.text.toString().trim { it <= ' ' }
        if (checked(name, password, passwordAgain)) {
            val intent = Intent(this, CreateMnemActivity::class.java)
            val wallet = PWallet()
            wallet.name = name
            wallet.password = password
            intent.putExtra(PWallet::class.java.simpleName, wallet)
            startActivity(intent)
        }

    }

    private fun checked(name: String, password: String, passwordAgain: String): Boolean {
        val pWallets = LitePal.where("name = ?", name).find(PWallet::class.java)

        var checked = true

        if (TextUtils.isEmpty(name)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name),Gravity.CENTER)
            checked = false
        } else if (!ListUtils.isEmpty(pWallets)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name_exist),Gravity.CENTER)
            checked = false
        } else if (TextUtils.isEmpty(password)) {
            ToastUtils.show(this, getString(R.string.my_wallet_set_password),Gravity.CENTER)
            checked = false
        } else if (password.length < 8 || password.length > 16) {
            ToastUtils.show(this, getString(R.string.my_create_letter),Gravity.CENTER)
            checked = false
            tv_prompt.setTextColor(resources.getColor(R.color.color_EA2551))
        } else if (TextUtils.isEmpty(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_change_password_again),Gravity.CENTER)
            checked = false
        } else if (password != passwordAgain) {
            ToastUtils.show(this, getString(R.string.my_set_password_different),Gravity.CENTER)
            tv_tip_error.visibility = View.VISIBLE
            checked = false
        } else if (!AppUtils.ispassWord(password) || !AppUtils.ispassWord(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_set_password_number_letter),Gravity.CENTER)
            checked = false
        }
        return checked
    }


}