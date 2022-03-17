package com.fzm.walletdemo

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.alpha.EmptyWallet
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.event.InitPasswordEvent
import com.fzm.walletmodule.event.MainCloseEvent
import com.fzm.walletmodule.event.MyWalletEvent
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.fragment.WalletFragment
import com.fzm.walletmodule.ui.fragment.WalletIndexFragment
import com.fzm.walletmodule.utils.WalletUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineScope
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.litepal.LitePal.count

class MainActivity : BaseActivity() {
    private var homeFragment: WalletFragment? = null
    private var mWalletIndexFragment: WalletIndexFragment? = null
    private var mExploreFragment: ExploreFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomToobar = true
        setStatusColor(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EventBus.getDefault().register(this)
        initView()
        setTabSelection(0)
        Constants.setCoins(DEFAULT_COINS)
        lifecycleScope.launchWhenResumed {
            BWallet.get().current.collect {
                if (it is EmptyWallet) {
                    setTabSelection(0)
                }
            }
        }
    }


    val DEFAULT_COINS = listOf(
        Coin().apply {
            name = "USDT"
            chain = "BNB"
            platform = "bnb"
            netId = "694"
        }, Coin().apply {
            name = "BTY"
            chain = "BTY"
            platform = "bty"
            netId = "154"
        }, Coin().apply {
            name = "YCC"
            chain = "BNB"
            platform = "bnb"
            netId = "695"
        }, Coin().apply {
            name = "BNB"
            chain = "BNB"
            platform = "bnb"
            netId = "641"
        }, Coin().apply {
            name = "TRX"
            chain = "TRX"
            platform = "trx"
            netId = "1"
        }, Coin().apply {
            name = "USDT"
            chain = "TRX"
            platform = "trx"
            netId = "600"
        }, Coin().apply {
            name = "ETH"
            chain = "ETH"
            platform = "ethereum"
            netId = "90"
        }, Coin().apply {
            name = "BTY"
            chain = "BNB"
            platform = "bnb"
            netId = "705"
        }
    )

    override fun initView() {
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.fragment_home -> {
                    Log.e("MainAc", "fragment_home")
                    setTabSelection(0)
                }
                R.id.fragment_explore -> {
                    Log.e("MainAc", "fragment_explore")
                    setTabSelection(1)
                }
            }
            true
        }
    }


    private fun setTabSelection(index: Int) {
        // 开启一个Fragment事务
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        // 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
        hideFragments(fragmentTransaction)
        when (index) {
            0 -> {
                val count: Int = count(PWallet::class.java)
                if (count > 0) {
                    showWalletFragment(fragmentTransaction)
                } else {
                    showWalletIndexFragment(fragmentTransaction)
                }
            }
            1 -> {
                showExploreFragment(fragmentTransaction)
            }
        }

    }

    private fun hideFragments(transaction: FragmentTransaction) {
        if (homeFragment != null) {
            transaction.hide(homeFragment!!)
        }
        if (mWalletIndexFragment != null) {
            transaction.hide(mWalletIndexFragment!!)
        }
        if (mExploreFragment != null) {
            transaction.hide(mExploreFragment!!)
        }
    }


    private fun showWalletFragment(fragmentTransaction: FragmentTransaction) {
        if (homeFragment != null) {
            fragmentTransaction.show(homeFragment!!)
        } else {

            if (homeFragment == null) {
                homeFragment = WalletFragment()
                fragmentTransaction.add(R.id.fl_tabcontent, homeFragment!!, "homeFragment")
            } else {
                fragmentTransaction.show(homeFragment!!)
            }
        }
        fragmentTransaction.commitAllowingStateLoss()
    }


    private fun showWalletIndexFragment(fragmentTransaction: FragmentTransaction) {
        if (mWalletIndexFragment != null) {
            fragmentTransaction.show(mWalletIndexFragment!!)
        } else {
            if (mWalletIndexFragment == null) {
                mWalletIndexFragment = WalletIndexFragment()
                fragmentTransaction.add(
                    R.id.fl_tabcontent,
                    mWalletIndexFragment!!,
                    "WalletIndexFragment"
                )
            } else {
                fragmentTransaction.show(mWalletIndexFragment!!)
            }
        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun showExploreFragment(fragmentTransaction: FragmentTransaction) {
        if (mExploreFragment != null) {
            fragmentTransaction.show(mExploreFragment!!)
        } else {
            if (mExploreFragment == null) {
                mExploreFragment = ExploreFragment()
                fragmentTransaction.add(
                    R.id.fl_tabcontent,
                    mExploreFragment!!,
                    "ExploreFragment"
                )
            } else {
                fragmentTransaction.show(mExploreFragment!!)
            }
        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setTabSelection(0)
    }

    private var mPWallet: PWallet? = null

    //回调 - 我的账户
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMyWalletEvent(event: MyWalletEvent) {
        if (null == event.mPWallet) {
            return
        } else {
            setTabSelection(0)
            mPWallet = event.mPWallet
            WalletUtils.setUsingWallet(mPWallet)
        }

        if (!event.isChoose) {
            val privkey = BWallet.get().getBtyPrikey()
            Log.v("tag", privkey + "")
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onInitPasswordEvent(event: InitPasswordEvent) {
        val password = event.password
        Log.v("zx", password)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MainCloseEvent) {
        setTabSelection(0)
    }


    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}