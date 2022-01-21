package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.bean.StringResult
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.utils.DecimalUtils
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.vm.ExchangeViewModel
import com.fzm.walletmodule.vm.OutViewModel
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_exchange.*
import kotlinx.android.synthetic.main.activity_exchange.tv_balance
import kotlinx.android.synthetic.main.activity_transactions.*
import kotlinx.android.synthetic.main.view_header_wallet.*
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.koin.android.ext.android.inject
import walletapi.Walletapi
import java.math.BigDecimal

class ExchangeActivity : BaseActivity() {

    private var mEditDialogFragment: EditDialogFragment? = null
    private lateinit var mCoin: Coin
    private val mainScope = MainScope()
    private lateinit var bnbAddress: String
    private val exchangeViewModel: ExchangeViewModel by inject(walletQualifier)
    private var checked = true

    //兑换手续费
    private var exFee = 0.0

    //兑换BNB消耗的USDT
    private var gasFeeUsdt = 0.0

    //总扣减手续费
    private var countFee = 0.0

    companion object {
        val TOADDRESS = "TPKLQtd9s7eZJtWPy4H63hCckhbbzmtStn"
    }

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
        tv_balance.text = "余额 ${mCoin.balance} USDT (TRC20)"
        balance = mCoin.balance
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
                tv_limit.text = "${it.data().toString()} USDT"
            } else {
                toast(it.error())
            }
        })
        exchangeViewModel.getExFee.observe(this, Observer {
            if (it.isSucceed()) {
                it.data().let {
                    exFee = it?.fee!!
                    gasFeeUsdt = it.gasFeeUsdt
                    countFee = exFee + gasFeeUsdt

                    val bigDecimal = BigDecimal(it.gasFeeAmount).setScale(4, BigDecimal.ROUND_DOWN);
                    val gasChain = bigDecimal.toString()
                    tv_ex_fee.text = "$exFee USDT"
                    tv_ex_chain.text = "是否兑换 $gasChain BNB"
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
        }
        btn_exchange.setOnClickListener {
            val value = et_value.text.toString()
            if (TextUtils.isEmpty(value)) {
                toast("请输入兑换数量")
                return@setOnClickListener
            }
            if (value.toDouble() <= countFee) {
                toast("请输入足够的兑换数量")
                return@setOnClickListener
            }
            showPasswordDialog()
        }

        tv_max.setOnClickListener {
            if (!TextUtils.isEmpty(balance)) {
                et_value.setText(balance)
            }

        }


        et_value.addTextChangedListener {
            try {
                val str = it.toString()
                if (!TextUtils.isEmpty(str)) {
                    val input = str.toDouble()
                    if (input > countFee) {

                        val inputstr = BigDecimal(input).setScale(2, BigDecimal.ROUND_DOWN)
                        val countFeeStr = BigDecimal(countFee).setScale(2, BigDecimal.ROUND_DOWN)

                        val value = inputstr.subtract(countFeeStr)

                        Log.v("zx","数据 = "+input)
                        Log.v("zx","数据 = "+countFee)
                        Log.v("zx","数据 = "+value)

                        tv_re_value.text = "${value} USDT"
                    }
                } else {
                    tv_re_value.text = "0 USDT"
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }


        }
    }

    override fun initData() {
        super.initData()
        getAddress()
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
                        val password =
                            etPassword?.text.toString().trim { it <= ' ' }
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
            TOADDRESS,
            amount,
            0.001,
            "闪兑测试",
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
            mCoin.chain, mCoin.name, bnbAddress, signtx!!, amount,
            TOADDRESS, checked
        )

    }


    private fun getAddress() {
        mainScope.launch(Dispatchers.IO) {
            bnbAddress = BWallet.get().getAddress(Walletapi.TypeBnbString)
            exchangeViewModel.getExLimit(bnbAddress)
            exchangeViewModel.getExFee()
            withContext(Dispatchers.Main) {
                tv_bsc_address.text = bnbAddress
            }

        }
    }

    private var balance: String = "0"
    private fun getBalance() {
        mainScope.launch(Dispatchers.IO) {
            while (true) {
                balance = GoWallet.handleBalance(mCoin)
                withContext(Dispatchers.Main) {
                    tv_balance.text = "余额 $balance USDT (TRC20)"
                }
                delay(3000)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}