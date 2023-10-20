package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.BuildConfig
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityCreateWalletBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.*
import com.snail.antifake.jni.EmulatorDetectUtil
import org.litepal.LitePal
import org.litepal.LitePal.where
import org.litepal.extension.find

/**
 * 创建账户页面
 */
@Route(path = RouterPath.WALLET_CREATE_WALLET)
class CreateWalletActivity : BaseActivity() {
    private var viewHeight = 0
    private val binding by lazy { ActivityCreateWalletBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        initListener()
    }

    override fun initView() {
        super.initView()
        if(BuildConfig.DEBUG){
            binding.etName.setText("test")
            binding.etPassword.setText("12345678a")
            binding.etPasswordAgain.setText("12345678a")
        }
        binding.etName.setSelection(binding.etName.text.length)
        title = ""
        binding.btnCreate.viewTreeObserver.addOnPreDrawListener {
            viewHeight = binding.btnCreate.height
            true
        }
        setLineFocusChage(binding.etName, binding.lineName.root)
        setLineFocusChage(binding.etPassword, binding.linePassword.root)
        setLineFocusChage(binding.etPasswordAgain, binding.linePasswordAgain.root)
    }

    private fun setLineFocusChage(editText: EditText, lineView: View) {
        editText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                lineView.setBackgroundResource(R.color.color_333649)
                val pra: ViewGroup.LayoutParams = binding.lineName.root.layoutParams
                pra.height = ScreenUtils.dp2px(this, 1f)
                lineView.layoutParams = pra
            } else {
                lineView.setBackgroundResource(R.color.lineColor)
                val pra: ViewGroup.LayoutParams = binding.lineName.root.layoutParams
                pra.height = 1
                lineView.layoutParams = pra
            }
            if (v.id == R.id.et_password) {
                binding.tvPrompt.visibility = View.VISIBLE
            } else {
                binding.tvPrompt.visibility = View.INVISIBLE
            }
        }
    }

    override fun initListener() {
        super.initListener()
        binding.btnCreate.setOnClickListener {
            hideKeyboard(binding.btnCreate)
            if (isFastClick()) {
                return@setOnClickListener
            }
            gotoFinishTask()
        }
    }

    private fun gotoFinishTask() {
        if (EmulatorDetectUtil.isEmulator(this)) {
            ToastUtils.show(this, getString(R.string.check_str))
        } else {
            finishTask()
        }
    }

    private fun finishTask() {
        val name: String = binding.etName.text.toString()
        val password: String = binding.etPassword.text.toString()
        val passwordAgain: String = binding.etPasswordAgain.text.toString()
        if (checked(name, password, passwordAgain)) {
            val wallet = PWallet()
            wallet.name = name
            wallet.password = password
            ARouter.getInstance().build(RouterPath.WALLET_CREATE_MNEM)
                .withSerializable(RouterPath.PARAM_WALLET, wallet).navigation()
        }

    }

    private fun checked(name: String, password: String, passwordAgain: String): Boolean {
        val pWallets = where("name = ?", name).find<PWallet>()

        var checked = true

        if (TextUtils.isEmpty(name)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name), Gravity.CENTER)
            checked = false
        } else if (!ListUtils.isEmpty(pWallets)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name_exist), Gravity.CENTER)
            checked = false
        } else if (TextUtils.isEmpty(password)) {
            ToastUtils.show(this, getString(R.string.my_wallet_set_password), Gravity.CENTER)
            checked = false
        } else if (password.length < 8 || password.length > 16) {
            ToastUtils.show(this, getString(R.string.my_create_letter), Gravity.CENTER)
            checked = false
            binding.tvPrompt.setTextColor(resources.getColor(R.color.color_EA2551))
        } else if (TextUtils.isEmpty(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_change_password_again), Gravity.CENTER)
            checked = false
        } else if (password != passwordAgain) {
            ToastUtils.show(this, getString(R.string.my_set_password_different), Gravity.CENTER)
            binding.tvTipError.visibility = View.VISIBLE
            checked = false
        } else if (!AppUtils.ispassWord(password) || !AppUtils.ispassWord(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_set_password_number_letter), Gravity.CENTER)
            checked = false
        }
        return checked
    }


}