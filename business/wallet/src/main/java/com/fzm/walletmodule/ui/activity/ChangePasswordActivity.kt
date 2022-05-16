package com.fzm.walletmodule.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import com.fzm.walletmodule.R
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.event.UpdatePasswordEvent
import com.fzm.walletmodule.listener.SoftKeyBoardListener
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.AppUtils
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.isFastClick
import kotlinx.android.synthetic.main.activity_change_password.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.doAsync
import org.litepal.LitePal.find

/**
 * 修改密码页面
 */
class ChangePasswordActivity : BaseActivity() {
    private var viewHeight = 0
    private var mPWalletId: Long = 0
    private var mPasswordHash: String? = null
    private var mPWallet: PWallet? = null
    var goChecked = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        btn_sure.viewTreeObserver.addOnPreDrawListener {
            viewHeight = btn_sure.height
            true
        }
        tvTitle.text = getString(R.string.change_password)
        initKeyBoardListener()
        initIntent()
        initView()
        initListener()
    }

    override fun initIntent() {
        mPWalletId = intent.getLongExtra(PWALLET_ID, 0)
        mPWallet = find(PWallet::class.java, mPWalletId)
        mPasswordHash = mPWallet?.password
    }

    override fun initView() {
        setHintSize(et_old_password, getString(R.string.my_change_password_old), 15)
        setHintSize(et_new_password, getString(R.string.my_change_password_new), 15)
        setHintSize(et_password_again, getString(R.string.my_change_password_again), 15)
    }

    private fun setHintSize(editText: EditText, hint: String, size: Int) {
        val spannableString = SpannableString(hint)
        val absoluteSizeSpan = AbsoluteSizeSpan(size, true)
        spannableString.setSpan(
            absoluteSizeSpan,
            0,
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        editText.hint = SpannableString(spannableString)
    }

    private fun changePassword() {
        val oldPassword: String = et_old_password.text.toString().trim { it <= ' ' }
        val newPassword: String = et_new_password.text.toString().trim { it <= ' ' }
        val passwordAgain: String = et_password_again.text.toString().trim { it <= ' ' }
        if (checked(oldPassword, newPassword, passwordAgain)) {
            showLoading()
            doAsync {
                if (!TextUtils.isEmpty(oldPassword)) {
                    if (!GoWallet.checkPasswd(oldPassword, mPasswordHash!!)) {
                        runOnUiThread {
                            dismiss()
                            ToastUtils.show(this@ChangePasswordActivity, getString(R.string.my_change_password_wrong), Gravity.CENTER)
                        }
                        return@doAsync
                    }
                }
                val pWallet = PWallet()
                val encPasswd = GoWallet.encPasswd(newPassword)
                val passwdHash = GoWallet.passwdHash(encPasswd!!)
                pWallet.password = passwdHash
                //同时更改助记词的加密
                val bOldPassword = GoWallet.encPasswd(oldPassword)
                val mnem = GoWallet.decMenm(bOldPassword!!, mPWallet!!.mnem)
                val encMenm = GoWallet.encMenm(encPasswd, mnem!!)
                pWallet.mnem = encMenm
                pWallet.update(mPWalletId)
                runOnUiThread {
                    dismiss()
                    ToastUtils.show(this@ChangePasswordActivity, getString(R.string.my_change_password_success), Gravity.CENTER)
                    Coin.mPriv = ""
                    EventBus.getDefault().post(UpdatePasswordEvent())
                    finish()
                }
            }
        }

    }

    private fun checked(oldPassword: String, newPassword: String, passwordAgain: String): Boolean {
        if (TextUtils.isEmpty(oldPassword)) {
            tv_wrong.visibility = View.VISIBLE
            return false
        } else {
            tv_wrong.visibility = View.INVISIBLE
        }
        if (TextUtils.isEmpty(newPassword) || !AppUtils.ispassWord(newPassword)) {
            tv_number_and_letter.setTextColor(resources.getColor(R.color.red_common))
            return false
        } else {
            tv_number_and_letter.setTextColor(resources.getColor(R.color.gray_99))
        }
        if (oldPassword == newPassword) {
            tv_same.visibility = View.VISIBLE
            tv_different.visibility = View.INVISIBLE
            return false
        }
        if (TextUtils.isEmpty(passwordAgain) || !AppUtils.ispassWord(passwordAgain) || newPassword != passwordAgain) {
            tv_different.visibility = View.VISIBLE
            tv_same.visibility = View.INVISIBLE
            return false
        } else {
            tv_different.visibility = View.INVISIBLE
        }
        return true
    }


    override fun initListener() {
        btn_sure.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            changePassword()
        }
    }

    //-----------------------------------按钮置于键盘上方处理----------------------------------------
    private fun initKeyBoardListener() {
        SoftKeyBoardListener.setListener(
            this,
            object : SoftKeyBoardListener.OnSoftKeyBoardChangeListener {
                override fun keyBoardShow(height: Int) {
                    showkeyBoard(height)
                    btn_sure.visibility = View.GONE
                }

                override fun keyBoardHide(height: Int) {
                    mLayoutBtn?.visibility = View.GONE
                    btn_sure.visibility = View.VISIBLE
                }
            })

    }


    private var mLayoutBtn: View? = null

    private fun showkeyBoard(height: Int) {
        if (mLayoutBtn == null) {
            mLayoutBtn =
                LayoutInflater.from(this@ChangePasswordActivity).inflate(R.layout.layout_btn, null)
            val btnCreate: Button = mLayoutBtn!!.findViewById(R.id.btn_create)
            btnCreate.text = getString(R.string.ok)
            val layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            btnCreate.setOnClickListener { changePassword() }
            rl_root.addView(mLayoutBtn, layoutParams)
        } else {
            mLayoutBtn!!.visibility = View.VISIBLE
        }
        if (currentLocation != null) {
            val screenHeight = window.decorView.height
            val delta: Int =
                screenHeight - (currentLocation!!.get(1) + currentView!!.height) - height - viewHeight
            if (delta < 0) {
                sv_root.post(Runnable { sv_root.scrollBy(0, viewHeight) })
            }
        }
    }


    var currentLocation: IntArray? = null
    var currentView: View? = null
    private val mEtPasswordLocation = IntArray(2)
    private val mEtPasswordAgainLocation = IntArray(2)

    @SuppressLint("ClickableViewAccessibility")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        et_new_password.getLocationOnScreen(mEtPasswordLocation)
        et_password_again.getLocationOnScreen(mEtPasswordAgainLocation)
        et_new_password.setOnTouchListener(OnTouchListener { v, event ->
            currentLocation = mEtPasswordLocation
            currentView = et_new_password
            false
        })
        et_password_again.setOnTouchListener(OnTouchListener { v, event ->
            currentLocation = mEtPasswordAgainLocation
            currentView = et_password_again
            false
        })
    }

    companion object {
        const val PWALLET_ID = "pwallet_id"
    }
}