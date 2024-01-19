package com.fzm.walletmodule.ui.activity

import android.graphics.Paint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.IPConfig.Companion.YBF_TOKEN_FEE
import com.fzm.wallet.sdk.IPConfig.Companion.getBrowserUrl
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.IAppTypeProvider
import com.fzm.wallet.sdk.base.ROUTE_APP_TYPE
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.walletmodule.R

import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Address
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.databinding.ActivityTransactionDetailsBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.configWindow
import com.fzm.walletmodule.utils.*
import com.google.gson.Gson
import com.kongzue.dialogx.dialogs.MessageDialog
import kotlinx.android.synthetic.main.dialog_tran_details.view.btn_ok
import kotlinx.android.synthetic.main.dialog_tran_details.view.et_fee
import kotlinx.android.synthetic.main.dialog_tran_details.view.et_password
import kotlinx.android.synthetic.main.dialog_tran_details.view.tv_cancel
import kotlinx.android.synthetic.main.dialog_tran_details.view.tv_fee_tip
import kotlinx.android.synthetic.main.dialog_tran_details.view.tv_title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.TxDetail
import walletapi.WalletTxPending
import walletapi.Walletapi

@Route(path = RouterPath.WALLET_TRANSACTION_DETAILS)
class TransactionDetailsActivity : BaseActivity() {
    private val binding by lazy { ActivityTransactionDetailsBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired(name = RouterPath.PARAM_COIN)
    var coin: Coin? = null

    @JvmField
    @Autowired(name = RouterPath.PARAM_TRANSACTIONS)
    var transactions: Transactions? = null

    private val tranDialog: AlertDialog by lazy { AlertDialog.Builder(this).create() }
    private val tranView: View by lazy {
        LayoutInflater.from(this).inflate(R.layout.dialog_tran_details, null)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        showInLoading()
        initView()
        initListener()
        initData()
        configWallets()
    }

    override fun configWallets() {
        super.configWallets()
        val navigation =
            ARouter.getInstance().build(ROUTE_APP_TYPE).navigation() as IAppTypeProvider
        if (navigation.getAppType() == IPConfig.APP_MY_DAO) {
            binding.btnAddContacts.visibility = View.VISIBLE
        }
    }

    override fun initView() {
        super.initView()
        binding.tvHash.paint.flags = Paint.UNDERLINE_TEXT_FLAG //下划线
        binding.tvHash.paint.isAntiAlias = true//抗锯齿

        transactions?.let { transaction ->
            if (transaction.status == 0 && transaction.type == Transactions.TYPE_SEND && coin!!.chain ==
                Walletapi.TypeETHString
            ) {
                binding.tvTranToto.visibility = View.VISIBLE
                binding.tvTranCancel.visibility = View.VISIBLE
            }
        }

    }

    override fun initListener() {
        super.initListener()
        binding.tvOutAddress.setOnClickListener {
            ClipboardUtils.clip(this, binding.tvOutAddress.text.toString())
        }
        binding.tvInAddress.setOnClickListener {
            ClipboardUtils.clip(this, binding.tvInAddress.text.toString())

        }
        binding.ivHxCopy.setOnClickListener {
            ClipboardUtils.clip(this, binding.tvHash.text.toString())

        }
        binding.tvHash.setOnClickListener {
            coin?.let {
                val coinToken = it.newChain
                val url = getBrowserUrl(coinToken.cointype)
                if (url.isEmpty()) {
                    toast(getString(R.string.no_sup))
                } else {
                    ARouter.getInstance().build(RouterPath.APP_DAPP)
                        .withString(RouterPath.PARAM_URL, "$url${transactions?.txid}").navigation()
                }

            }

        }

        binding.btnAddContacts.setOnClickListener {
            transactions?.let { transaction ->
                val otherAddress =
                    if (transaction.type == Transactions.TYPE_SEND) transaction.to else transaction.from
                ARouter.getInstance().build(RouterPath.WALLET_UPDATE_CONTACTS)
                    .withInt(RouterPath.PARAM_FROM, 2).withSerializable(RouterPath.PARAM_COIN, coin)
                    .withString(RouterPath.PARAM_ADDRESS, otherAddress).navigation()
            }

        }

        binding.tvTranToto.setOnClickListener {
            showTranDialog(1)
        }
        binding.tvTranCancel.setOnClickListener {
            showTranDialog(2)
        }
    }


    override fun initData() {
        super.initData()
        coin?.let { co ->
            transactions?.let { transaction ->
                binding.tvOutAddress.text = transaction.from
                binding.tvInAddress.text = transaction.to
                if (GoWallet.isPara(co)) {
                    if (transaction.note!!.contains("para")) {
                        binding.tvMiner.text = "0 ${co.uiName}"
                    } else {
                        binding.tvMiner.text = "$YBF_TOKEN_FEE ${co.uiName}"
                    }
                } else {
                    binding.tvMiner.text = "${transaction.fee} ${co.newChain.cointype}"
                }
                binding.tvBlock.text = transaction.height.toString()

                binding.tvHash.text = transaction.txid
                if (!TextUtils.isEmpty(transaction.nickName)) {
                    binding.tvNickName.text = "${transaction.nickName}"
                }
                binding.tvInout.text =
                    if (transaction.type == Transactions.TYPE_SEND) Transactions.OUT_STR else Transactions.IN_STR
                binding.tvNumber.text = transaction.value
                binding.tvCoin.text = co.uiName
                binding.tvNote.text =
                    if (TextUtils.isEmpty(transaction.note)) getString(R.string.home_no) else transaction.note
                when (transaction.status) {
                    -1 -> {
                        handleStatus(getString(R.string.home_transaction_fails), R.mipmap.icon_fail)
                        binding.tvTime.text = TimeUtils.getTime(transaction.blocktime * 1000L)
                    }

                    0 -> handleStatus(getString(R.string.home_confirming), R.mipmap.icon_waitting)
                    1 -> {
                        handleStatus(
                            getString(R.string.home_transaction_success), R.mipmap.icon_success
                        )
                        binding.tvTime.text = TimeUtils.getTime(transaction.blocktime * 1000L)
                    }
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    var names = ""
                    try {
                        val otherAddress =
                            if (transaction.type == Transactions.TYPE_SEND) transaction.to else transaction.from
                        val addressList =
                            LitePal.where("address = ?", otherAddress).find<Address>(true)
                        val list = mutableListOf<String?>()
                        for (addr in addressList) {
                            list.add(addr.contacts?.nickName)
                        }
                        names = list.joinToString(separator = " ")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    withContext(Dispatchers.Main) {
                        binding.tvContacts.text = names
                    }
                }


            }

        }

    }

    private fun handleStatus(text: String, imgId: Int) {
        binding.tvStatus.text = text
        binding.ivStatus.setImageResource(imgId)
    }


    private fun showTranDialog(type: Int) {
        tranView.et_fee.text.clear()
        tranView.et_password.text.clear()
        tranView.tv_title.text = when (type) {
            1 -> getString(R.string.speed_up_str)
            2 -> getString(R.string.cancel_tran_str)
            else -> getString(R.string.speed_up_str)
        }
        tranView.tv_fee_tip.setOnClickListener {
            MaterialDialog.Builder(this).title(getString(R.string.tip_2))
                .content("${getString(R.string.eth_tip_speed)}\n\n${getString(R.string.eth_tip_cancle)}")
                .show()
        }
        tranDialog.setCancelable(false)
        tranDialog.setView(tranView)
        configWindow(tranDialog)
        tranView.tv_cancel.setOnClickListener {
            tranDialog.dismiss()
        }
        tranView.btn_ok.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            val fee = tranView.et_fee.text.toString()
            val password = tranView.et_password.text.toString()
            if (!check(fee, password)) {
                return@setOnClickListener
            }
            if (type == 1) {
                val pendingTx =
                    Walletapi.speedupPendingTx(walletTxPending(fee.toDouble()), GoWallet.getUtil())
                sendTran(pendingTx, password)
            } else if (type == 2) {
                val pendingTx =
                    Walletapi.cancelPendingTx(walletTxPending(fee.toDouble()), GoWallet.getUtil())
                sendTran(pendingTx, password)
            }
        }

        tranDialog.show()

    }

    private fun walletTxPending(fee: Double): WalletTxPending {
        val pend = WalletTxPending()
        val txinfo = TxDetail()
        txinfo.from = transactions?.from
        txinfo.to = transactions?.to
        txinfo.fee = fee
        txinfo.txid = transactions?.txid
        pend.cointype = Walletapi.TypeETHString
        val tokensymbol = if (coin?.name == coin?.chain) "" else coin?.name
        pend.tokenSymbol = tokensymbol
        pend.txinfo = txinfo
        pend.txinfo
        return pend
    }

    private fun sendTran(pendingTx: ByteArray, password: String) {
        coin?.let { co ->
            val createtx = Walletapi.byteTostring(pendingTx)
            val result = parseCreateResult(createtx)
            val bResult = Walletapi.stringTobyte(result)
            var sendtx = ""

            val localPassword = coin!!.getpWallet().password
            showLoading()
            doAsync {
                val checked = GoWallet.checkPasswd(password, localPassword)
                var checkPwd = false
                if (checked) {
                    val mnem =
                        GoWallet.decMenm(GoWallet.encPasswd(password)!!, co.getpWallet().mnem)
                    val priv = co.getPrivkey(co.chain, mnem)
                    val signRawTransaction = GoWallet.signTran(co.chain, bResult, priv)
                    sendtx = GoWallet.sendTran(co.chain, signRawTransaction!!, "") ?: ""
                    checkPwd = true
                } else {
                    checkPwd = false
                }
                uiThread {
                    if (checkPwd) {
                        val result = parseResult(sendtx)
                        if (result == null) {
                            toast(getString(R.string.basic_error_send))
                        } else if (!TextUtils.isEmpty(result.error)) {
                            toast(getString(R.string.basic_error_send) + result.error)
                        } else {
                            toast(getString(R.string.send_suc_str) + result.result)
                        }


                    } else {
                        toast(getString(R.string.home_pwd_input_error))

                    }
                    dismiss()
                    tranDialog.dismiss()
                }
            }
        }

    }

    private fun parseCreateResult(json: String): String? {
        if (TextUtils.isEmpty(json)) {
            return json
        }
        val stringResult = JsonUtils.toObject(json, StringResult::class.java)
        return stringResult!!.result
    }

    private fun parseResult(json: String): StringResult? {
        return if (TextUtils.isEmpty(json)) {
            null
        } else Gson().fromJson(json, StringResult::class.java)
    }

    private fun check(fee: String, password: String): Boolean {
        var checked = true
        if (fee.isEmpty()) {
            toast(getString(R.string.p_input_fee_str))
            checked = false
        } else if (password.isEmpty()) {
            toast(getString(R.string.my_wallet_set_password))
            checked = false
        } else if (!(password.length >= 8 && password.length <= 16)) {
            toast(getString(R.string.my_create_letter))
            checked = false
        } else if (!AppUtils.ispassWord(password)) {
            toast(getString(R.string.my_set_password_number_letter))
            checked = false
        } else if (fee.toDouble() > coin!!.balance.toDouble()) {
            toast(getString(R.string.home_balance_insufficient))
            checked = false
        } else if (fee.toDouble() < transactions!!.fee.toDouble() * 1.2) {
            toast(getString(R.string.tip_fee_low))
            checked = false
        } else if (fee.toDouble() > transactions!!.fee.toDouble() * 10) {
            toast(getString(R.string.tip_fee_high))
            checked = false
        }
        return checked
    }
}