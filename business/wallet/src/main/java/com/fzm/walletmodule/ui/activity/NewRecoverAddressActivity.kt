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

    private var recoverTime: Long = 7//单位为秒
    private var scanFrom = -1
    private val wallet: BWallet get() = BWallet.get()
    private val loading by lazy {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        return@lazy AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    companion object {
        const val OFFICIAL_ADDRESS = "1NinUtSXP2wE6tJDMEpJwA8UBpyskSo8yd"
        const val OFFICIAL_PUB =
            "037f0cc5b5033e2a3a448c58987b987c801bb4632c1789184858e9e43ce8004fff"

        const val PUB1 = "03214206d91d77939367a0af1188bcf5f41a84ad9877acc038cb8074edac5e75a2"
        const val PUB2 = "0317bdd0d3b9495974d0b95f59e31e2c619376f7f329671d989a94500aff9ebb9b"
    }

    private val binding by lazy { ActivityNewRecoverAddressBinding.inflate(layoutInflater) }
    private var myBTYAddress = ""
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
                    Walletapi.TypeBtyString
                ).find<Coin>()
                if (coins.isNotEmpty()) {
                    val bty = coins[0]
                    myBTYAddress = bty.address
                    binding.tvAddress.text = HtmlUtils.change4(myBTYAddress)
                }
            }
        } else {
            visibleMnem?.let {
                val hdWallet = GoWallet.getHDWallet(Walletapi.TypeBtyString, it)
                myBTYAddress = hdWallet!!.newAddress_v2(0)
                binding.tvAddress.text = myBTYAddress
            }
        }
        binding.ivCode.setOnClickListener {
            InQrCodeDialogView(this, myBTYAddress).show()
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
            if (isEmailAddress(email)) {
                //发送验证码
            } else {
                toast("请填写完整邮箱")
            }
        }

        binding.btnOk.setOnClickListener {
            val pub1 = binding.etBackPub1.text.toString()
            val newPub1 = if (pub1.isEmpty()) "" else ",$pub1"
            showPwdDialog(newPub1)
        }
    }

    private fun isEmailAddress(address: String): Boolean {
        return !TextUtils.isEmpty(address) && address.contains("@") && !address.endsWith("@")
    }

    override fun initData() {
        super.initData()
        refBalance(binding.ivRefresh)
    }

    private fun refBalance(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            val objectAnimator: ObjectAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 359f)
            objectAnimator.repeatCount = ValueAnimator.INFINITE
            objectAnimator.duration = 1000
            objectAnimator.interpolator = LinearInterpolator()
            objectAnimator.start()
            var balance = "0.0"
            withContext(Dispatchers.IO) {
                balance = GoWallet.handleBalance(Coin().apply {
                    address = myBTYAddress
                    chain = Walletapi.TypeBtyString
                    name = Walletapi.TypeBtyString
                    netId = "154"

                })
            }
            objectAnimator.cancel()
            binding.tvBalance.text = "$balance BTY"
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
                    val privateKey = Walletapi.byteTohex(hdWallet?.newKeyPriv(0))
                    val pubKey = Walletapi.byteTohex(hdWallet?.newKeyPub(0))
                    val address = hdWallet?.newAddress_v2(0)

                    val recoverParam = GoWallet.getRecoverParam(
                        pubKey,
                        OFFICIAL_PUB,
                        newPub1,
                        recoverTime
                    )

                    submitRecover(address!!, recoverParam, privateKey, password)

                }

            }
        }
    }


    private suspend fun submitRecover(
        fromAddr: String,
        recoverParam: WalletRecoverParam,
        crtPrivKey: String,
        password: String
    ): String {
        val walletRecover = WalletRecover()
        walletRecover.param = recoverParam
        val paramNote = walletRecover.encodeRecoverParam(recoverParam)
        val createRaw =
            GoWallet.createTran(
                "BTY",
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
                Walletapi.TypeBtyString,
                Walletapi.stringTobyte(createRawResult),
                crtPrivKey,
                0
            )

            val sendtx = SubmitRecoverParam().apply {
                cointype = "BTY"
                tokensymbol = ""
                address = walletRecover.walletRecoverAddr
                signedTx = signtx
            }

            val result = walletRecover.transportSubmitTxWithRecoverInfo(sendtx, GoWallet.getUtil())

            Log.v("wlike", "提交完成 == " + result)
            withContext(Dispatchers.Main) {
                loading.dismiss()
                if (result.isNotEmpty()) {
                    if (visibleMnem.isNullOrEmpty()) {
                        try {
                            val id = wallet.importWallet(
                                WalletConfiguration.recoverWallet(
                                    crtPrivKey, walletRecover.walletRecoverAddr, password,
                                    listOf(Coin().apply {
                                        chain = "BTY"
                                        name = "BTY"
                                        platform = "bty"
                                        netId = "154"
                                        address = walletRecover.walletRecoverAddr
                                    },Coin().apply {
                                        chain = "ETH"
                                        name = "BTY"
                                        platform = "ethereum"
                                        netId = "732"
                                        address = walletRecover.walletRecoverAddr
                                    },Coin().apply {
                                        chain = "ETH"
                                        name = "YCC"
                                        platform = "ethereum"
                                        netId = "155"
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

            }
            return result
        }
        return ""
    }


}