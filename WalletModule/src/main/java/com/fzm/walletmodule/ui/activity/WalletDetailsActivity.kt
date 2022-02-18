package com.fzm.walletmodule.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.event.CheckMnemEvent
import com.fzm.walletmodule.event.UpdatePasswordEvent
import com.fzm.walletmodule.manager.WalletManager
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.CommonDialogFragment
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.utils.ListUtils
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.isFastClick
import kotlinx.android.synthetic.main.activity_wallet_details.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.litepal.LitePal.find
import org.litepal.LitePal.select
import java.util.*
import kotlin.coroutines.resume

/**
 * 账户详情页面
 */
class WalletDetailsActivity : BaseActivity() {
    private var mPWallet: PWallet? = null
    private var mEditDialogFragment: EditDialogFragment? = null
    private var mPasswordDialogFragment: EditDialogFragment? = null
    private var mCommonDialogFragment: CommonDialogFragment? = null
    private var needUpdate = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_details)
        tvTitle.text = getString(R.string.title_wallet_details)
        EventBus.getDefault().register(this)
        initIntent()
        initListener()
    }

    override fun initIntent() {
        val pWalletId = intent.getLongExtra(PWallet.PWALLET_ID, 0L)
        mPWallet = find(PWallet::class.java, pWalletId)
    }

    override fun initListener() {
        tv_forget_password.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            val `in` = Intent(this, CheckMnemActivity::class.java)
            `in`.putExtra(PWallet.PWALLET_ID, mPWallet!!.id)
            startActivity(`in`)
        }
        updatePassword.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            val intent = Intent()
            intent.setClass(
                this,
                if (TextUtils.isEmpty(mPWallet!!.password)) SetPasswordActivity::class.java else ChangePasswordActivity::class.java
            )
            intent.putExtra(ChangePasswordActivity.PWALLET_ID, mPWallet!!.id)
            startActivity(intent)
        }
        outPriv.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            checkPassword(1)
        }
        updateName.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            updateWalletName()
        }
        outMnem.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            checkPassword(3)
        }
        delete.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            checkPassword(2)
        }
    }


    private fun updateWalletName() {
        if (mEditDialogFragment == null) {
            mEditDialogFragment = EditDialogFragment()
            mEditDialogFragment!!.setTitle(getString(R.string.my_wallet_detail_name))
            mEditDialogFragment!!.setInput(mPWallet!!.name)
            mEditDialogFragment!!.type = 1
            mEditDialogFragment!!.setInputType(InputType.TYPE_CLASS_TEXT)
            mEditDialogFragment!!.setRightButtonStr(getString(R.string.ok))
            mEditDialogFragment!!.setOnButtonClickListener(object :
                EditDialogFragment.OnButtonClickListener {
                override fun onRightButtonClick(v: View?) {
                    val input = mEditDialogFragment!!.etInput.text.toString().trim()
                    if (TextUtils.isEmpty(input)) {
                        ToastUtils.show(
                            this@WalletDetailsActivity,
                            getString(R.string.my_wallet_detail_name)
                        )
                        return
                    }
                    lifecycleScope.launch {
                        try {
                            BWallet.get().changeWalletName(input)
                            needUpdate = true
                            ToastUtils.show(
                                this@WalletDetailsActivity,
                                getString(R.string.my_wallet_modified_success)
                            )
                        } catch (e: Exception) {
//                            ToastUtils.show(
//                                this@WalletDetailsActivity,
//                                getString(R.string.my_wallet_detail_name_exist)
//                            )
                            ToastUtils.show(this@WalletDetailsActivity, e.message)
                            return@launch
                        }
                    }

                }

                override fun onLeftButtonClick(v: View?) {

                }

            })
        }
        mEditDialogFragment!!.showDialog(
            getString(R.string.my_wallet_detail_modify_name),
            supportFragmentManager
        )
    }

    /**
     * 校验密码
     * type   1 代表查看私钥   2 代表删除账户   3 代表查看助记词
     */
    private fun checkPassword(type: Int) {
        if (mPasswordDialogFragment == null) {
            mPasswordDialogFragment = EditDialogFragment()
            mPasswordDialogFragment!!.setTitle(getString(R.string.my_wallet_detail_password))
            mPasswordDialogFragment!!.setHint(getString(R.string.my_wallet_detail_password))
            mPasswordDialogFragment!!.setAutoDismiss(false)
            mPasswordDialogFragment!!.setType(1)
                .setRightButtonStr(getString(R.string.ok))
        }
        mPasswordDialogFragment!!.setOnButtonClickListener(object :
            EditDialogFragment.OnButtonClickListener {
            override fun onRightButtonClick(v: View?) {
                val password = mPasswordDialogFragment!!.etInput.text.toString().trim()
                if (TextUtils.isEmpty(password)) {
                    ToastUtils.show(
                        this@WalletDetailsActivity,
                        R.string.my_wallet_detail_password
                    )
                    return
                }
                mPasswordDialogFragment?.dismiss()
                val localPassword = mPWallet?.password
                showLoading()
                doAsync {
                    val result = GoWallet.checkPasswd(password, localPassword!!)
                    if (result) {
                        handlePasswordAfter(type, password)
                    } else {
                        uiThread {
                            ToastUtils.show(
                                this@WalletDetailsActivity,
                                getString(R.string.my_wallet_detail_wrong_password)
                            )
                            dismiss()
                        }
                    }
                }

            }
            override fun onLeftButtonClick(v: View?) {
            }
        })
        mPasswordDialogFragment?.showDialog("tag", supportFragmentManager)
    }

    private fun handlePasswordAfter(type: Int, password: String) {
        when (type) {
            1 -> {
                val mnem = getMnem(password)
                outPriv(mnem!!, password)
            }
            2 -> {
                lifecycleScope.launch {
                    BWallet.get().deleteWallet(password) { handleDelete() }
                    dismiss()
                    needUpdate = true
                    finish()
                }
            }
            3 -> {
                val mnem = getMnem(password)
                runOnUiThread {
                    dismiss()
                    WalletManager().exportMnem(this@WalletDetailsActivity, mnem!!, mPWallet!!)
                }

            }
        }
    }

    private fun getMnem(password: String): String? {
        return GoWallet.decMenm(GoWallet.encPasswd(password)!!, mPWallet!!.mnem)
    }

    private suspend fun handleDelete() = suspendCancellableCoroutine<Boolean> { cont ->
        dismiss()
        if (mCommonDialogFragment == null) {
            mCommonDialogFragment = CommonDialogFragment()
            mCommonDialogFragment!!.setResult(getString(R.string.my_wallet_detail_safe))
                .setResultColor(resources.getColor(R.color.red_common))
                .setResultDetails(getString(R.string.my_wallet_detail_delete_message))
                .setLeftButtonStr(getString(R.string.cancel))
                .setRightButtonStr(getString(R.string.ok))
                .setOnDismissListener { cont.resume(false) }
                .setOnButtonClickListener(object : CommonDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View?) {
                        cont.resume(false)
                    }

                    override fun onRightButtonClick(v: View?) {
                        cont.resume(true)
                    }
                })
        }
        mCommonDialogFragment?.show(
            supportFragmentManager,
            getString(R.string.my_wallet_detail_delete)
        )
    }

    fun outPriv(mnem: String, password: String) {
        val coinList: List<Coin> =
            select().where("pwallet_id = ?", java.lang.String.valueOf(mPWallet!!.id))
                .find(Coin::class.java)
        if (!ListUtils.isEmpty(coinList)) {
            runOnUiThread {
                dismiss()
                val walletManager = WalletManager()
                walletManager.chooseChain(this@WalletDetailsActivity, coinList)
                walletManager.setOnItemClickListener(object : WalletManager.OnItemClickListener {
                    override fun onItemClick(position: Int) {
                        if (position < coinList.size) {
                            val coin = coinList[position]
                            WalletManager().exportPriv(
                                this@WalletDetailsActivity,
                                coin.getPrivkey(coin.chain, mnem)
                            )
                        }
                    }
                })
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onUpdatePasswordEvent(event: UpdatePasswordEvent) {
        mPWallet = find(PWallet::class.java, mPWallet!!.id)
        mPWallet!!.update(mPWallet!!.id)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onCheckMnemEvent(event: CheckMnemEvent) {
        mPWallet = find(PWallet::class.java, mPWallet!!.id)
        mPWallet!!.update(mPWallet!!.id)
    }

    override fun finish() {
        if (needUpdate) {
            setResult(RESULT_OK)
        }
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}