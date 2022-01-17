package com.fzm.walletmodule.ui.base


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.widget.LoadingView
import com.fzm.walletmodule.utils.StatusBarUtil
import java.util.*

abstract class BaseActivity : AppCompatActivity() {

    //出现头部出现和状态栏非常近(部分手机toolbar展示一半)的情况需要在layout根布局新增：android:fitsSystemWindows="true"，参考activity_update_contacts.xml
    private val ACTION_FINISHALL: String = "action_finishall"
    protected var mCustomToobar = false
    protected var mStatusColor = Color.WHITE
    protected lateinit var toolbar: Toolbar
    protected lateinit var tvTitle: TextView
    private lateinit var loadingView: LoadingView
    protected var mConfigFinish = false
    private var mFinishReceiver: BroadcastReceiver? = null
    fun setStatusColor(res: Int) {
        mStatusColor = resources.getColor(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mConfigFinish) {
            initIntentFilter()
        }
        loadingView = LoadingView()
        StatusBarUtil.setStatusBarColor(this, mStatusColor, true)
        StatusBarUtil.StatusBarLightMode(this)
    }

    private fun initIntentFilter() {
        mFinishReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_FINISHALL) {
                    finish()
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_FINISHALL)
        registerReceiver(mFinishReceiver, intentFilter)
    }

    open fun closeSomeActivitys() {
        sendBroadcast(Intent(ACTION_FINISHALL))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mConfigFinish) {
            if (mFinishReceiver != null) {
                unregisterReceiver(mFinishReceiver)
            }
        }
    }

    fun showLoading() {
        loadingView.show(supportFragmentManager, "showLoading")
    }

    fun showLoading(full: Boolean) {
        loadingView.setFullscreen(full)
        loadingView.show(supportFragmentManager, "showLoading")
    }

    fun dismiss() {
        try {
            loadingView.dismiss()
        } catch (e: Exception) {
            Log.e("loadingView：", "loadingView为null")
        }

    }

    fun showInLoading() {
        val flLoading = findViewById<FrameLayout>(R.id.fl_loading)
        flLoading?.visibility = View.VISIBLE
    }

    fun disInLoading() {
        val flLoading = findViewById<FrameLayout>(R.id.fl_loading)
        flLoading?.visibility = View.GONE
    }


    fun useNightMode(isNight: Boolean) {
        if (isNight) {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )
        } else {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        recreate()
    }


    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        if (!mCustomToobar) {
            toolbar.title = ""
            tvTitle.text = title
        }
    }


    protected fun setCustomToobar(customToolbar: Toolbar, back: Int) {
        setSupportActionBar(customToolbar)
        customToolbar.setNavigationIcon(back)
        customToolbar.setBackgroundColor(mStatusColor)
        customToolbar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun setContentView(layoutResID: Int) {
        if (mCustomToobar) {
            super.setContentView(layoutResID)
        } else {
            val contentView = LinearLayout(this)
            contentView.orientation = LinearLayout.VERTICAL
            val layoutToolbar = FrameLayout(this)
            LayoutInflater.from(this).inflate(R.layout.layout_base_bar, layoutToolbar, true)
            toolbar = layoutToolbar.findViewById<Toolbar>(R.id.toolbar)
            tvTitle = layoutToolbar.findViewById<TextView>(R.id.tv_title)
            contentView.addView(layoutToolbar)
            LayoutInflater.from(this).inflate(layoutResID, contentView, true)
            super.setContentView(contentView)
            setSupportActionBar(toolbar)
            toolbar.setNavigationIcon(R.drawable.ic_back)
            toolbar.setNavigationOnClickListener { onBackPressed() }

            val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
            view.fitsSystemWindows = true
            view.setBackgroundResource(android.R.color.white)
        }
    }

    protected fun setToolBar(@IdRes toolBarId: Int, @IdRes titleId: Int) {
        setToolBar(toolBarId, titleId, R.drawable.ic_back_white)
    }

    protected fun setToolBar(@IdRes toolBarId: Int, @IdRes titleId: Int, @DrawableRes resId: Int) {
        toolbar = findViewById(toolBarId)
        tvTitle = findViewById(titleId)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayUseLogoEnabled(false)
            actionBar.setHomeAsUpIndicator(resId)
        }
        toolbar.setNavigationOnClickListener { onBackPressed(); }
    }

    protected open fun configWallets() {}
    protected open fun initView() {}
    protected open fun initIntent() {}
    protected open fun initData() {}
    protected open fun initListener() {}
    protected open fun initRefresh() {}
    protected open fun initObserver() {}

    //-----------------------------每隔5秒刷新一次余额----------------------------
    //默认都自动刷新
    protected var mDelayedRefresh = true
    private var mIsRefreshBalance: Boolean = false
    private val balanceHandler = Handler()
    private var balanceRunnable: Runnable? = null
    fun setRefreshBalance(refreshBalance: Boolean) {
        mIsRefreshBalance = refreshBalance
    }

    var DELAYED_TIME = (8 * 1000).toLong()
    fun delayedRefresh(isRefreshBalance: Boolean) {
        balanceRunnable = Runnable {
            refreshBalance()
            balanceHandler.postDelayed(balanceRunnable!!, DELAYED_TIME)
        }
    }

    private fun onResumeBalance() {
        if (mIsRefreshBalance) {
            delayedRefresh(mIsRefreshBalance)
        }
    }

    fun onPauseBalance() {
        if (mIsRefreshBalance) {
            balanceHandler.removeCallbacks(balanceRunnable!!)
        }

    }

    protected open fun refreshBalance() {

    }

    internal var activities: MutableList<AppCompatActivity>? = ArrayList()

    fun addAcitivity(activity: AppCompatActivity) {
        activities!!.add(activity)
    }

    fun removeAcitivity(activity: AppCompatActivity) {
        activities!!.remove(activity)
    }

    fun clearActivity() {
        if (activities != null) {
            for (activity in activities!!) {
                activity.finish()
            }
        }
        activities!!.clear()
    }

    open fun hideKeyboard() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(
            currentFocus!!.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    open fun hideKeyboard(view: View): Boolean {
        if (null == view) return false
        val inputManager = view.context.applicationContext
            .getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        // 即使当前焦点不在editText，也是可以隐藏的。
        return inputManager.hideSoftInputFromWindow(view.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS)
    }


}