package com.fzm.walletmodule.ui.activity

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.LIVE_KEY_WALLET
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.exception.ImportWalletException
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.databinding.ActivityNewRecoverAddressBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.InQrCodeDialogView
import com.fzm.walletmodule.utils.ClickUtils
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.HtmlUtils
import com.fzm.walletmodule.utils.ToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.SubmitRecoverParam
import walletapi.WalletRecover
import walletapi.WalletRecoverParam
import walletapi.Walletapi


@Route(path = RouterPath.WALLET_NEW_RECOVER_ADDRESS)
class NewRecoverAddressActivity : BaseActivity() {
    @JvmField
    @Autowired(name = PWallet.PWALLET_ID)
    var walletid: Long = 0

    @JvmField
    @Autowired(name = RouterPath.PARAM_WALLET)
    var mPWallet: PWallet? = null

    @JvmField
    @Autowired(name = RouterPath.PARAM_VISIBLE_MNEM)
    var visibleMnem: String? = null

    private var recoverTime: Long = 30//单位为秒
    private var scanFrom = -1
    private val wallet: BWallet get() = BWallet.get()

    private val loading by lazy {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        return@lazy AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    companion object {
        const val OFFICIAL_ADDRESS = "0xf440B6464600D83F6AbaeBFD2773Af9B1Fd8e9dd"
        const val OFFICIAL_PUB =
            "02059f401bfabd8e1c8cf099ced414fbe2fca5dae7e931a82d837c1dfd7ece17c9"

        const val PUB1 = "028af81cc6e1ad3f2d48c588b314f3d476074d03d40438c441b25f882e7bff915f"
        const val PUB2 = "03ac79d706f303a9033acb500ec3a941c9f2d6dbee5696d963a96a13d33f2c1029"

        //0是BTC格式，2是ETH格式
        const val ADDRESS_ID = 2
        //BTY是0，YCC是999
        const val CHAIN_ID = 999
    }

    private val binding by lazy { ActivityNewRecoverAddressBinding.inflate(layoutInflater) }
    private var myRecoverAddress = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = "设置找回参数"
        initView()
        initData()
        initObserver()
    }


    override fun initView() {
        super.initView()
        //设置页面Mnem为空
        if (visibleMnem.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val coins = LitePal.where(
                    "pwallet_id = ? and name = ?",
                    walletid.toString(),
                    Walletapi.TypeETHString
                ).find<Coin>()
                if (coins.isNotEmpty()) {
                    val eth = coins[0]
                    myRecoverAddress = eth.address
                    binding.tvAddress.text = HtmlUtils.change4(myRecoverAddress)
                }
            }
        } else {
            visibleMnem?.let {
                val hdWallet = GoWallet.getHDWallet(Walletapi.TypeETHString, it)
                myRecoverAddress = hdWallet!!.newAddress_v2(0)
                binding.tvAddress.text = myRecoverAddress
            }
        }
        binding.ivCode.setOnClickListener {
            InQrCodeDialogView(this, myRecoverAddress).show()
        }

        binding.ivRefresh.setOnClickListener {
            refBalance(it)
        }
        binding.tvBackAddressDefault.text = HtmlUtils.change4(OFFICIAL_ADDRESS)

        binding.tvBackAddressDefault.setOnClickListener {
            ClipboardUtils.clip(this, OFFICIAL_ADDRESS)
        }

