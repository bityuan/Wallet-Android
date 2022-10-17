package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.PRE_X_RECOVER
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GZipUtils
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.BuildConfig
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityCheckEmailBinding
import com.fzm.walletmodule.databinding.ActivityRecoverBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.jeremyliao.liveeventbus.LiveEventBus
import com.king.zxing.util.CodeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.koin.android.ext.android.bind
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.NoneDelayTxParam
import walletapi.WalletRecover
import walletapi.Walletapi

@Route(path = RouterPath.WALLET_CHECKEMAIL)
class CheckEmailActivity : BaseActivity() {
    private var chooseCoin = "BTY"

    private val loading by lazy {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        return@lazy AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private val binding by lazy { ActivityCheckEmailBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = "邮箱验证"
        initView()
        initListener()
    }

    override fun initView() {
        super.initView()
        binding.rgCoin.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_bty -> {
                    chooseCoin = "BTY"
                }
                R.id.rb_ycc -> {
                    chooseCoin = "YCC"
                }

            }
        }
        binding.btnRecover.setOnClickListener {
            showPwdDialog()
        }
        binding.ivScanXAddr.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
        }
    }

    override fun initListener() {
        super.initListener()
        //扫一扫
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            binding.etXAddress.setText(scan)

        })
    }

    private fun showPwdDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_pwd, null)
        val dialog = AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            show()
        }
        val bindingDialog = DialogPwdBinding.bind(view)
        bindingDialog.ivClose.setOnClickListener {
            dialog.dismiss()
        }
        bindingDialog.btnOk.setOnClickListener {
            val password = bindingDialog.etInput.text.toString()
            if (password.isEmpty()) {
                toast("请输入密码")
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val wallet = LitePal.find<PWallet>(MyWallet.getId())
                wallet?.let {
                    withContext(Dispatchers.Main) {
                        loading.show()
                    }
                    val check = GoWallet.checkPasswd(password, it.password)
                    if (!check) {
                        withContext(Dispatchers.Main) {
                            toast("密码不正确")
                            loading.dismiss()
                        }
                        return@let
                    }
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        if (!loading.isShowing) {
                            loading.show()
                        }

                    }

                    val bPassword = GoWallet.encPasswd(password)!!
                    val mnem: String = GoWallet.decMenm(bPassword, it.mnem)
                    val hdWallet = GoWallet.getHDWallet(Walletapi.TypeETHString, mnem)
                    val privkey = Walletapi.byteTohex(hdWallet?.newKeyPriv(0))
                    val address = hdWallet?.newAddress_v2(0)
                    doRecover(privkey)
                }

            }
        }
    }


    private suspend fun doRecover(privkey: String) {
        try {
            val xAddress = binding.etXAddress.text.toString()

            val walletRecoverParam = GoWallet.queryRecover(xAddress, chooseCoin)
            val wr = WalletRecover()
            val result = wr.eccDecrypt(privkey, walletRecoverParam.memo.encryptedContact)
            withContext(Dispatchers.Main) {
                loading.dismiss()
                binding.tvResult.text = Walletapi.byteTostring(result)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


}