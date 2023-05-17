package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import com.fzm.wallet.sdk.IPConfig.Companion.YBF_TOKEN_FEE
import com.fzm.walletmodule.R

import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.databinding.ActivityTransactionDetailsBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.*


class TransactionDetailsActivity : BaseActivity() {
    private lateinit var transaction: Transactions
    private lateinit var coin: Coin
    private val binding by lazy { ActivityTransactionDetailsBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        showInLoading()
        initIntent()
        initListener()
        initData()
        configWallets()
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
    }

    override fun initIntent() {
        super.initIntent()
        transaction =
            intent.getSerializableExtra(Transactions::class.java.simpleName) as Transactions
        coin = intent.getSerializableExtra(Coin::class.java.simpleName) as Coin
    }

    override fun initData() {
        super.initData()
        binding.tvOutAddress.text = transaction.from
        binding.tvInAddress.text = transaction.to
        if(GoWallet.isPara(coin)) {
            if(transaction.note!!.contains("para")) {
                binding.tvMiner.text = "0 ${coin.uiName}"
            }else {
                binding.tvMiner.text = "$YBF_TOKEN_FEE ${coin.uiName}"
            }
        }else {
            binding.tvMiner.text = "${transaction.fee} ${coin.newChain.cointype}"
        }
        binding.tvBlock.text = transaction.height.toString()

        binding.tvHash.text = transaction.txid
        if (!TextUtils.isEmpty(transaction.nickName)) {
            binding.tvNickName.text = "${transaction.nickName}"
        }
        binding.tvInout.text =
            if (transaction.type == Transactions.TYPE_SEND) Transactions.OUT_STR else Transactions.IN_STR
        binding.tvNumber.text = transaction.value
        binding.tvCoin.text = coin.uiName
        binding.tvNote.text =
            if (TextUtils.isEmpty(transaction.note)) getString(R.string.home_no) else transaction.note
        when (transaction.status) {
            -1 -> {
                handleStatus(getString(R.string.home_transaction_fails), R.mipmap.icon_fail)
                binding.tvTime.text = TimeUtils.getTime(transaction.blocktime * 1000L)
            }
            0 -> handleStatus(getString(R.string.home_confirming), R.mipmap.icon_waitting)
            1 -> {
                handleStatus(getString(R.string.home_transaction_success), R.mipmap.icon_success)
                binding.tvTime.text = TimeUtils.getTime(transaction.blocktime * 1000L)
            }
        }
    }

    private fun handleStatus(text: String, imgId: Int) {
        binding.tvStatus.text = text
        binding.ivStatus.setImageResource(imgId)
    }

}