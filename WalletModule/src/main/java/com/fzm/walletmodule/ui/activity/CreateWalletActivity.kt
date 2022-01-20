package com.fzm.walletmodule.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import com.fzm.walletmodule.R
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.listener.SoftKeyBoardListener
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.*
import com.snail.antifake.jni.EmulatorDetectUtil
import kotlinx.android.synthetic.main.activity_create_wallet.*
import org.litepal.LitePal

/**
 * 创建账户页面
 */
class CreateWalletActivity : BaseActivity() {
    private var viewHeight = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)
        initView()
        initKeyBoardListener()
        initListener()
    }

    override fun initView() {
        super.initView()
        et_name.setSelection(et_name.text.length)
        title = ""
        btn_create.viewTreeObserver.addOnPreDrawListener {
            viewHeight = btn_create.height
            true
        }
        setLineFocusChage(et_name, line_name)
        setLineFocusChage(et_password, line_password)
        setLineFocusChage(et_password_again, line_password_again)
    }

    private fun setLineFocusChage(editText: EditText, lineView: View) {
        editText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                lineView.setBackgroundResource(R.color.color_333649)
                val pra: ViewGroup.LayoutParams = line_name.layoutParams
                pra.height = ScreenUtils.dp2px(this, 1f)
                lineView.layoutParams = pra
            } else {
                lineView.setBackgroundResource(R.color.lineColor)
                val pra: ViewGroup.LayoutParams = line_name.layoutParams
                pra.height = 1
                lineView.layoutParams = pra
            }
            if (v.id == R.id.et_password) {
                tv_prompt.visibility = View.VISIBLE
            } else {
                tv_prompt.visibility = View.INVISIBLE
            }
        }
    }

    override fun initListener() {
        super.initListener()
        btn_create.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            hideKeyboard()
            gotoFinishTask()
        }
    }

    private fun gotoFinishTask() {
        if (EmulatorDetectUtil.isEmulator(this)) {
            ToastUtils.show(this, "检测到您使用模拟器创建账户，请切换到真机")
        } else {
            finishTask()
        }
    }

    private fun finishTask() {
        val name: String = et_name.text.toString().trim { it <= ' ' }
        val password: String = et_password.text.toString().trim { it <= ' ' }
        val passwordAgain: String = et_password_again.text.toString().trim { it <= ' ' }
        if (checked(name, password, passwordAgain)) {
            val intent = Intent(this, CreateMnemActivity::class.java)
            val wallet = PWallet()
            wallet.name = name
            wallet.password = password
            intent.putExtra(PWallet::class.java.simpleName, wallet)
            startActivity(intent)
        }

    }

    private fun checked(name: String, password: String, passwordAgain: String): Boolean {
        val pWallets = LitePal.where("name = ?", name).find(PWallet::class.java)

        var checked = true

        if (TextUtils.isEmpty(name)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name),Gravity.CENTER)
            checked = false
        } else if (!ListUtils.isEmpty(pWallets)) {
            ToastUtils.show(this, getString(R.string.my_wallet_detail_name_exist),Gravity.CENTER)
            checked = false
        } else if (TextUtils.isEmpty(password)) {
            ToastUtils.show(this, getString(R.string.my_wallet_set_password),Gravity.CENTER)
            checked = false
        } else if (password.length < 8 || password.length > 16) {
            ToastUtils.show(this, getString(R.string.my_create_letter),Gravity.CENTER)
            checked = false
            tv_prompt.setTextColor(resources.getColor(R.color.color_EA2551))
        } else if (TextUtils.isEmpty(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_change_password_again),Gravity.CENTER)
            checked = false
        } else if (password != passwordAgain) {
            ToastUtils.show(this, getString(R.string.my_set_password_different),Gravity.CENTER)
            tv_tip_error.visibility = View.VISIBLE
            checked = false
        } else if (!AppUtils.ispassWord(password) || !AppUtils.ispassWord(passwordAgain)) {
            ToastUtils.show(this, getString(R.string.my_set_password_number_letter),Gravity.CENTER)
            checked = false
        }
        return checked
    }


    //-----------------------------------按钮置于键盘上方处理----------------------------------------
    private var mLayoutBtn: View? = null
    private fun initKeyBoardListener() {
        SoftKeyBoardListener.setListener(
            this,
            object : SoftKeyBoardListener.OnSoftKeyBoardChangeListener {
                override fun keyBoardShow(height: Int) {
                    showkeyBoard(height)
                    btn_create.visibility = View.GONE
                    hideLargeTitle(tv_large_title, getString(R.string.my_create_wallet))
                }


                override fun keyBoardHide(height: Int) {
                    mLayoutBtn!!.visibility = View.GONE
                    btn_create.visibility = View.VISIBLE
                    showLargeTitle(tv_large_title, "")
                }
            })

    }

    private fun hideLargeTitle(tvLargeTitle: TextView?, string: String) {
        tvLargeTitle!!.visibility = View.GONE
        tvTitle.text = string
    }
    private fun showLargeTitle(tvLargeTitle: TextView?, s: String) {
        tvLargeTitle!!.visibility = View.VISIBLE
        tvTitle.text = s
    }

    private fun showkeyBoard(height: Int) {
        if (mLayoutBtn == null) {
            mLayoutBtn =
                LayoutInflater.from(this@CreateWalletActivity).inflate(R.layout.layout_btn, null)
            val btnCreate = mLayoutBtn?.findViewById<Button>(R.id.btn_create)
            val layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            btnCreate?.setOnClickListener { finishTask() }
            rl_root.addView(mLayoutBtn, layoutParams)
        } else {
            mLayoutBtn?.visibility = View.VISIBLE
        }
        if (currentLocation != null) {
            val screenHeight = window.decorView.height
            val delta: Int =
                screenHeight - (currentLocation!![1] + currentView!!.height) - height - viewHeight
            if (delta < 0) {
                sv_root.post(Runnable { sv_root.scrollBy(0, viewHeight) })
            }
        }
    }

    private var currentLocation: IntArray? = null
    var currentView: View? = null
    private val mEtPasswordLocation = IntArray(2)
    private val mEtPasswordAgainLocation = IntArray(2)

    @SuppressLint("ClickableViewAccessibility")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        et_password.getLocationOnScreen(mEtPasswordLocation)
        et_password_again.getLocationOnScreen(mEtPasswordAgainLocation)

        et_password.setOnTouchListener(View.OnTouchListener { v: View?, event: MotionEvent? ->
            currentLocation = mEtPasswordLocation
            currentView = et_password
            false
        })

        et_password_again.setOnTouchListener(View.OnTouchListener { v: View?, event: MotionEvent? ->
            currentLocation = mEtPasswordAgainLocation
            currentView = et_password_again
            false
        })
    }
}