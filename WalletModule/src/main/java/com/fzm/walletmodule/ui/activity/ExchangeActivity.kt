package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
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
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.vm.ExchangeViewModel
import com.fzm.walletmodule.vm.OutViewModel
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_exchange.*
import kotlinx.android.synthetic.main.activity_out.*
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import walletapi.Walletapi

class ExchangeActivity : BaseActivity() {

    private var mEditDialogFragment: EditDialogFragment? = null
    private lateinit var mCoin: Coin
    private val mainScope = MainScope()
    private lateinit var bnbAddress: String
    private val exchangeViewModel: ExchangeViewModel by inject(walletQualifier)
    private var checked = true

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
    }

    override fun initObserver() {
        super.initObserver()
        exchangeViewModel.flashExchange.observe(this, Observer {
            if (it.isSucceed()) {
                val result = it.data()
                toast("操作成功")

            } else {
                toast(it.error())
            }
        })
    }

    override fun initListener() {
        super.initListener()
        iv_check.setOnClickListener {
            if(checked) {
                iv_check.setImageResource(R.mipmap.ic_ex_nomal)
                checked = false
            }else {
                iv_check.setImageResource(R.mipmap.ic_ex_sel)
                checked = true
            }
        }
        btn_exchange.setOnClickListener {
            showPasswordDialog()
        }
    }

    override fun initData() {
        super.initData()
        mainScope.launch(Dispatchers.IO) {
            bnbAddress = BWallet.get().getAddress(Walletapi.TypeBnbString)
            withContext(Dispatchers.Main) {
                tv_bsc_address.text = bnbAddress
            }
        }


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
                dismiss()
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

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}