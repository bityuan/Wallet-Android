package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.UrlConfig
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityNewRecoverAddressBinding
import com.fzm.walletmodule.databinding.ActivityRecoverBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.ListUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.*

@Route(path = RouterPath.WALLET_RECOVER)
class RecoverActivity : BaseActivity() {

    @JvmField
    @Autowired(name = PWallet.PWALLET_ID)
    var walletid: Long = 0

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
        binding.etBackAddress.setText("1NinUtSXP2wE6tJDMEpJwA8UBpyskSo8yd")
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
                toast("请输入密码")
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                BWallet.get().getCurrentWallet()?.let {
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
                    doRecover(type,privkey)
                }

            }
        }
    }


    private fun doRecover(type:Int,privkey:String){
        val fromAddress = binding.etBackAddress.text.toString()
        val toAddress = binding.etToAddress.text.toString()
        val inputAmount = binding.etAmount.text.toString()
        val inputFee = binding.etFee.text.toString()

        //使用控制地址提取X资产
        val walletTx = WalletTx().apply {
            cointype = "BTY"
            tokenSymbol = ""
            tx = Txdata().apply {
                //from可以填写任何一个备份地址，控制地址提取填写""
                from = if (type == 1) "" else fromAddress
                amount = inputAmount.toDouble()
                fee = inputFee.toDouble()
                note = "找回test"
                to = toAddress
            }
            util = GoWallet.getUtil(UrlConfig.GO_URL)
        }

        val create = Walletapi.createRawTransaction(walletTx)
        val stringResult = JSON.parseObject(Walletapi.byteTostring(create), StringResult::class.java)
        val result = stringResult.result
        when(type){
            1->{
                ckrSend(result!!, privkey)
            }
            2->{
                backSend(result!!, privkey)
            }
        }


    }

    //控制地址提取资产
    private fun ckrSend(result: String, privkey: String) {
        val signtx =
            WalletRecover().signRecoverTxWithCtrKey(Walletapi.stringTobyte(result), privkey)
        val sendRawTransaction = GoWallet.sendTran("BTY", signtx, "")
    }

    //备份地址（找回地址）提取资产
    private fun backSend(result: String, privkey: String) {
        //找回地址提取资产
        val noneDelayTxParam = NoneDelayTxParam().apply {
            execer = "none"
            addressID = 0
            chainID = 0
            fee = 0.01
        }
        val walletRecover = WalletRecover()
        val noneDelaytx = walletRecover.createNoneDelayTx(noneDelayTxParam)
        val signtx2 = walletRecover.signRecoverTxWithBackupKey(
            Walletapi.stringTobyte(result),
            privkey,
            noneDelaytx
        )
        val sendRawTransaction2 = GoWallet.sendTran("BTY", signtx2, "")
    }

}