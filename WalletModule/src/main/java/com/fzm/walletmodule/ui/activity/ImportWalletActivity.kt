package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.exception.ImportWalletException
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.event.CaptureEvent
import com.fzm.walletmodule.event.InitPasswordEvent
import com.fzm.walletmodule.event.MyWalletEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.LimitEditText
import com.fzm.walletmodule.utils.*
import com.snail.antifake.jni.EmulatorDetectUtil
import kotlinx.android.synthetic.main.activity_import_wallet.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_wallet)
        initData()
        initListener()
        initObserver()
    }

    override fun initData() {
        val count = LitePal.count<PWallet>()
        val name = getString(R.string.import_wallet_wallet_name) + (count + 1)
        walletName.setText(name)
        et_mnem.setRegex(LimitEditText.REGEX_CHINESE_ENGLISH)
    }

    override fun initListener() {
        et_mnem.doOnTextChanged { text, start, count, after ->
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
                            et_mnem.setText(stringBuffer.toString())
                            et_mnem.setSelection(stringBuffer.toString().length)
                        }
                    }
                }
            }
        }

        walletPassword.doOnTextChanged { text, start, count, after ->
            if (TextUtils.isEmpty(text)) {
                passwordTip.visibility = View.INVISIBLE
            } else {
                passwordTip.visibility = View.VISIBLE
                passwordTip.text = getString(R.string.set_wallet_password)
            }
        }
        walletPasswordAgain.doOnTextChanged { text, start, count, after ->
            if (TextUtils.isEmpty(text)) {
                passwordAgainTip.visibility = View.INVISIBLE
            } else {
                passwordAgainTip.visibility = View.VISIBLE
                passwordAgainTip.text = getString(R.string.confirm_wallet_password)
            }
        }

        walletName.doOnTextChanged { text, start, count, after ->
            importButtonState()
        }
        walletPassword.doOnTextChanged { text, start, count, after ->
            importButtonState()
        }
        walletPasswordAgain.doOnTextChanged { text, start, count, after ->
            importButtonState()
        }

        btnImport.setOnClickListener {
            hideKeyboard(btnImport)
            if (EmulatorDetectUtil.isEmulator(this)) {
                ToastUtils.show(this, "检测到您使用模拟器创建账户，请切换到真机")
            } else {
                finishTask()
            }

        }
    }


    private fun finishTask() {
        if (isFastClick()) {
            return
        }
        val name = walletName.text.toString()
        val password = walletPassword.text.toString()
        val passwordAgain = walletPasswordAgain.text.toString()
        val mnem = et_mnem.text.toString()
        if (checkMnem(mnem)) {
            if (checked(name, password, passwordAgain)) {
                EventBus.getDefault().post(InitPasswordEvent(password))
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
                        EventBus.getDefault().postSticky(MyWalletEvent(pWallet))
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
                btnImport.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_import_wallet_button_ok)
            } else {
                btnImport.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_import_wallet_button)
            }
        }
    }

    private val importButtonState: Boolean
        get() {
            if (TextUtils.isEmpty(et_mnem.text.toString())) {
                return false
            }
            if (TextUtils.isEmpty(walletPassword.text.toString())) {
                return false
            }
            if (TextUtils.isEmpty(walletPasswordAgain.text.toString())) {
                return false
            }
            if (TextUtils.isEmpty(walletName.text.toString())) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCaptureEvent(event: CaptureEvent) {
        if (event != null && event.type == CaptureCustomActivity.RESULT_SUCCESS) {
            et_mnem.setText(event.text)
        }
    }


    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}
