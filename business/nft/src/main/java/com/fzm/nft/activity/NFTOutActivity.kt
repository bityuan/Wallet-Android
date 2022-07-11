package com.fzm.nft.activity

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.NFTViewModel
import com.fzm.nft.R
import com.fzm.nft.databinding.ActivityNftoutBinding
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.UrlConfig
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.StatusBarUtil
import com.fzm.walletmodule.vm.OutViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import walletapi.Walletapi
import java.math.RoundingMode
import java.text.DecimalFormat


@Route(path = RouterPath.NFT_OUT)
class NFTOutActivity : AppCompatActivity() {
    private val binding by lazy { ActivityNftoutBinding.inflate(layoutInflater) }
    private val nftViewModel: NFTViewModel by inject(walletQualifier)
    private lateinit var privkey: String
    private var nftListDialog: AlertDialog? = null
    private var tokenID: String = ""
    private val outViewModel: OutViewModel by inject(walletQualifier)

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
            binding.tvBalance.text = " 余额：${it.balance}"
        }

        binding.ivScan.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
        }

        binding.btnOk.setOnClickListener {
            showPwdDialog()
        }

        binding.tvChooseNftid.setOnClickListener {
            coin?.let {
                nftViewModel.getNFTList(it.contract_address, it.address)
            }
        }

        outViewModel.getMiner(Walletapi.TypeETHString)
        binding.seekbarFee.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value: Double = progress.plus(min).div(100000000.00)
                //val rmb = eth.rmb.times(value)
                val format = DecimalFormat("0.##")
                //未保留小数的舍弃规则，RoundingMode.FLOOR表示直接舍弃。
                format.roundingMode = RoundingMode.FLOOR
                binding.tvFee.text = "$value ETH"

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

    }

    private fun doBar() {
        setSupportActionBar(binding.xbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.xbar.toolbar.setNavigationOnClickListener { onBackPressed() }
        val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
        view.fitsSystemWindows = true
        StatusBarUtil.StatusBarLightMode(this)
    }


    private var min = 0
    private fun initObserver() {
        outViewModel.getMiner.observe(this, Observer {
            if (it.isSucceed()) {
                it.data()?.let { miner: Miner ->
                    val max = miner.high.toDouble().times(100000000).toInt()
                    min = miner.low.toDouble().times(100000000).toInt()
                    val maxValue = max.minus(min)
                    binding.seekbarFee.max = maxValue
                    binding.seekbarFee.progress = maxValue.div(2)

                }

            } else {
                it.error()
            }
        })
        nftViewModel.outNFT.observe(this, Observer { createHash: String ->
            val sign = GoWallet.signTran(
                Walletapi.TypeETHString,
                Walletapi.hexTobyte(createHash),
                privkey
            )
            sign?.let { signHash ->
                val send =
                    GoWallet.sendTran(Walletapi.TypeETHString, signHash, "", UrlConfig.GO_URL)
                Log.v("nft", "send = $send")
                toast("操作成功" + send)
                loading.dismiss()
                finish()
            }

        })


        nftViewModel.getNFTList.observe(this, Observer {
            val items = it.toTypedArray()
            if (nftListDialog == null) {
                nftListDialog = AlertDialog.Builder(this)
                    .setItems(items, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            tokenID = items[which]
                            binding.tvChooseNftid.text = "选择编号：$tokenID"
                        }

                    }).create()
            }
            nftListDialog?.show()
        })

        //扫一扫
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            binding.etAddress.setText(scan)
            binding.etAddress.setSelection(scan.length)
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
                            tokenID,
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
}