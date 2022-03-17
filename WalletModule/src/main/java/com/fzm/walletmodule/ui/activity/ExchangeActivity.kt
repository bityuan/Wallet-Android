package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.activity.TransactionsActivity.Companion.USDT
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.vm.ExchangeViewModel
import kotlinx.android.synthetic.main.activity_exchange.*
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import walletapi.Walletapi
import java.math.BigDecimal

class ExchangeActivity : BaseActivity() {

    private var mEditDialogFragment: EditDialogFragment? = null
    private lateinit var mCoin: Coin
    private lateinit var mExchange: Exchange
    private val exchangeViewModel: ExchangeViewModel by inject(walletQualifier)
    private var checked = true

    private var exFee = 0.0

    private var gasFeeUsdt = 0.0

    private var countFee = 0.0

    private var limit = 0.0

    private var gasChain = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange)
        initIntent()
        initObserver()
        initListener()
        initData()
    }

    override fun initIntent() {
        mCoin = intent.getSerializableExtra(Coin::class.java.simpleName) as Coin
        if (mCoin.chain == Walletapi.TypeTrxString && mCoin.name == USDT) {
            ll_ex_bnb.visibility = View.VISIBLE
            tv_re_chain.visibility = View.VISIBLE
            checked = true
        } else {
            ll_ex_bnb.visibility = View.GONE
            tv_re_chain.visibility = View.GONE
            checked = false
        }
    }

    override fun initObserver() {
        super.initObserver()
        exchangeViewModel.flashExchange.observe(this, Observer {
            if (it.isSucceed()) {
                val result = it.data()
                Log.v("zx", "res = " + result)
                dismiss()
                toast("操作成功")
                getBalance()
            } else {
                dismiss()
                toast(it.error())
            }
        })

        exchangeViewModel.getExLimit.observe(this, Observer {
            if (it.isSucceed()) {
                limit = it.data()!!
                tv_limit.text = "$limit ${mCoin.name}"
            } else {
                toast(it.error())
            }
        })
        exchangeViewModel.getExFee.observe(this, Observer {
            if (it.isSucceed()) {
                disInLoading()
                it.data().let {
                    exFee = it?.fee!!
                    gasFeeUsdt = it.gasFeeUsdt
                    gasChain = it.gasFeeAmount
                    countFee = exFee + gasFeeUsdt

                    val bigDecimal = BigDecimal(gasChain).setScale(4, BigDecimal.ROUND_DOWN);
                    val gasChain = bigDecimal.toString()
                    tv_ex_fee.text = "$exFee ${mCoin.name}"
                    tv_ex_chain.text = "是否使用$gasFeeUsdt USDT兑换BNB ≈$gasChain BNB"
                    tv_re_chain.text = "$gasChain BNB"
                }
            }
        })
    }

    override fun initListener() {
        super.initListener()
        iv_check.setOnClickListener {
            if (checked) {
                iv_check.setImageResource(R.mipmap.ic_ex_nomal)
                checked = false
            } else {
                iv_check.setImageResource(R.mipmap.ic_ex_sel)
                checked = true
            }
            handleCheck(et_value.text.toString(), checked)
        }
        btn_exchange.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val value = et_value.text.toString()
                if (TextUtils.isEmpty(value)) {
                    toast("请输入兑换数量")
                    return@launch
                } else if (value.toDouble() > balance.toDouble()) {
                    toast("余额不足")
                    return@launch
                } else if (value.toDouble() > limit) {
                    toast("今日可兑额度不足")
                    return@launch
                }

                if (mCoin.name == Walletapi.TypeBtyString && mCoin.chain == Walletapi.TypeBtyString) {
                    if (value.toDouble() + 0.05 > balance.toDouble()) {
                        toast("BTY不能全额兑换，需预留约0.05个BTY作为手续费")
                        return@launch
                    }
                }

                if (checked) {
                    if (value.toDouble() < (countFee + 1)) {
                        toast("请输入足够的兑换数量")
                        return@launch
                    }
                } else {
                    if (value.toDouble() < (exFee + 1)) {
                        toast("请输入足够的兑换数量")
                        return@launch
                    }
                }


                var chainBalance = 0.0
                withContext(Dispatchers.IO) {
                    val chain = BWallet.get().getMainCoin(mCoin.chain)
                    chainBalance = chain?.balance?.toDouble() ?: 0.0
                }
                val minFee: Double = when (mCoin.chain) {
                    Walletapi.TypeTrxString -> 10.0
                    Walletapi.TypeBtyString -> 0.01
                    Walletapi.TypeBnbString -> 0.001
                    else -> 0.01
                }
                if (chainBalance < minFee) {
                    toast("最低矿工费为$minFee ${mCoin.chain}")
                } else {
                    showPasswordDialog()
                }
            }
        }

        tv_max.setOnClickListener {
            if (!TextUtils.isEmpty(balance)) {
                if (mCoin.name == Walletapi.TypeBtyString && mCoin.chain == Walletapi.TypeBtyString) {
                    val b = balance.toDouble() - 0.01
                    et_value.setText(b.toString())
                } else {
                    et_value.setText(balance)
                }

            }
        }


        et_value.addTextChangedListener {
            try {

                //如果第一个数字为0，第二个不为点，就不允许输入
                if (it.toString().startsWith("0") && it.toString().trim().length > 1) {
                    if (!it.toString().substring(1, 2).equals(".")) {
                        et_value.setText(it?.subSequence(0, 1));
                        et_value.setSelection(1);
                        return@addTextChangedListener;
                    }
                }
                //如果第一为点，直接显示0.
                if (it.toString().startsWith(".")) {
                    et_value.setText("0.");
                    et_value.setSelection(2);
                    return@addTextChangedListener;
                }
                //限制输入小数位数(2位)
                if (it.toString().contains(".")) {
                    if (it?.length!! - 1 - it.toString().indexOf(".") > 2) {
                        val s = it.toString().subSequence(0, it.toString().indexOf(".") + 2 + 1);
                        et_value.setText(s);
                        et_value.setSelection(s.length);
                    }

                }
                handleCheck(it.toString(), checked)

            } catch (e: Exception) {
                e.printStackTrace()
            }


        }
    }


    private fun handleCheck(inputStr: String, checked: Boolean) {
        if (!TextUtils.isEmpty(inputStr)) {
            val input = inputStr.toDouble()
            if (checked) {
                if (input >= countFee) {
                    val inputb = BigDecimal(inputStr).setScale(2, BigDecimal.ROUND_DOWN)
                    val countFeeStr = BigDecimal(countFee).setScale(2, BigDecimal.ROUND_DOWN)
                    val value = inputb.subtract(countFeeStr)
                    tv_re_value.text = "${value} USDT"
                    tv_re_chain.text = "$gasChain BNB"
                } else {
                    resetExValue()
                }
            } else {
                if (input >= exFee) {
                    val inputb = BigDecimal(inputStr).setScale(2, BigDecimal.ROUND_DOWN)
                    val exFeeStr = BigDecimal(exFee).setScale(2, BigDecimal.ROUND_DOWN)
                    val value = inputb.subtract(exFeeStr)
                    tv_re_value.text = "${value} ${mCoin.name}"
                    tv_re_chain.text = "0 BNB"
                } else {
                    resetExValue()
                }
            }
        } else {
            resetExValue()
        }

    }

    private fun resetExValue() {
        tv_re_value.text = "0 ${mCoin.name}"
        tv_re_chain.text = "0 BNB"
    }

    override fun initData() {
        super.initData()
        tv_balance.text = "余额 ${mCoin.balance} ${mCoin.name}(${mCoin.nickname})"
        balance = mCoin.balance

        exchangeTips.visibility = if(mCoin.name == "USDT" && mCoin.chain == "TRX") View.VISIBLE else View.GONE
        getExchange(mCoin)
    }


    private fun showPasswordDialog() {
        if (mEditDialogFragment == null) {
            mEditDialogFragment = EditDialogFragment()

            mEditDialogFragment!!.setType(1)
                .setRightButtonStr(getString(R.string.home_confirm))
                .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View?) {}
                    override fun onRightButtonClick(v: View?) {
                        val etPassword = mEditDialogFragment?.etInput
                        val password = etPassword?.text.toString()
                        if (TextUtils.isEmpty(password)) {
                            toast("请输入账户密码")
                            return
                        }
                        val localPassword = mCoin.getpWallet().password
                        payCheck(password, localPassword)
                    }
                })
        }
        mEditDialogFragment?.showDialog("tag", supportFragmentManager)
    }

    private fun payCheck(password: String, localPassword: String) {
        showLoading()
        doAsync {
            val result = GoWallet.checkPasswd(password, localPassword)
            if (result) {
                configTransaction(password)
            } else {
                runOnUiThread {
                    ToastUtils.show(this@ExchangeActivity, R.string.home_pwd_input_error)
                    dismiss()
                }
            }
        }
    }

    private fun configTransaction(password: String) {
        val mnem = GoWallet.decMenm(GoWallet.encPasswd(password)!!, mCoin.getpWallet().mnem)
        val priv = mCoin.getPrivkey(mCoin.chain, mnem)
        handleTransactions(priv)

    }

    private fun handleTransactions(priv: String) {
        val amount = et_value.text.toString().toDouble()
        val tokensymbol = if (mCoin.name == mCoin.chain) "" else mCoin.name
        //构造交易
        val createRaw = GoWallet.createTran(
            mCoin.chain,
            mCoin.address,
            mExchange.toAddress,
            amount,
            0.001,
            "exchange",
            tokensymbol
        )
        val stringResult = JSON.parseObject(createRaw, StringResult::class.java)
        val createRawResult: String? = stringResult.result
        if (TextUtils.isEmpty(createRawResult)) {
            return
        }
        //签名交易
        val signtx = GoWallet.signTran(mCoin.chain, createRawResult!!, priv)
        if (TextUtils.isEmpty(signtx)) {
            return
        }
        exchangeViewModel.flashExchange(
            mCoin.chain,
            if (mCoin.chain == mCoin.name) "" else mCoin.name,
            mExchange.receiveAddress,
            signtx!!,
            amount,
            mExchange.toAddress,
            checked
        )

    }


    private fun getExchange(coin: Coin) {

        lifecycleScope.launch(Dispatchers.Main) {
            showInLoading()
            val job1 = lifecycleScope.async(Dispatchers.Main) {
                val exchange = Exchange()
                if (Walletapi.TypeTrxString == coin.chain && TransactionsActivity.USDT == coin.name) {
                    exchange.toAddress = "TLeG94FNqAg7fs9C2ytcBk1eWcn3vaK9hb"
                    exchange.receiveAddressTitle = "接收地址(BEP20)"
                    withContext(Dispatchers.IO) {
                        exchange.receiveAddress = BWallet.get().getAddress(Walletapi.TypeBnbString)
                    }

                } else if (Walletapi.TypeBnbString == coin.chain) {
                    when (coin.name) {
                        TransactionsActivity.USDT -> {
                            exchange.toAddress = "0xA5d8f37CA965b01E5E54390449E50A4241d7AE55"
                            exchange.receiveAddressTitle = "接收地址(TRC20)"
                            withContext(Dispatchers.IO) {
                                exchange.receiveAddress =
                                    BWallet.get().getAddress(Walletapi.TypeTrxString)
                            }
                        }
                        TransactionsActivity.YCC -> {
                            exchange.toAddress = "0xA5d8f37CA965b01E5E54390449E50A4241d7AE55"
                            exchange.receiveAddressTitle = "接收地址(ERC20)"
                            withContext(Dispatchers.IO) {
                                exchange.receiveAddress =
                                    BWallet.get().getAddress(Walletapi.TypeETHString)
                            }
                        }
                        TransactionsActivity.BTY -> {
                            exchange.toAddress = "0xA5d8f37CA965b01E5E54390449E50A4241d7AE55"
                            exchange.receiveAddressTitle = "接收地址(BTY)"
                            withContext(Dispatchers.IO) {
                                exchange.receiveAddress =
                                    BWallet.get().getAddress(Walletapi.TypeBtyString)
                            }
                        }
                    }

                } else if (Walletapi.TypeBtyString == coin.chain && Walletapi.TypeBtyString == coin.name) {
                    exchange.toAddress = "156SZUbSkKGkzJ5Mypt2u467JBZ8QkzDg1"
                    exchange.receiveAddressTitle = "接收地址(BEP20)"
                    withContext(Dispatchers.IO) {
                        exchange.receiveAddress =
                            BWallet.get().getAddress(Walletapi.TypeBnbString)
                    }


                } else if (Walletapi.TypeETHString == coin.chain && Walletapi.TypeYccString == coin.name) {

                }

                exchange
            }

            mExchange = job1.await()
            tv_receive_address_title.text = mExchange.receiveAddressTitle
            tv_receive_address.text = mExchange.receiveAddress
            exchangeViewModel.getExLimit(
                mExchange.receiveAddress,
                mCoin.chain,
                if (mCoin.chain == mCoin.name) "" else mCoin.name
            )
            exchangeViewModel.getExFee(
                mCoin.chain,
                if (mCoin.chain == mCoin.name) "" else mCoin.name
            )
        }
    }

    private var balance: String = "0"
    private fun getBalance() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                balance = GoWallet.handleBalance(mCoin)
                withContext(Dispatchers.Main) {
                    tv_balance.text = "余额 $balance USDT (TRC20)"
                }
                delay(3000)
            }

        }
    }


    companion object {
        class Exchange {
            lateinit var toAddress: String
            lateinit var receiveAddressTitle: String
            lateinit var receiveAddress: String
        }
    }
}