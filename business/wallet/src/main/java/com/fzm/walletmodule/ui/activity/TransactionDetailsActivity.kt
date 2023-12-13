package com.fzm.walletmodule.ui.activity

import android.graphics.Paint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.IPConfig.Companion.YBF_TOKEN_FEE
import com.fzm.wallet.sdk.IPConfig.Companion.getBrowserUrl
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.IAppTypeProvider
import com.fzm.wallet.sdk.base.ROUTE_APP_TYPE
import com.fzm.walletmodule.R

import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Address
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.databinding.ActivityTransactionDetailsBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find

@Route(path = RouterPath.WALLET_TRANSACTION_DETAILS)
class TransactionDetailsActivity : BaseActivity() {
    private val binding by lazy { ActivityTransactionDetailsBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired(name = RouterPath.PARAM_COIN)
    var coin: Coin? = null

    @JvmField
    @Autowired(name = RouterPath.PARAM_TRANSACTIONS)
    var transactions: Transactions? = null


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

}