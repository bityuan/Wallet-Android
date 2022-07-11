package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.LIVE_KEY_WALLET
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.exception.ImportWalletException
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.databinding.ActivityImportWalletBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.LimitEditText
import com.fzm.walletmodule.utils.AppUtils
import com.fzm.walletmodule.utils.ListUtils
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.isFastClick
import com.jeremyliao.liveeventbus.LiveEventBus
import com.snail.antifake.jni.EmulatorDetectUtil
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import org.litepal.LitePal
import org.litepal.extension.count
import org.litepal.extension.find

/**
 * 导入账户页面
 */
class ImportWalletActivity : BaseActivity() {

    private var isOK: Boolean = false

    private val wallet: BWallet get() = BWallet.get()
    private val binding by lazy { ActivityImportWalletBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initData()
        initListener()
        initObserver()
    }

    override fun initData() {
        val count = LitePal.count<PWallet>()
        val name = getString(R.string.import_wallet_wallet_name) + (count + 1)
        binding.etWalletName.setText(name)
        binding.etMnem.setRegex(LimitEditText.REGEX_CHINESE_ENGLISH)
    }

    override fun initListener() {
        binding.etMnem.doOnTextChanged { text, start, count, after ->
            importButtonState()
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
                            binding.etMnem.setText(stringBuffer.toString())
                            binding.etMnem.setSelection(stringBuffer.toString().length)
                        }
                    }
                }
            }
        }

        binding.etWalletPassword.doOnTextChanged { text, start, count, after ->
            if (TextUtils.isEmpty(text)) {
                binding.tvPasswordTip.visibility = View.INVISIBLE
            } else {
                binding.tvPasswordTip.visibility = View.VISIBLE
                binding.tvPasswordTip.text = getString(R.string.set_wallet_password)
            }
        }
        binding.etWalletPasswordAgain.doOnTextChanged { text, start, count, after ->
            if (TextUtils.isEmpty(text)) {
                binding.tvPasswordAgainTip.visibility = View.INVISIBLE
            } else {
                binding.tvPasswordAgainTip.visibility = View.VISIBLE
                binding.tvPasswordAgainTip.text = getString(R.string.confirm_wallet_password)
            }
        }

        binding.etWalletName.doOnTextChanged { text, start, count, after ->
            importButtonState()
        }
        binding.etWalletPassword.doOnTextChanged { text, start, count, after ->
            importButtonState()
        }
        binding.etWalletPasswordAgain.doOnTextChanged { text, start, count, after ->
            importButtonState()
        }

        binding.btnImport.setOnClickListener {
            hideKeyboard(binding.btnImport)
            if (EmulatorDetectUtil.isEmulator(this)) {
                ToastUtils.show(this, "检测到您使用模拟器创建账户，请切换到真机")
            } else {
                finishTask()
            }

        }
    }


    override fun initObserver() {
        super.initObserver()
        //扫一扫
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            binding.etMnem.setText(scan)
        })
    }


    private fun finishTask() {
        if (isFastClick()) {
            return
        }
        val name = binding.etWalletName.text.toString()
        val password = binding.etWalletPassword.text.toString()
        val passwordAgain = binding.etWalletPasswordAgain.text.toString()
        val mnem =  binding.etMnem.text.toString()
        if (checkMnem(mnem)) {
            if (checked(name, password, passwordAgain)) {
                lifecycleScope.launch {
                    try {
                        showLoading()
                        val id = wallet.importWallet(
                            WalletConfiguration.mnemonicWallet(
                                mnem,
                                name,
                                password,
                                "",
                                Constants.getCoins()
                            ), true
                        )
                        val pWallet = wallet.findWallet(id)
                        dismiss()
                        LiveEventBus.get<PWallet>(LIVE_KEY_WALLET).post(pWallet)
                        ToastUtils.show(this@ImportWalletActivity, getString(R.string.my_import_success))
                        closeSomeActivitys()
                        finish()
                    } catch (e: ImportWalletException) {
                        dismiss()
                        ToastUtils.show(this@ImportWalletActivity, e.message)
                    }
                }
            }
        }
    }


    private fun getChineseMnem(mnem: String): String {
        val afterString = mnem.replace(" ", "")
        val afterString2 = afterString.replace("\n", "")
        val value = afterString2.replace("", " ").trim()
        return value
    }

    private fun importButtonState() {
        val ok = importButtonState
        if (isOK == ok) {
            return
        } else {
            isOK = ok
            if (ok) {
                binding.btnImport.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_import_wallet_button_ok)
            } else {
                binding.btnImport.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_import_wallet_button)
            }
        }
    }

    private val importButtonState: Boolean
        get() {
            if (TextUtils.isEmpty(binding.etMnem.text.toString())) {
                return false
            }
            if (TextUtils.isEmpty(binding.etWalletPassword.text.toString())) {
                return false
            }
            if (TextUtils.isEmpty(binding.etWalletPasswordAgain.text.toString())) {
                return false
            }
            if (TextUtils.isEmpty(binding.etWalletName.text.toString())) {
                return false
            }
            return true
        }

    private fun checkMnem(mnem: String): Boolean {
        var checked = true
        if (TextUtils.isEmpty(mnem)) {
            ToastUtils.show(this, getString(R.string.my_import_backup_null))
            checked = false
        }
        return checked
    }


    private fun checked(name: String, password: String, passwordAgain: String): Boolean {
        val pWallets = LitePal.where("name = ?", name).find<PWallet>()
        var checked = true
        if (TextUtils.isEmpty(name)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name))
            checked = false
        } else if (!ListUtils.isEmpty(pWallets)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name_exist))
            checked = false
        } else if (TextUtils.isEmpty(password)) {
            ToastUtils.show(this, getString(R.string.my_wallet_set_password))
            checked = false
        } else if (TextUtils.isEmpty(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_change_password_again))
            checked = false
        } else if (password.length !in 8..16 || passwordAgain.length !in 8..16) {
            ToastUtils.show(this, getString(R.string.my_create_letter))
            checked = false
        } else if (password != passwordAgain) {
            ToastUtils.show(this, getString(R.string.my_set_password_different))
            checked = false
        } else if (!AppUtils.ispassWord(password) || !AppUtils.ispassWord(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_set_password_number_letter))
            checked = false
        }
        return checked
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuItem = menu.add(0, 1, 0, getString(R.string.my_scan))
        menuItem.setIcon(R.mipmap.import_wallet_right)
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            startActivity<CaptureCustomActivity>()
        }
        return super.onOptionsItemSelected(item)
    }
}
