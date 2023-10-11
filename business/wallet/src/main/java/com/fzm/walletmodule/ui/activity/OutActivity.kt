package com.fzm.walletmodule.ui.activity


import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.IPConfig.Companion.BTY_FEE
import com.fzm.wallet.sdk.IPConfig.Companion.BTY_PR
import com.fzm.wallet.sdk.IPConfig.Companion.TOKEN_FEE
import com.fzm.wallet.sdk.IPConfig.Companion.YBF_BTY_PR
import com.fzm.wallet.sdk.IPConfig.Companion.YBF_FEE_ADDR
import com.fzm.wallet.sdk.IPConfig.Companion.YBF_TOKEN_FEE
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.AddressCheckUtils
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.ListUtils
import com.fzm.wallet.sdk.utils.MMkvUtil
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
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.MultiItemTypeAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.android.synthetic.main.activity_out.*
import kotlinx.android.synthetic.main.item_text.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.litepal.LitePal.where
import org.litepal.extension.find
import walletapi.GsendTx
import walletapi.WalletRecover
import walletapi.Walletapi
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale

//原BTY,BTC和ETH格式的BTY，查余额，账单，构造签名发送都是 "BTY",BNB的BTY是"BNB"
//原YCC查余额，账单，构造签名发送都是"ETH",BTC和ETH格式的YCC，查余额，账单，构造签名发送都是 "YCC",BNB的YCC是"BNB"
@Route(path = RouterPath.WALLET_OUT)
class OutActivity : BaseActivity() {

    private var toAddress: String = ""
    private lateinit var privkey: String
    private val outViewModel by viewModel<OutViewModel>(walletQualifier)
    private val walletViewModel by viewModel<WalletViewModel>(walletQualifier)
    private val walletRepository: WalletRepository by inject(walletQualifier)
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

    @JvmField
    @Autowired(name = RouterPath.PARAM_ADDRESS)
    var address: String? = null

