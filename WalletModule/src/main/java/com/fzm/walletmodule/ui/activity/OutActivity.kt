package com.fzm.walletmodule.ui.activity


import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.lifecycle.Observer
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.AddressCheckUtils
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.RegularUtils
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.event.CaptureEvent
import com.fzm.walletmodule.event.TransactionsEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.ui.widget.RemarksTipsDialogView
import com.fzm.walletmodule.utils.*
import com.fzm.walletmodule.vm.OutViewModel
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_out.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.koin.android.ext.android.inject
import org.litepal.LitePal.find
import org.litepal.LitePal.where
import java.math.BigDecimal

class OutActivity : BaseActivity() {
    private var mFrom = 0
    private var mCoin: Coin? = null
    private var mPWallet: PWallet? = null
    private var mChainBean: Coin? = null
    private var mEtMoneyTextSize = 0f  //记录mEtMoney的字符大小初始值,用于清空mEtMoney输入框时重心设置回原始大小
    private var mToAddress = ""
    private var mMoneyStr = ""
    private var mFee: Double = 0.01   // 手续费
    private var mMoney: Double = 0.0
    private var mEditDialogFragment: EditDialogFragment? = null
    private var mPriv: String? = null
    private var mMaxEditLegth = 0
    private var mMiner: Miner? = null
    private val outViewModel: OutViewModel by inject(walletQualifier)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_out)
        initIntent()
        initView()
        initData()
        initListener()
    }

    override fun initIntent() {
        mCoin = intent.getSerializableExtra(Coin::class.java.simpleName) as Coin?
        mFrom = intent.getIntExtra(Constants.FROM, -1)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        EventBus.getDefault().register(this)
        mMaxEditLegth = (ScreenUtils.getScreenWidth(this) - ScreenUtils.dp2px(
            this,
            105f
        )) / 2 - ScreenUtils.dp2px(this, 5f)
        tv_coin_name.text = mCoin!!.uiName + getString(R.string.home_transfer)
        btn_out.setText(R.string.home_confirm_transfer_currency)
        if (FROM_SCAN == mFrom) {
            tv_other_address.setText(mCoin!!.scanAddress)
        }
        mPWallet = find(PWallet::class.java, mCoin!!.getpWallet().id)
        val chainBeans = where(
            "name = ? and pwallet_id = ?",
            mCoin!!.chain,
            (mCoin!!.getpWallet().id).toString()
        ).find(Coin::class.java)
        if (!ListUtils.isEmpty(chainBeans)) {
            mChainBean = chainBeans[0]
        }
        tv_wallet_name.text = mPWallet!!.name
        val balance = mCoin!!.balance.toDouble()
        tv_balance.text =
            getString(R.string.home_balance, DecimalUtils.subWithNum(balance, 4), mCoin!!.uiName)
        mEtMoneyTextSize = ScreenUtils.pxTosp(this, et_money.textSize)
    }

    override fun initData() {
        if ("TRX" == mCoin!!.chain) {
            ll_out_miner.visibility = View.GONE
            mFee = 0.0
        }
        outViewModel.getMiner.observe(this, Observer {
            dismiss()
            if (it.isSucceed()) {
                mMiner = it.data()
                handleFee()
            } else {
                ToastUtils.show(this, it.error())
            }
        })
        showLoading()
        outViewModel.getMiner(mCoin?.chain!!)
    }

    fun handleFee() {
        val min: String? = mMiner?.low
        val max: String? = mMiner?.high
        val average: String? = mMiner?.average
        val minLength: Int = DoubleUtils.dotLength(min)
        val maxLength: Int = DoubleUtils.dotLength(max)
        val averageLength: Int = DoubleUtils.dotLength(average)
        val blength = if (minLength > averageLength) minLength else averageLength
        val length = if (blength > 8) 8 else blength
        val minInt: Int = DoubleUtils.doubleToInt(min, length)
        val maxInt: Int = DoubleUtils.doubleToInt(max, length)
        val averageInt: Int = DoubleUtils.doubleToInt(average, length)
        seekbar_money.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean,
            ) {
                mFee = DoubleUtils.intToDouble(progress + minInt, length)
                val rmb: Double = mChainBean?.rmb!! * mFee
                val rmbValue = BigDecimal(rmb.toString()).setScale(4, BigDecimal.ROUND_DOWN)

                // updateFee(progress, length)
                tv_fee.text = DecimalUtils.subZero(DecimalUtils.formatDouble(mFee))
                tv_fee_coin_name.text = mCoin!!.chain
                tv_fee_rmb.text = " ≈ ¥${rmbValue}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        seekbar_money.max = maxInt
        //初始进度（推荐款工费）
        seekbar_money.progress = maxInt / 2
    }

    override fun initListener() {
        iv_chain_title.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            RemarksTipsDialogView(this, false)
        }
        iv_scan.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            startActivity<CaptureCustomActivity>()
        }
        btn_out.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            mToAddress = tv_other_address.text.toString().trim { it <= ' ' }
            mMoneyStr = et_money.text.toString().trim { it <= ' ' }
            if (checkAddressAndMoney(mToAddress, mMoneyStr)) {
                if (check()) {
                    showPasswordDialog()
                }
            }
        }
        et_money.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                var editable =
                    castParam(s, "onTextChanged", 0, "handleMoney", 0, Editable::class.java)
                formatEditable(editable)
            }
        })

    }

    private fun showPasswordDialog() {
        if (mEditDialogFragment == null) {
            mEditDialogFragment = EditDialogFragment()

            mEditDialogFragment!!.setType(1)
                .setRightButtonStr(getString(R.string.home_confirm))
                .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View?) {}
                    override fun onRightButtonClick(v: View?) {
                        val etPassword = mEditDialogFragment!!.etInput
                        val password =
                            etPassword.text.toString().trim { it <= ' ' }
                        val localPassword = mCoin!!.getpWallet().password
                        payCheck(password, localPassword)
                    }
                })
        }
        mEditDialogFragment!!.showDialog("tag", supportFragmentManager)
    }

    /**
     * 支付密码校验
     * @param password String
     * @param localPassword String?
     */
    private fun payCheck(password: String, localPassword: String?) {
        showLoading()
        doAsync {
            val result = GoWallet.checkPasswd(password, localPassword!!)
            if (result) {
                configTransaction(password)
            } else {
                runOnUiThread {
                    ToastUtils.show(this@OutActivity, R.string.home_pwd_input_error)
                    dismiss()
                }
            }
        }
    }

    private fun configTransaction(password: String) {
        mMoney = mMoneyStr.toDouble()
        val mnem = GoWallet.decMenm(GoWallet.encPasswd(password)!!, mCoin!!.getpWallet().mnem)
        mPriv = mCoin!!.getPrivkey(mCoin!!.chain, mnem)
        handleTransactions()
    }

    private fun handleTransactions() {
        val tokensymbol = if (mCoin!!.name == mCoin!!.chain) "" else mCoin!!.name
        //构造交易
        val createRaw = GoWallet.createTran(
            mCoin!!.chain,
            mCoin!!.address,
            mToAddress,
            mMoney,
            mFee,
            et_note.text.toString(),
            tokensymbol
        )
        val stringResult = JSON.parseObject(createRaw, StringResult::class.java)
        val createRawResult: String? = stringResult.result
        if (TextUtils.isEmpty(createRawResult)) {
            return
        }
        //签名交易
        val signtx = GoWallet.signTran(mCoin!!.chain, createRawResult!!, mPriv!!)
        if (TextUtils.isEmpty(signtx)) {
            return
        }
        //发送交易
        val sendRawTransaction = GoWallet.sendTran(mCoin!!.chain, signtx!!, tokensymbol)
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
            EventBus.getDefault().post(TransactionsEvent(mCoin!!, tv_other_address.text.toString()))
            finish()
        }

    }


    private fun parseResult(json: String): StringResult? {
        return if (TextUtils.isEmpty(json)) {
            null
        } else Gson().fromJson(json, StringResult::class.java)
    }

    /**
     * 校验地址和币种金额
     * @param toAddress String  地址
     * @param moneyStr String  币种数量
     * @return Boolean
     */
    private fun checkAddressAndMoney(toAddress: String, moneyStr: String): Boolean {
        if (TextUtils.isEmpty(toAddress)) {
            ToastUtils.show(this, R.string.home_please_input_receipt_address)
            return false
        } else if (TextUtils.isEmpty(moneyStr)) {
            ToastUtils.show(this, R.string.home_please_input_amount)
            return false
        } else if (toAddress == mCoin!!.address) {
            ToastUtils.show(this, R.string.home_receipt_send_address_is_same)
            return false
        } else if (toAddress.length < 20 || !RegularUtils.isAddress(toAddress)) {
            ToastUtils.show(this, R.string.home_receipt_address_is_illegal)
            return false
        } else if (!AddressCheckUtils.check(mCoin!!.chain, toAddress)) {
            ToastUtils.show(this, getString(R.string.home_receipt_address_is_illegal))
            return false
        }
        return true
    }

    private fun check(): Boolean {
        val money = mMoneyStr.toDouble()
        val balance = mCoin!!.balance.toDouble()
        if (money <= 0) {
            ToastUtils.show(this, R.string.home_input_greater_than_zero_amount)
            return false
        }
        if (mCoin?.name == mCoin?.chain) {
            return checkBalance(money, balance)
        } else {
            //token或者coins,由于BTY是代扣的
            if (isBTYCoins()) {
                return checkBalance(money, balance)
            } else if (isBTYToken()) {
                val coins = where(
                    "platform = ? and treaty = ? and pwallet_id = ? ",
                    mCoin!!.platform,
                    "2",
                    (mCoin!!.getpWallet().id).toString()
                ).find(Coin::class.java)
                if (!ListUtils.isEmpty(coins)) {
                    val coin = coins[0]
                    val coinsBalance = coin.balance.toDouble()
                    return checkChainToken(money, balance, coinsBalance)
                }
            } else {
                //BTY的主链Token和其他主链的Token
                val chainBalance: Double =
                    if (mChainBean == null) 0.0 else mChainBean!!.balance.toDouble()
                return checkChainToken(money, balance, chainBalance)
            }
        }
        return true
    }

    private fun checkBalance(money: Double, balance: Double): Boolean {
        if (money + mFee > balance) {
            ToastUtils.show(this, getString(R.string.home_balance_insufficient))
            return false
        }
        return true
    }

    private fun isBTYCoins(): Boolean {
        return GoWallet.isBTYChild(mCoin!!) && "2" == mCoin!!.treaty
    }


    private fun isBTYToken(): Boolean {
        return GoWallet.isBTYChild(mCoin!!) && "1" == mCoin!!.treaty
    }

    //判断转币金额<余额和手续费<主链余额或coins余额
    private fun checkChainToken(money: Double, balance: Double, chainBalance: Double): Boolean {
        if (money > balance) {
            ToastUtils.show(this, getString(R.string.home_balance_insufficient))
            return false
        } else if (mFee > chainBalance) {
            ToastUtils.show(this, getString(R.string.fee_not_enough))
            return false
        }
        return true
    }

    /**
     * 格式化editable
     *
     * @param editable
     * @return
     */
    private fun formatEditable(editable: Editable): Editable {
        val temp = editable.toString()
        val posDot = temp.indexOf(".")
        //直接输入小数点的情况
        if (posDot == 0) {
            editable.insert(0, "0")
            return editable
        }
        //连续输入0
        if (temp == "00") {
            editable.delete(1, 2)
            return editable
        }
        //输入"08" 等类似情况
        if (temp.startsWith("0") && temp.length > 1 && (posDot == -1 || posDot > 1)) {
            editable.delete(0, 1)
            return editable
        }
        //不包含小数点 不限制小数点前位数
        if (posDot < 0) {
            return editable
        }
        //如果包含小数点 限制小数点后位数
        if (temp.length - posDot - 1 > 4) {
            editable.delete(posDot + 4 + 1, posDot + 4 + 2) //删除小数点后多余位数
        }
        return editable
    }


    fun <T> castParam(
        value: Any?,
        from: String,
        fromPos: Int,
        to: String,
        toPos: Int,
        cls: Class<T>,
    ): T {
        return try {
            cls.cast(value)
        } catch (e: ClassCastException) {
            throw IllegalStateException(
                "Parameter #" + (fromPos + 1) + " of method '" + from + "' was of the wrong type for parameter #"
                        + (toPos + 1) + " of method '" + to + "'. See cause for more info.", e
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: CaptureEvent) {
        val text: String = event.text
        if (text.contains(",")) {
            val split: Array<String> = text.split(",").toTypedArray()
            val netId = split[0]
            val money = split[1]
            val address = split[2]
            if (!TextUtils.isEmpty(money)) {
                et_money.setText(money)
            }
            tv_other_address.setText(address)
        } else {
            tv_other_address.setText(text)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    companion object {
        //扫码
        const val FROM_SCAN = 3

        //交易详情
        const val FROM_TRANSACTION = 1
    }

}