package com.fzm.nft.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.NFTViewModel
import com.fzm.nft.R
import com.fzm.nft.databinding.ActivityNftoutBinding
import com.fzm.nft.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.UrlConfig
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.StatusBarUtil
import com.fzm.walletmodule.event.CaptureEvent
import com.fzm.walletmodule.ui.widget.configWindow
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import walletapi.HDWallet
import walletapi.Walletapi


@Route(path = RouterPath.NFT_OUT)
class NFTOutActivity : AppCompatActivity() {
    private val binding by lazy { ActivityNftoutBinding.inflate(layoutInflater) }
    private val nftViewModel: NFTViewModel by inject(walletQualifier)
    private lateinit var privkey: String

    private val loading by lazy {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        return@lazy AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    @JvmField
    @Autowired
    var coin: Coin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        doBar()
        initObserver()
        coin?.let {

        }

        binding.ivScan.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
        }

        binding.btnOk.setOnClickListener {
            showPwdDialog()
        }
    }

    private fun doBar() {
        setSupportActionBar(binding.xbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.xbar.toolbar.setNavigationOnClickListener { onBackPressed() }
        val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
        view.fitsSystemWindows = true
        StatusBarUtil.StatusBarLightMode(this)
    }

    private fun initObserver() {
        nftViewModel.outNFT.observe(this, Observer {
            toast(it)

            val sign = GoWallet.signTran(
                Walletapi.TypeETHString,
                Walletapi.hexTobyte(it),
                privkey
            )
            sign?.let {
                val send = GoWallet.sendTran(Walletapi.TypeETHString, it, "", UrlConfig.GO_URL)
                Log.v("nft", "send = $send")
                toast("操作成功" + send)
                loading.dismiss()
                finish()
            }

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
            if (password.isNullOrEmpty()) {
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
                            toast("密码错误")
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
                    privkey = Walletapi.byteTohex(hdWallet?.newKeyPriv(0))
                    val address = hdWallet?.newAddress_v2(0)!!

                    coin?.let {
                        nftViewModel.outNFT(
                            Walletapi.TypeETHString,
                            "1",
                            it.contract_address,
                            address,
                            binding.etAddress.text.toString(),
                            0.01
                        )
                    }

                }

            }
        }
    }


    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        binding.xbar.toolbar.title = ""
        binding.xbar.tvToolbar.text = title
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: CaptureEvent) {
        binding.etAddress.setText(event.text)
        binding.etAddress.setSelection(event.text.length)
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