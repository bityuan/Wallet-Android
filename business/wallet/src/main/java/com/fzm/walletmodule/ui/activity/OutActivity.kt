package com.fzm.walletmodule.ui.activity


import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.AddressCheckUtils
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.RegularUtils
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityOutBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.RemarksTipsDialogView
import com.fzm.walletmodule.utils.ClickUtils
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.vm.OutViewModel
import com.fzm.walletmodule.vm.WalletViewModel
import com.google.gson.Gson
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.android.synthetic.main.activity_out.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import walletapi.Walletapi
import java.math.RoundingMode
import java.text.DecimalFormat

@Route(path = RouterPath.WALLET_OUT)
class OutActivity : BaseActivity() {

    private var toAddress: String = ""
    private lateinit var privkey: String
    private val outViewModel by viewModel<OutViewModel>(walletQualifier)
    private val walletViewModel by viewModel<WalletViewModel>(walletQualifier)
    private val binding by lazy { ActivityOutBinding.inflate(layoutInflater) }
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
        initView()
        initData()
        initListener()
    }


    override fun initView() {
        coin?.let {
            binding.tvCoinName.text = it.uiName + getString(R.string.home_transfer)
            binding.tvWalletName.text = it.getpWallet().name
            binding.tvBalance.text = "${it.balance} ${it.uiName}"
            if ("TRX" == it.chain) {
                binding.llOutMiner.visibility = View.GONE
            }
        }
    }

    private var min = 0
    private var fee = 0.0
    override fun initData() {
        outViewModel.getMiner.observe(this, Observer {
            dismiss()
            if (it.isSucceed()) {
                it.data()?.let { miner: Miner ->
                    val max = miner.high.toDouble().times(100000000).toInt()
                    min = miner.low.toDouble().times(100000000).toInt()
                    val maxValue = max.minus(min)
                    binding.seekbarFee.max = maxValue
                    binding.seekbarFee.progress = maxValue.div(2)

                }
            } else {
                ToastUtils.show(this, it.error())
            }
        })
        outViewModel.getMiner(coin?.chain!!)
        binding.seekbarFee.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value: Double = progress.plus(min).div(100000000.0000)
                //val rmb = eth.rmb.times(value)
                val format = DecimalFormat("0.####")
                //未保留小数的舍弃规则，RoundingMode.FLOOR表示直接舍弃。
                format.roundingMode = RoundingMode.FLOOR
                val formatValue = format.format(value)


                fee = value
                binding.tvFee.text = "$formatValue ${coin?.chain}"

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        //扫一扫
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            binding.tvToAddress.setText(scan)
        })
    }


    override fun initListener() {
        binding.ivChainTitle.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            RemarksTipsDialogView(this, false)
        }
        binding.ivScan.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
        }
        binding.btnOut.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            toAddress = binding.tvToAddress.text.toString()
            val money = binding.etMoney.text.toString()
            if (!checkAddressAndMoney(toAddress, money)) {
                return@setOnClickListener
            }
            if (RegularUtils.isAddress(toAddress)) {
                showPwdDialog()
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    loading.show()
                    walletViewModel.getDNSResolve.observe(this@OutActivity, Observer {
                        if (it.isSucceed()) {
                            it.data()?.let { list ->
                                if (list.isNotEmpty()) {
                                    loading.dismiss()
                                    toAddress = list[0]
                                    Log.v("zx", toAddress)
                                    showPwdDialog()
                                }
                            }
                        }
                    })
                    walletViewModel.getDNSResolve(1, toAddress, 0)
                }
            }


        }

    }

    private var addressId = -1
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
            val money = binding.etMoney.text.toString()
            val password = bindingDialog.etInput.text.toString()
            if (password.isNullOrEmpty()) {
                toast("请输入密码")
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                coin?.let {
                    withContext(Dispatchers.Main) {
                        loading.show()
                    }
                    val check = GoWallet.checkPasswd(password, it.getpWallet().password)
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

                    if (it.getpWallet().type == PWallet.TYPE_PRI_KEY) {
                        if ("YCC" == it.chain || "BTY" == it.chain) {
                            if ("ethereum" == it.platform) {
                                addressId = 2
                            } else if ("btc" == it.platform) {
                                addressId = 0
                            }
                        }
                        privkey = it.getPrivkey(password)
                    } else {
                        val bPassword = GoWallet.encPasswd(password)!!
                        val mnem: String = GoWallet.decMenm(bPassword, it.getpWallet().mnem)
                        if ("YCC" == it.chain || "BTY" == it.chain) {
                            if ("ethereum" == it.platform) {
                                addressId = 2
                                privkey = it.getPrivkey("ETH", mnem)
                            } else if ("btc" == it.platform) {
                                privkey = it.getPrivkey("BTC", mnem)
                                addressId = 0
                            }
                        } else {
                            privkey = it.getPrivkey(it.chain, mnem)
                        }
                    }

                    loading.dismiss()
                    handleTransactions(toAddress, money)


                }

            }
        }
    }


    private fun handleTransactions(toAddress: String, money: String) {
        coin?.let {
            val tokensymbol = if (it.name == it.chain) "" else it.name
            //构造交易
            val createRaw = GoWallet.createTran(
                it.chain,
                it.address,
                toAddress,
                money.toDouble(),
                fee,
                et_note.text.toString(),
                tokensymbol
            )
            val stringResult = JSON.parseObject(createRaw, StringResult::class.java)
            val createRawResult: String? = stringResult.result
            if (TextUtils.isEmpty(createRawResult)) {
                return
            }
            //签名交易
            val signtx = GoWallet.signTran(
                it.chain,
                Walletapi.stringTobyte(createRawResult),
                privkey,
                addressId
            )
            if (TextUtils.isEmpty(signtx)) {
                return
            }
            //发送交易
            val sendRawTransaction = GoWallet.sendTran(it.chain, signtx!!, tokensymbol)
            runOnUiThread {
                dismiss()
                val result: StringResult? = parseResult(sendRawTransaction!!)
                if (result == null) {
                    ToastUtils.show(this, getString(R.string.home_transfer_currency_fails))
                    finish()
                    return@runOnUiThread
                }
                if (!TextUtils.isEmpty(result.error)) {
                    ToastUtils.show(this, result.error)
                    finish()
                    return@runOnUiThread
                }
                ToastUtils.show(this, R.string.home_transfer_currency_success)
                finish()
            }
        }


    }


    private fun parseResult(json: String): StringResult? {
        return if (TextUtils.isEmpty(json)) {
            null
        } else Gson().fromJson(json, StringResult::class.java)
    }

    private fun checkAddressAndMoney(toAddress: String, moneyStr: String): Boolean {
        if (TextUtils.isEmpty(toAddress)) {
            ToastUtils.show(this, R.string.home_please_input_receipt_address)
            return false
        } else if (TextUtils.isEmpty(moneyStr)) {
            ToastUtils.show(this, R.string.home_please_input_amount)
            return false
        } else if (toAddress == coin?.address) {
            ToastUtils.show(this, R.string.home_receipt_send_address_is_same)
            return false
        } else if (!RegularUtils.isEnglish(toAddress)) {
            ToastUtils.show(this, R.string.home_receipt_address_is_illegal)
            return false
        } else if (!AddressCheckUtils.check(coin?.chain, toAddress)) {
            ToastUtils.show(this, getString(R.string.home_receipt_address_is_illegal))
            return false
        }
        return true
    }


}