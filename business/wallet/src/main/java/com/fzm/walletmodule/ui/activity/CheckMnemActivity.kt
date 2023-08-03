package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import com.fzm.walletmodule.R
import android.text.TextUtils
import androidx.core.widget.doOnTextChanged
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.event.CheckMnemEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.LimitEditText
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.databinding.ActivityCheckMnemBinding
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.isFastClick
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
@Route(path = RouterPath.WALLET_CHECK_MNEM)
class CheckMnemActivity : BaseActivity() {
    @JvmField
    @Autowired(name = PWallet.PWALLET_ID)
    var walletid: Long = 0

    private val binding by lazy { ActivityCheckMnemBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = getString(R.string.check_mnem)
        binding.viewMnem.etMnem.setRegex(LimitEditText.REGEX_CHINESE_ENGLISH)
        binding.viewMnem.etMnem.doOnTextChanged { text, start, count, after ->
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
                            binding.viewMnem.etMnem.setText(stringBuffer.toString())
                            binding.viewMnem.etMnem.setSelection(stringBuffer.toString().length)
                        }
                    }
                }
            }
        }

        binding.btnCheck.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            val mnem = binding.viewMnem.etMnem.text.toString()
            if (mnem.isEmpty()) {
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
                val hdWallet = GoWallet.getHDWallet(Walletapi.TypeETHString, newMnem)
                runOnUiThread {
                    if (null == hdWallet) {
                        ToastUtils.show(
                            this@CheckMnemActivity,
                            getString(R.string.my_import_backup_none)
                        )
                        return@runOnUiThread
                    }
                    val pubkeyStr = GoWallet.encodeToStrings(hdWallet.newKeyPub(0))
                    val count =
                        LitePal.where("pubkey = ? and pwallet_id = ?", pubkeyStr, "$walletid")
                            .count<Coin>()
                    if (count > 0) {
                        ToastUtils.show(this@CheckMnemActivity, "校验成功")
                        startActivity<MnemPasswordActivity>(
                            PWallet.PWALLET_MNEM to newMnem,
                            PWallet.PWALLET_ID to walletid
                        )
                    } else {
                        ToastUtils.show(this@CheckMnemActivity, "校验失败")
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
