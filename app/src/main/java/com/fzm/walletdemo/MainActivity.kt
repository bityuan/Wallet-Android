package com.fzm.walletdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.fzm.walletmodule.db.entity.PWallet
import com.fzm.walletmodule.event.MainCloseEvent
import com.fzm.walletmodule.event.MyWalletEvent
import com.fzm.walletmodule.ui.fragment.WalletFragment
import com.fzm.walletmodule.ui.fragment.WalletIndexFragment
import com.fzm.walletmodule.utils.GoWallet
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.litepal.LitePal.count

class MainActivity : AppCompatActivity() {
    private var homeFragment: WalletFragment? = null
    private var mWalletIndexFragment: WalletIndexFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EventBus.getDefault().register(this)
        setTabSelection()

    }


    private fun setTabSelection() {
        // 开启一个Fragment事务
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        // 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
        hideFragments(fragmentTransaction)
        val count: Int = count(PWallet::class.java)
        if (count > 0) {
            showWalletFragment(fragmentTransaction)
        } else {
            showWalletIndexFragment(fragmentTransaction)
        }
    }

    private fun hideFragments(transaction: FragmentTransaction) {
        if (homeFragment != null) {
            transaction.hide(homeFragment!!)
        }
        if (mWalletIndexFragment != null) {
            transaction.hide(mWalletIndexFragment!!)
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setTabSelection()
    }

    private var mPWallet: PWallet? = null

    //回调 - 我的账户
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMyWalletEvent(event: MyWalletEvent) {
        if (null == event.mPWallet) {
            return
        } else {
            setTabSelection()
            mPWallet = event.mPWallet
            PWallet.setUsingWallet(mPWallet)
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MainCloseEvent) {
        setTabSelection()
    }


    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}