    //主链余额
    var chainBalance: Double = 0.0
    private var oldName = ""
    private lateinit var coinToken: GoWallet.Companion.CoinToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initObserver()
        initView()
        initData()
        initListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val address = intent.getStringExtra(RouterPath.PARAM_ADDRESS)
        binding.etToAddress.setText(address)
    }

    override fun initObserver() {
        super.initObserver()
    }


    override fun initView() {
        coin?.let {
            oldName = it.name
            it.oldName = oldName
            title = "${it.uiName}(${it.nickname})${getString(R.string.home_transfer)}"
            binding.tvBalance.text = "${it.balance} ${it.uiName}"
            coinToken = it.newChain
            //危险操作，此页面不可对coin进行数据库修改，不然chain和name也会被修改
            it.chain = coinToken.cointype
            it.name = coinToken.tokenSymbol
            //替换好主链后再去查询主链余额，此操作只为判断矿工费使用,加个地址以判断不同主链,再测试下其他币种
            lifecycleScope.launch(Dispatchers.IO) {
                val chainBeans = where(
                    "name = ? and pwallet_id = ? and address = ?",
                    it.chain,
                    java.lang.String.valueOf(it.getpWallet().id),
                    it.address
                ).find<Coin>()
                withContext(Dispatchers.Main) {
                    if (!ListUtils.isEmpty(chainBeans)) {
                        chainBalance = chainBeans[0].balance.toDouble()
                    }
                }
            }

            binding.tvWalletName.text = it.getpWallet().name
            if ("TRX" == it.chain) {
                binding.llOutMiner.visibility = View.GONE
            }

            binding.etToAddress.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    val input = binding.etToAddress.text.toString()
                    if (input.isNotEmpty()) {
                        handleDNS(input)
                    }

                }
            }

            if (!address.isNullOrEmpty()) {
                binding.etToAddress.setText(address)
            }
        }
    }

    private fun handleDNS(input: String) {
        if (input.contains(".") && oldName in listOf("BTY","BTC","ETH","TRX","BNB")) {
            getAddressByDns(input)
        } else {
            getDnsByAddress(input)
        }
    }

    //通过地址查询域名为反向解析kind=1，只有地址类型才支持
    private fun getDnsByAddress(address: String) {
        walletViewModel.getDNSResolve(key = address, kind = 1)

    }

    //通过域名查询地址为正向解析kind=0，类型不重要
    private fun getAddressByDns(dns: String) {
        val newDNS = getDNS(dns)
        if (newDNS.isEmpty()) {
            toast(getString(R.string.home_receipt_address_is_illegal))
            return
        }
        walletViewModel.getDNSResolve(key = newDNS, kind = 0)
    }

    private fun getDNS(dns: String): String {
        val count = dns.count { it == '.' }
        //至少要有一个.
        if (count >= 2) {
            return dns
        } else if (count == 1) {
            return "${oldName.lowercase()}.$dns"
        }
        return ""
    }

    private var min = 0
    private var fee = 0.0
    override fun initData() {
        walletViewModel.getDNSResolve.observe(this, Observer {
            if (it.isSucceed()) {
                it.data()?.let { list ->
                    showDnsList(list)
                }
            }
        })
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
        val minerChain =
            if (coin?.chain == "ETH" && coin?.name != "ETH") "ETHTOKEN" else coin?.chain
        if (coinToken.proxy) {
            binding.seekbarFee.visibility = View.GONE
            binding.llVMiner.visibility = View.GONE
            binding.tvFee.text =
                "${if (coin?.platform == IPConfig.YBF_CHAIN) YBF_TOKEN_FEE else TOKEN_FEE} $oldName"
        } else {
            outViewModel.getMiner(minerChain!!)
        }
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
            handleDNS(scan)
            binding.etToAddress.setText(scan)
        })
    }


    private fun showDnsList(list: List<String>) {
        try {
            if (list.isNotEmpty()) {
                binding.rvDnsList.visibility = View.VISIBLE
                binding.rvDnsList.layoutManager = LinearLayoutManager(this)
                val dnsAdapter = object : CommonAdapter<String>(this, R.layout.item_text, list) {
                    override fun convert(
                        holder: ViewHolder, t: String, position: Int
                    ) {
                        holder.setText(R.id.tv_text, t)

                    }

                }
                binding.rvDnsList.adapter = dnsAdapter
                dnsAdapter.setOnItemClickListener(object :
                    MultiItemTypeAdapter.OnItemClickListener {
                    override fun onItemClick(
                        view: View, viewHolder: RecyclerView.ViewHolder, position: Int
                    ) {
                        val item = list[position]
                        binding.etToAddress.setText(item)
                        binding.etToAddress.requestFocus()
                        binding.etToAddress.setSelection(item.length)
                    }

                    override fun onItemLongClick(
                        view: View, viewHolder: RecyclerView.ViewHolder, position: Int
                    ): Boolean {
                        return false
                    }

                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun initListener() {
        binding.ivChainTitle.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            RemarksTipsDialogView(this, false)
        }

        binding.ivContact.setOnClickListener {
            coin?.let {
                ARouter.getInstance().build(RouterPath.WALLET_CONTACTS)
                    .withSerializable(RouterPath.PARAM_COIN, coin)
                    .navigation()
            }

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
            toAddress = binding.etToAddress.text.toString()
            val money = binding.etMoney.text.toString()
            if (!checkAddressAndMoney(toAddress, money)) {
                return@setOnClickListener
            }
            if (!checkFee(money)) {
                return@setOnClickListener
            }

            if (RegularUtils.isAddress(toAddress)) {
                showPwdDialog()
            } else {
                loading.show()
                lifecycleScope.launch(Dispatchers.IO) {
                    val newDNS = getDNS(toAddress)
                    if (newDNS.isEmpty()) {
                        toast(getString(R.string.home_receipt_address_is_illegal))
                        return@launch
                    }
                    val dnsRep = walletRepository.getDNSResolve(1, newDNS, 0)
                    withContext(Dispatchers.Main) {
                        if (dnsRep.isSucceed()) {
                            dnsRep.data()?.let { list ->
                                if (list.isNotEmpty()) {
                                    showDnsList(list)
                                    loading.dismiss()
                                    toAddress = list[0]
                                    if (RegularUtils.isAddress(toAddress)) {
                                        showPwdDialog()
                                    } else {
                                        ToastUtils.show(
                                            this@OutActivity,
                                            getString(R.string.home_receipt_address_is_illegal)
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
            }


        }

    }

    private fun checkFee(money: String): Boolean {
        coin?.let {
            val inputMoney = money.toDouble()
            val dBalance = it.balance.toDouble()

            if (it.chain == it.name) {
                if (inputMoney + fee > dBalance) {
                    toast(getString(R.string.home_balance_insufficient))
                    return false
                }
            } else {
                if (inputMoney > dBalance) {
                    toast(getString(R.string.home_balance_insufficient))
                    return false
                } else if (fee > chainBalance) {
                    toast(getString(R.string.fee_not_enough))
                    return false
                }
            }
        }

        return true
    }

    private var addressId = 0
    private fun showPwdDialog() {
        if (!AddressCheckUtils.check(coin?.chain, toAddress)) {
            ToastUtils.show(this, getString(R.string.home_receipt_address_is_illegal))
            return
        }
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
            if (password.isEmpty()) {
                toast(getString(R.string.my_wallet_password_tips))
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    coin?.let {
                        withContext(Dispatchers.Main) {
                            loading.show()
                        }
                        val check = GoWallet.checkPasswd(password, it.getpWallet().password)
                        if (!check) {
                            withContext(Dispatchers.Main) {
                                toast(getString(R.string.pwd_fail_str))
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

                        when (it.getpWallet().type) {
                            PWallet.TYPE_NOMAL -> {
                                val bPassword = GoWallet.encPasswd(password)!!
                                val mnem: String = GoWallet.decMenm(bPassword, it.getpWallet().mnem)
                                configNomalWallet(it, mnem)

                                //如果需要代扣
                                if (coinToken.proxy) {
                                    toPara(it, money)
                                } else {
                                    handleTransactions(toAddress, money)
                                }
                            }

                            PWallet.TYPE_PRI_KEY -> {
                                configPrikeyWallet(it)
                                privkey = it.getPrivkey(password)
                                //如果需要代扣
                                if (coinToken.proxy) {
                                    toPara(it, money)
                                } else {
                                    handleTransactions(toAddress, money)
                                }
                            }

                            PWallet.TYPE_RECOVER -> {
                                configPrikeyWallet(it)
                                privkey = it.getPrivkey(password)
                                //找回钱包发送交易
                                doRecover(toAddress, money)
                            }
                        }


                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun toPara(it: Coin, money: String) {
        val gsendTx = GsendTx().apply {
            feepriv = if (it.platform == IPConfig.YBF_CHAIN) YBF_BTY_PR else BTY_PR
            to = toAddress
            tokenSymbol = it.name
            execer = coinToken.exer
            amount = money.toDouble()
            txpriv = privkey
            //消耗的BTY
            fee = BTY_FEE
            //扣的手续费接收地址
            tokenFeeAddr = YBF_FEE_ADDR
            //扣多少手续费
            tokenFee = if (it.platform == IPConfig.YBF_CHAIN) YBF_TOKEN_FEE else TOKEN_FEE
            if (it.treaty == "1") {
                coinsForFee = false
                tokenFeeSymbol = oldName
            } else if (it.treaty == "2") {
                coinsForFee = true
            }
            //feeAddressID是收比特元的手续费地址格式，txAddressID是当前用户地址格式
            feeAddressID = if (it.address.startsWith("0x")) 2 else 0
            txAddressID = if (it.address.startsWith("0x")) 2 else 0
        }
        val gsendTxResp = Walletapi.coinsTxGroup(gsendTx)
        GoWallet.sendTran(it.chain, gsendTxResp.signedTx, it.name)
        val sendTx = gsendTxResp.txId
        runOnUiThread {
            loading.dismiss()
            ToastUtils.show(
                this@OutActivity,
                R.string.home_transfer_currency_success
            )
            finish()
        }
    }

    private fun configNomalWallet(coin: Coin, mnem: String) {
        if ("YCC" == coin.chain || "BTY" == coin.chain) {
            if ("ethereum" == coin.platform || "yhchain" == coin.platform) {
                addressId = 2
                privkey = coin.getPrivkey("ETH", mnem)
            } else if ("btc" == coin.platform) {
                privkey = coin.getPrivkey("BTC", mnem)
                addressId = 0
            } else if ("bty" == coin.platform) {
                privkey = coin.getPrivkey("BTY", mnem)
                addressId = 0
            } else if ("btymain" == coin.platform) {
                privkey = coin.getPrivkey("BNB", mnem)
                addressId = 2
            } else {
                privkey = coin.getPrivkey(coin.chain, mnem)
            }
        } else {
            privkey = coin.getPrivkey(coin.chain, mnem)
        }

    }

    private fun configPrikeyWallet(coin: Coin) {
        if ("YCC" == coin.chain || "BTY" == coin.chain) {
            if ("ethereum" == coin.platform || "yhchain" == coin.platform) {
                addressId = 2
            } else if ("btc" == coin.platform) {
                addressId = 0
            } else if ("bty" == coin.platform) {
                addressId = 0
            } else if ("btymain" == coin.platform) {
                privkey = coin.getPrivkey("BNB")
                addressId = 2
            }
        }
    }

    private suspend fun doRecover(toAddress: String, money: String) {
        coin?.let {
            val tokenSymbol = coinToken.tokenSymbol
            val walletRecoverParam = GoWallet.queryRecover(it.address, it.chain)
            val walletRecover = WalletRecover()
            walletRecover.param = walletRecoverParam
            val createRaw = GoWallet.createTran(
                it.chain, it.address, toAddress, money.toDouble(), fee, "", tokenSymbol
            )
            val strResult = JSON.parseObject(createRaw, StringResult::class.java)
            val createRawResult: String? = strResult.result
            if (!createRawResult.isNullOrEmpty()) {
                val signtx = walletRecover.signRecoverTxWithCtrKey(
                    Walletapi.stringTobyte(createRawResult), privkey
                )
                val sendRawTransaction = GoWallet.sendTran(it.chain, signtx, tokenSymbol)
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                    if (sendRawTransaction.isNullOrEmpty()) {
                        toast(getString(R.string.home_transfer_currency_fails))
                        finish()
                        return@withContext
                    }
                    val result: StringResult? = parseResult(sendRawTransaction)
                    if (result == null) {
                        toast(getString(R.string.out_result_fails))
                        finish()
                        return@withContext
                    }
                    if (!TextUtils.isEmpty(result.error)) {
                        toast(result.error!!)
                        finish()
                        return@withContext
                    }
                    toast(getString(R.string.home_transfer_currency_success))
                    finish()
                }
            }

        }


    }


    private fun handleTransactions(toAddress: String, money: String) {
        coin?.let {
            try {
                val tokensymbol = coinToken.tokenSymbol
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
                if (createRawResult.isNullOrEmpty()) {
                    return
                }
                //签名交易
                val signtx = GoWallet.signTran(
                    it.chain, Walletapi.stringTobyte(createRawResult), privkey, addressId
                )
                if (signtx.isNullOrEmpty()) {
                    return
                }
                //发送交易
                val sendRawTransaction = GoWallet.sendTran(it.chain, signtx, tokensymbol)
                runOnUiThread {
                    loading.dismiss()
                    if (sendRawTransaction.isNullOrEmpty()) {
                        ToastUtils.show(this, getString(R.string.home_transfer_currency_fails))
                        finish()
                        return@runOnUiThread
                    }
                    val result: StringResult? = parseResult(sendRawTransaction)
                    if (result == null) {
                        ToastUtils.show(this, getString(R.string.out_result_fails))
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

            } catch (e: Exception) {
                e.printStackTrace()
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
        }/* else if (!RegularUtils.isEnglish(toAddress)) {
            ToastUtils.show(this, R.string.home_receipt_address_is_illegal)
            return false
        } else if (!AddressCheckUtils.check(coin?.chain, toAddress)) {
            ToastUtils.show(this, getString(R.string.home_receipt_address_is_illegal))
            return false
        }*/
        return true
    }


}