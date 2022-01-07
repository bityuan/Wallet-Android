package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import com.fzm.walletmodule.R

import com.fzm.wallet.sdk.bean.Transactions
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.*
import kotlinx.android.synthetic.main.activity_transaction_details.*


class TransactionDetailsActivity : BaseActivity() {
    private lateinit var transaction: Transactions
    private lateinit var coin: Coin


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_details)
        showInLoading()
        initIntent()
        initListener()
        initData()
        configWallets()
    }

    override fun initListener() {
        super.initListener()
        tv_out_address.setOnClickListener {
            ClipboardUtils.clip(this, tv_out_address.text.toString())
        }
        tv_in_address.setOnClickListener {
            ClipboardUtils.clip(this, tv_in_address.text.toString())

        }
        iv_hx_copy.setOnClickListener {
            ClipboardUtils.clip(this, tv_hash.text.toString())

        }
    }

    override fun initIntent() {
        super.initIntent()
        transaction = intent.getSerializableExtra(Transactions::class.java.simpleName) as Transactions
        coin = intent.getSerializableExtra(Coin::class.java.simpleName) as Coin
    }

    override fun initData() {
        super.initData()
        tv_out_address.text = transaction.from
        tv_in_address.text = transaction.to
        tv_miner.text = "${transaction.fee} ${coin.chain}"
        tv_block.text = transaction.height.toString()

        tv_hash.text = transaction.txid
        if (!TextUtils.isEmpty(transaction.nickName)) {
            tv_nick_name.text = "${transaction.nickName}"
        }
        tv_inout.text = if (transaction.type == Transactions.TYPE_SEND) Transactions.OUT_STR else Transactions.IN_STR
        tv_number.text = transaction.value
        tv_coin.text = coin.uiName
        tv_note.text = if (TextUtils.isEmpty(transaction.note)) getString(R.string.home_no) else transaction.note
        when (transaction.status) {
            -1 -> {
                handleStatus(getString(R.string.home_transaction_fails), R.mipmap.icon_fail)
                tv_time.text = TimeUtils.getTime(transaction.blocktime * 1000L)
            }
            0 -> handleStatus(getString(R.string.home_confirming), R.mipmap.icon_waitting)
            1 -> {
                handleStatus(getString(R.string.home_transaction_success), R.mipmap.icon_success)
                tv_time.text = TimeUtils.getTime(transaction.blocktime * 1000L)
            }
        }
    }

    private fun handleStatus(text: String, imgId: Int) {
        tv_status.text = text
        iv_status.setImageResource(imgId)
    }

}