        binding.etBackPub1.setText(PUB1)
        binding.ivScan.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            scanFrom = 1
            ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
        }

        binding.rgDay.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_7day -> {
                    recoverTime = 7
                }
                R.id.rb_30day -> {
                    recoverTime = 30
                }
                R.id.rb_90day -> {
                    recoverTime = 90
                }

            }
        }
        binding.tvSendCode.setOnClickListener {
            val email = binding.etEmail.text.toString()


            val addr = Walletapi.pubToAddress_v2("ETH",Walletapi.hexTobyte(OFFICIAL_PUB))
            Log.v("wlike","addr == "+addr)

            if (isEmailAddress(email)) {
                //发送验证码
            } else {
                toast("请填写完整邮箱")
            }
        }

        binding.btnOk.setOnClickListener {
            if (balance.toDouble() < 1.0) {
                toast("请先往您的地址充值1YCC保证找回账户成功创建")
                return@setOnClickListener
            }


            val pub1 = binding.etBackPub1.text.toString()
            val newPub1 = if (pub1.isEmpty()) "" else ",$pub1"
            if (visibleMnem.isNullOrEmpty()) {
                showPwdDialog(newPub1)
            } else {
                loading.show()
                lifecycleScope.launch(Dispatchers.IO) {
                    mPWallet?.let {
                        doRecoverParam(it.password, visibleMnem!!, newPub1)
                    }
                }

            }


        }
    }

    private fun isEmailAddress(address: String): Boolean {
        return !TextUtils.isEmpty(address) && address.contains("@") && !address.endsWith("@")
    }

    override fun initData() {
        super.initData()
        refBalance(binding.ivRefresh)
    }

    var balance = "0.0"
    private fun refBalance(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            val objectAnimator: ObjectAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 359f)
            objectAnimator.repeatCount = ValueAnimator.INFINITE
            objectAnimator.duration = 1000
            objectAnimator.interpolator = LinearInterpolator()
            objectAnimator.start()
            withContext(Dispatchers.IO) {
                balance = GoWallet.handleBalance(Coin().apply {
                    address = myRecoverAddress
                    chain = Walletapi.TypeYccString
                    name = ""
                    netId = "155"

                })
            }
            objectAnimator.cancel()
            binding.tvBalance.text = "$balance YCC"
        }
    }

    override fun initObserver() {
        super.initObserver()
        //扫一扫
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            when (scanFrom) {
                1 -> {
                    binding.etBackPub1.setText(scan)
                }
            }
        })
    }

    private fun showPwdDialog(newPub1: String) {
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
            lifecycleScope.launch(Dispatchers.IO) {
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
                    }

                    val bPassword = GoWallet.encPasswd(password)!!
                    val mnem: String = GoWallet.decMenm(bPassword, it.mnem)
                    doRecoverParam(password, mnem, newPub1)
                }

            }
        }
    }

    private suspend fun doRecoverParam(password: String, mnem: String, newPub1: String) {
        val hdWallet = GoWallet.getHDWallet(Walletapi.TypeETHString, mnem)
        val privateKey = Walletapi.byteTohex(hdWallet?.newKeyPriv(0))
        val pubKey = Walletapi.byteTohex(hdWallet?.newKeyPub(0))
        val address = hdWallet?.newAddress_v2(0)

        val recoverParam = GoWallet.getRecoverParam(
            pubKey,
            OFFICIAL_PUB,
            newPub1,
            recoverTime,
            ADDRESS_ID,
            CHAIN_ID
        )

        if (visibleMnem.isNullOrEmpty()) {
            createRecoverWallet(address!!, recoverParam, privateKey, password)
        } else {
            try {
                mPWallet?.let {
                    val id = BWallet.get().importWallet(
                        WalletConfiguration.mnemonicWallet(
                            visibleMnem!!,
                            it.name,
                            it.password,
                            Constants.getCoins()
                        )
                    )
                    if (id != MyWallet.ID_DEFAULT) {
                        createRecoverWallet(address!!, recoverParam, privateKey, password)
                    }
                }
            } catch (e: ImportWalletException) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    e.message?.let {
                        toast(it)
                    }
                }

            }


        }
    }

    private suspend fun createRecoverWallet(
        fromAddr: String,
        recoverParam: WalletRecoverParam,
        crtPrivKey: String,
        password: String
    ): String {
        val walletRecover = WalletRecover()
        walletRecover.param = recoverParam
        //检查是否已经存在找回钱包
        val addrCoin = LitePal.where("address = ?",walletRecover.walletRecoverAddr).find<Coin>()
        if(addrCoin.isNotEmpty()){
            withContext(Dispatchers.Main){
                toast("此找回钱包已存在")
                loading.dismiss()

            }
            return ""
        }



        val paramNote = walletRecover.encodeRecoverParam(recoverParam)
        val createRaw =
            GoWallet.createTran(
                Walletapi.TypeYccString,
                fromAddr,
                walletRecover.walletRecoverAddr,
                0.1,
                0.1,
                paramNote,
                ""
            )
        val stringResult = JSON.parseObject(createRaw, StringResult::class.java)
        val createRawResult: String? = stringResult.result
        if (!createRawResult.isNullOrEmpty()) {
            //签名交易
            val signtx = GoWallet.signTran(
                Walletapi.TypeYccString,
                Walletapi.stringTobyte(createRawResult),
                crtPrivKey,
                ADDRESS_ID
            )

            val sendtx = SubmitRecoverParam().apply {
                cointype = Walletapi.TypeYccString
                tokensymbol = ""
                address = walletRecover.walletRecoverAddr
                signedTx = signtx
            }
            try {
                val result =
                    walletRecover.transportSubmitTxWithRecoverInfo(sendtx, GoWallet.getUtil())
                Log.v("wlike", "提交完成 == " + result)
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    if (result.isNotEmpty()) {
                        try {
                            val id = wallet.importWallet(
                                WalletConfiguration.recoverWallet(
                                    crtPrivKey, walletRecover.walletRecoverAddr, password,
                                    listOf(
                                        Coin().apply {
                                            chain = "ETH"
                                            name = "ETH"
                                            platform = "ethereum"
                                            netId = "90"
                                            address = walletRecover.walletRecoverAddr
                                        },
                                        Coin().apply {
                                            chain = "ETH"
                                            name = "YCC"
                                            platform = "ethereum"
                                            netId = "155"
                                            address = walletRecover.walletRecoverAddr
                                        }, Coin().apply {
                                            chain = "ETH"
                                            name = "BTY"
                                            platform = "ethereum"
                                            netId = "732"
                                            address = walletRecover.walletRecoverAddr
                                        })
                                )
                            )

                            if (id != MyWallet.ID_DEFAULT) {
                                MyWallet.setId(id)
                                dismiss()
                                LiveEventBus.get<Long>(LIVE_KEY_WALLET).post(id)
                                toast("创建成功")
                                closeSomeActivitys()
                                finish()
                            }
                        } catch (e: ImportWalletException) {
                            dismiss()
                            e.message?.let { toast(it) }
                        }
                    }

                }
                return result
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.message?.let {
                        toast(it)
                    }
                }
            }


        }
        return ""
    }


}