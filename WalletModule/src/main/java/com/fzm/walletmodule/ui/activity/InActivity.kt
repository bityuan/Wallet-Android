package com.fzm.walletmodule.ui.activity

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fzm.walletmodule.R
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.utils.*
import com.king.zxing.util.CodeUtils
import kotlinx.android.synthetic.main.activity_in.*
import kotlinx.android.synthetic.main.layout_in_tab.*
import org.jetbrains.anko.doAsync
import org.litepal.LitePal.find

class InActivity : BaseActivity() {
    private var mCoin: Coin? = null
    private var mInputMoney = "0"
    private var mPutMoneyDialog: EditDialogFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        mStatusColor = Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in)
        setToolBar(R.id.toolbar, R.id.tv_title)
        initIntent()
        initView()
        setRefreshBalance(mDelayedRefresh)
        initListener()
    }

    override fun initIntent() {
        mCoin = intent.getSerializableExtra(Coin::class.java.simpleName) as Coin?
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        tvTitle.text = mCoin!!.uiName + getString(R.string.home_receipt_currency)
        val pWallet: PWallet = find(PWallet::class.java, mCoin!!.getpWallet().id)
        tv_wallet_name.text = pWallet.name
        tv_money.text = mCoin?.balance + mCoin?.name
        tv_address.text = mCoin?.address
        configQRCode(mCoin!!.address)
        tv_address.text = HtmlUtils.change4(tv_address.text.toString())
        tv_in_name.text = getString(R.string.in_p_in) + " " + mCoin!!.name
        tv_bi_tip.text = getString(R.string.in_bi_tip, getString(R.string.app_name))
    }

    private fun configQRCode(address: String) {
        if (TextUtils.isEmpty(mCoin?.icon)) {
            val bitmap: Bitmap = CodeUtils.createQRCode(address, 190)
            iv_my_wallet.setImageBitmap(bitmap)
        } else {
            Glide.with(this).asBitmap().load(mCoin!!.icon)
                .into(object : SimpleTarget<Bitmap?>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        val bitmap: Bitmap = CodeUtils.createQRCode(address, 190, resource)
                        iv_my_wallet.setImageBitmap(bitmap)
                    }
                })
        }
    }

    override fun initListener() {
        fl_status.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            if (iv_status.isSelected) {
                iv_status.isSelected = false
                tv_money.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                iv_status.isSelected = true
                //隐藏密码
                tv_money.transformationMethod = PasswordTransformationMethod.getInstance()
            }
        }
        iv_my_wallet.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            copyAddress()
        }
        tv_address.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            copyAddress()
        }
        tv_put_money.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            showPutMoneydialog()
        }
    }

    private fun copyAddress() {
        val cm = this@InActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val mClipData =
            ClipData.newPlainText("Label", tv_address.text.toString().trim { it <= ' ' })
        cm.setPrimaryClip(mClipData)
        ToastUtils.show(this, R.string.copy_success)
    }

    fun showPutMoneydialog() {
        if (mPutMoneyDialog == null) {
            mPutMoneyDialog = EditDialogFragment()
            mPutMoneyDialog!!.setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            mPutMoneyDialog!!.setAutoDismiss(false)
            mPutMoneyDialog!!.setHint(getString(R.string.in_p_amount))
            mPutMoneyDialog!!.setTitle(getString(R.string.in_p_amount))
            mPutMoneyDialog!!.setRightButtonStr(getString(R.string.ok))
            mPutMoneyDialog!!.type = 1
            mPutMoneyDialog!!.setOnButtonClickListener(object :
                EditDialogFragment.OnButtonClickListener {
                @SuppressLint("SetTextI18n")
                override fun onRightButtonClick(v: View?) {
                    val str = mPutMoneyDialog!!.etInput.text.toString()
                    if (TextUtils.isEmpty(str)) {
                        ToastUtils.show(this@InActivity, getString(R.string.in_p_amount))
                        return
                    }
                    mPutMoneyDialog!!.dismiss()
                    mInputMoney = str
                    tv_in_name.text = getString(R.string.in_set_money) + " " + " " + mCoin?.name
                    tv_put_money.text = getString(R.string.in_r_money)
                    val qrStr: String = if (TextUtils.equals("0", mInputMoney)) {
                        mCoin!!.address
                    } else {
                        mInputMoney + "," + mCoin!!.address
                    }
                    configQRCode(qrStr)
                }

                override fun onLeftButtonClick(v: View?) {}

            })
        }
        mPutMoneyDialog!!.show(supportFragmentManager, "")
    }

    override fun refreshBalance() {
        super.refreshBalance()
        doAsync {
            val handleBalance = GoWallet.handleBalance(mCoin!!)
            runOnUiThread {
                tv_money.text = handleBalance + mCoin?.name
            }
        }
    }
}