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
import kotlinx.android.synthetic.main.listitem_choose_chain.*
import kotlinx.android.synthetic.main.view_header_wallet.*
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import walletapi.Walletapi
import java.math.BigDecimal

class ExchangeActivity : BaseActivity() {

    private var mEditDialogFragment: EditDialogFragment? = null
    private lateinit var mCoin: Coin
    private val mainScope = MainScope()
    private lateinit var bnbAddress: String
    private val exchangeViewModel: ExchangeViewModel by inject(walletQualifier)
    private var checked = true

    private var exFee = 0.0

    private var gasFeeUsdt = 0.0

    private var countFee = 0.0

    private var limit = 0.0

    private var gasChain = 0.0

    companion object {
        val TOADDRESS = "TLeG94FNqAg7fs9C2ytcBk1eWcn3vaK9hb"
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
        tv_balance.text = "?????? ${mCoin.balance} USDT (TRC20)"
        balance = mCoin.balance
    }

    override fun initObserver() {
        super.initObserver()
        exchangeViewModel.flashExchange.observe(this, Observer {
            if (it.isSucceed()) {
                val result = it.data()
                Log.v("zx", "res = " + result)
                dismiss()
                toast("????????????")
                getBalance()
            } else {
                dismiss()
                toast(it.error())
            }
        })

        exchangeViewModel.getExLimit.observe(this, Observer {
            if (it.isSucceed()) {
                limit = it.data()!!
                tv_limit.text = "$limit USDT"
            } else {
                limit = 1000.0
                tv_limit.text = "$limit USDT"
                //toast(it.error())
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
                    tv_ex_fee.text = "$exFee USDT"
                    tv_ex_chain.text = "????????????$gasFeeUsdt USDT??????BNB ???$gasChain BNB"
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
                    toast("?????????????????????")
                    return@launch
                } else if (value.toDouble() > balance.toDouble() || value.toDouble() > limit) {
                    toast("????????????")
                    return@launch
                }

                if (checked) {
                    if (value.toDouble() < (countFee + 1)) {
                        toast("??????????????????????????????")
                        return@launch
                    }
                } else {
                    if (value.toDouble() < (exFee+1)) {
                        toast("??????????????????????????????")
                        return@launch
                    }
                }


                var trx: Coin?
                withContext(Dispatchers.IO) {
                    trx = BWallet.get().getChain(Walletapi.TypeTrxString)
                }
                if (trx?.balance?.toDouble()!! < 10) {
                    toast("??????????????????10TRX")
                } else {
                    showPasswordDialog()
                }
            }
        }

        tv_max.setOnClickListener {
            if (!TextUtils.isEmpty(balance)) {
                et_value.setText(balance)
            }

        }


        et_value.addTextChangedListener {
            try {

                //????????????????????????0??????????????????????????????????????????
                if (it.toString().startsWith("0") && it.toString().trim().length > 1) {
                    if (!it.toString().substring(1, 2).equals(".")) {
                        et_value.setText(it?.subSequence(0, 1));
                        et_value.setSelection(1);
                        return@addTextChangedListener;
                    }
                }
                //?????????????????????????????????0.
                if (it.toString().startsWith(".")) {
                    et_value.setText("0.");
                    et_value.setSelection(2);
                    return@addTextChangedListener;
                }
                //????????????????????????(2???)
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
                    tv_re_value.text = "${value} USDT"
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
        tv_re_value.text = "0 USDT"
        tv_re_chain.text = "0 BNB"
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
                        val password = etPassword?.text.toString()
                        if (TextUtils.isEmpty(password)) {
                            toast("?????????????????????")
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
        //????????????
        val createRaw = GoWallet.createTran(
            mCoin.chain,
            mCoin.address,
            TOADDRESS,
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
        //????????????
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
        showInLoading()
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
                    tv_balance.text = "?????? $balance USDT (TRC20)"
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