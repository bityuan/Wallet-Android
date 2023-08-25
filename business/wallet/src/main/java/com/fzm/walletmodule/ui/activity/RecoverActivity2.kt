/*
package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityRecoverBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.NoneDelayTxParam
import walletapi.WalletRecover
import walletapi.Walletapi

@Route(path = RouterPath.WALLET_RECOVER)
class RecoverActivity2 : BaseActivity() {

    private val loading by lazy {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        return@lazy AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private val binding by lazy { ActivityRecoverBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = "找回资产"
        initView()
    }

    override fun initView() {
        super.initView()
        binding.etToAddress.setText("1P7P4v3kL39zugQgDDLRqxzGjQd7aEbfKs")
        binding.etAmount.setText("0.02")
        binding.etFee.setText("0.001")
        binding.btnOut.setOnClickListener {
            showPwdDialog(1)
        }
        binding.btnRecover.setOnClickListener {
            showPwdDialog(2)
        }
    }

    private fun showPwdDialog(type: Int) {
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
                toast(getString(R.string.my_wallet_password_tips))
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
                    val hdWallet = GoWallet.getHDWallet(Walletapi.TypeBtyString, mnem)
                    val privkey = Walletapi.byteTohex(hdWallet?.newKeyPriv(0))
                    val address = hdWallet?.newAddress_v2(0)
                    //当前钱包为控制地址，那么prikey就是控制地址的
                    //当前钱包为备份地址，那么prikey就是备份地址的
                    //所以我们只需要获取当前钱包的私钥即可
                    loading.dismiss()
                    doRecover(type, privkey)
                }

            }
        }
    }

    private fun doRecover(type: Int, privkey: String) {
        val toAddress = binding.etToAddress.text.toString()
        val inputAmount = binding.etAmount.text.toString()
        val inputFee = binding.etFee.text.toString()
        val xAddress = binding.etXAddress.text.toString()

        val walletRecoverParam = GoWallet.queryRecover(xAddress)
        val walletRecover = WalletRecover()
        walletRecover.param = walletRecoverParam
        val createRaw = GoWallet.createTran(
            "BTY",
            xAddress,
            toAddress,
            inputAmount.toDouble(),
            inputFee.toDouble(),
            "zh测试",
            ""
        )
        val strResult = JSON.parseObject(createRaw, StringResult::class.java)
        val createRawResult: String? = strResult.result
        if (!createRawResult.isNullOrEmpty()) {
            when (type) {
                1 -> {
                    ckrSend(walletRecover, createRawResult, privkey)
                }
                2 -> {
                    backSend(walletRecover, createRawResult, privkey)
                }
            }

        }
    }

    //控制地址提取资产
    private fun ckrSend(walletRecover: WalletRecover, result: String, privkey: String) {
        val signtx =
            walletRecover.signRecoverTxWithCtrKey(Walletapi.stringTobyte(result), privkey)
        val sendRawTransaction = GoWallet.sendTran("BTY", signtx, "")
        Log.v("wlike", "控制地址找回 == " + sendRawTransaction)
    }

    //备份地址（找回地址）提取资产
    private fun backSend(walletRecover: WalletRecover, result: String, privkey: String) {
        val noneDelayTxParam = NoneDelayTxParam().apply {
            execer = "none"
            addressID = 0
            chainID = 0
            fee = 0.003
        }
        val noneDelaytx = walletRecover.createNoneDelayTx(noneDelayTxParam)
        val signtx2 = walletRecover.signRecoverTxWithRecoverKey(
            Walletapi.stringTobyte(result),
            privkey,
            noneDelaytx
        )
        val sendRawTransaction2 = GoWallet.sendTran("BTY", signtx2, "")
        Log.v("wlike", "备份找回 == " + sendRawTransaction2)
    }

}*/
