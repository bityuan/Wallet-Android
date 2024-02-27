package com.fzm.walletmodule.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.IPConfig.Companion.FIGER_KEY
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.IAppTypeProvider
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.ROUTE_APP_TYPE
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.db.entity.PWallet.TYPE_ADDR_KEY
import com.fzm.wallet.sdk.db.entity.PWallet.TYPE_PRI_KEY
import com.fzm.wallet.sdk.db.entity.PWallet.TYPE_RECOVER
import com.fzm.wallet.sdk.utils.AESForPayUtil
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityWalletDetailsBinding
import com.fzm.walletmodule.databinding.DialogCommonBinding
import com.fzm.walletmodule.databinding.DialogEditBinding
import com.fzm.walletmodule.event.CheckMnemEvent
import com.fzm.walletmodule.event.UpdatePasswordEvent
import com.fzm.walletmodule.manager.WalletManager
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.FingerManager
import com.fzm.walletmodule.utils.ListUtils
import com.fzm.walletmodule.utils.isFastClick
import com.kongzue.dialogx.dialogs.MessageDialog
import com.tencent.soter.core.model.SoterErrCode
import com.tencent.soter.wrapper.SoterWrapperApi
import com.tencent.soter.wrapper.wrap_callback.SoterProcessAuthenticationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.LitePal.find
import org.litepal.LitePal.select
import org.litepal.extension.delete
import org.litepal.extension.find
import org.litepal.extension.findFirst

@Route(path = RouterPath.WALLET_WALLET_DETAILS)
class WalletDetailsActivity : BaseActivity() {

    @JvmField
    @Autowired(name = PWallet.PWALLET_ID)
    var walletid: Long = 0
    private var mPWallet: PWallet? = null
    private var needUpdate = false
    private val binding by lazy { ActivityWalletDetailsBinding.inflate(layoutInflater) }
    private val editBinding by lazy { DialogEditBinding.inflate(layoutInflater) }
    private val editDialog by lazy {
        AlertDialog.Builder(this).setView(editBinding.root).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
    private val commonBinding by lazy { DialogCommonBinding.inflate(layoutInflater) }
    private val commonDialog by lazy {
        AlertDialog.Builder(this).setView(commonBinding.root).create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = getString(R.string.title_wallet_details)
        initView()
        EventBus.getDefault().register(this)
        initListener()
        initSort()
        configWallets()
    }


    override fun initView() {
        super.initView()
        tvTitle.text = getString(R.string.title_wallet_details)
    }

    override fun configWallets() {
        super.configWallets()
        val navigation =
            ARouter.getInstance().build(ROUTE_APP_TYPE).navigation() as IAppTypeProvider
        if (navigation.getAppType() != IPConfig.APP_MY_DAO) {
            binding.tvNewRecoverAddress.visibility = View.GONE
        }

        lifecycleScope.launch(Dispatchers.IO) {
            mPWallet = find(PWallet::class.java, walletid)
            withContext(Dispatchers.Main) {
                mPWallet?.let {
                    binding.switchFingerPay.setCheckedNoEvent(it.fingerState == PWallet.OPEN)
                    when (it.type) {
                        TYPE_PRI_KEY -> {
                            binding.tvForgetPassword.visibility = View.GONE
                            binding.tvOutMnem.visibility = View.GONE
                            binding.tvNewRecoverAddress.visibility = View.GONE
                        }

                        TYPE_ADDR_KEY -> {
                            binding.tvForgetPassword.visibility = View.GONE
                            binding.tvUpdatePassword.visibility = View.GONE
                            binding.tvOutMnem.visibility = View.GONE
                            binding.tvOutPriv.visibility = View.GONE
                            binding.tvOutPub.visibility = View.GONE
                            binding.tvNewRecoverAddress.visibility = View.GONE
                            binding.llFingerPay.visibility = View.GONE
                        }

                        TYPE_RECOVER -> {
                            binding.tvForgetPassword.visibility = View.GONE
                            binding.tvOutMnem.visibility = View.GONE
                            binding.tvNewRecoverAddress.visibility = View.GONE
                            binding.tvOutPriv.visibility = View.GONE
                            binding.tvOutPub.visibility = View.GONE
                            binding.llFingerPay.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun initSort() {
        SoterWrapperApi.prepareAuthKey(
            { result ->
                logDebug("init sort = $result")
                if (result.errCode == SoterErrCode.ERR_OK) {
                    binding.llFingerPay.visibility = View.VISIBLE
                } else {
                    binding.llFingerPay.visibility = View.GONE
                }

            }, false, true, FIGER_KEY, null, null
        )

    }


    override fun initListener() {
        binding.tvForgetPassword.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            mPWallet?.let {
                ARouter.getInstance().build(RouterPath.WALLET_CHECK_MNEM)
                    .withLong(PWallet.PWALLET_ID, it.id).navigation()
            }

        }
        binding.tvUpdatePassword.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            mPWallet?.let {
                ARouter.getInstance()
                    .build(if (it.password.isNullOrEmpty()) RouterPath.WALLET_SET_PASSWORD else RouterPath.WALLET_CHANGE_PASSWORD)
                    .withLong(PWallet.PWALLET_ID, it.id).navigation()
            }
        }
        binding.tvOutPriv.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            checkPassword(1)
        }
        binding.tvOutPub.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            outPub()
        }
        binding.tvUpdateName.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            updateWalletName()
        }
        binding.tvOutMnem.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            checkPassword(3)
        }
        binding.tvDelete.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            mPWallet?.let {
                if (it.type == TYPE_ADDR_KEY) {
                    doingDelete()
                } else {
                    checkPassword(2)
                }
            }


        }

        binding.tvNewRecoverAddress.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_NEW_RECOVER_ADDRESS)
                .withLong(PWallet.PWALLET_ID, walletid).navigation()
        }

        binding.switchFingerPay.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.switchFingerPay.setCheckedNoEvent(false)
                checkPassword(4)
            } else {
                binding.switchFingerPay.setCheckedNoEvent(true)
                val dialog = MessageDialog.build()
                dialog.title = getString(R.string.close_finger_tip)
                dialog.cancelButton = getString(R.string.cancel)
                dialog.okButton = getString(R.string.ok)
                dialog.setOkButtonClickListener { dialog, v ->
                    dialog.dismiss()
                    binding.switchFingerPay.setCheckedNoEvent(false)
                    mPWallet?.let {
                        it.fingerState = PWallet.CLOSE
                        it.update(it.id)
                    }
                    true
                }
                dialog.show()
            }
        }
    }


    private fun updateWalletName() {
        editBinding.etInput.inputType = InputType.TYPE_CLASS_TEXT
        editBinding.tvTitle.text = getString(R.string.my_wallet_detail_name)
        mPWallet?.let {
            editBinding.etInput.setText(it.name)
            editBinding.etInput.setSelection(it.name.length)
        }

        editBinding.ivClose.setOnClickListener {
            editDialog.dismiss()
        }
        editBinding.btnRight.setOnClickListener {
            val input = editBinding.etInput.text.toString()
            if (input.isEmpty()) {
                toast(getString(R.string.my_wallet_detail_name))
                return@setOnClickListener
            }
            lifecycleScope.launch(Dispatchers.IO) {
                mPWallet?.let {
                    it.name = input
                    it.update(it.id)
                }
                withContext(Dispatchers.Main) {
                    toast(getString(R.string.my_wallet_modified_success))
                    editDialog.dismiss()
                }
            }

        }
        editDialog.show()
    }


    private fun checkPassword(type: Int) {
        editBinding.etInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        editBinding.etInput.setText("")
        editBinding.tvTitle.text = getString(R.string.my_wallet_detail_password)
        editBinding.etInput.hint = getString(R.string.my_wallet_detail_password)
        editBinding.ivClose.setOnClickListener { editDialog.dismiss() }
        editBinding.btnRight.setOnClickListener {
            val input = editBinding.etInput.text.toString()
            if (input.isEmpty()) {
                toast(getString(R.string.my_wallet_detail_password))
                return@setOnClickListener
            }
            editDialog.dismiss()
            showLoading()
            lifecycleScope.launch(Dispatchers.IO) {
                mPWallet?.let {
                    val result = GoWallet.checkPasswd(input, it.password)
                    if (result) {
                        handlePasswordAfter(type, input)
                    } else {
                        withContext(Dispatchers.Main) {
                            toast(getString(R.string.my_wallet_detail_wrong_password))
                            dismiss()
                        }
                    }
                }

            }
        }
        editDialog.show()
    }

    /**
     * type   1 代表查看私钥   2 代表删除账户   3 代表查看助记词
     */
    private suspend fun handlePasswordAfter(type: Int, password: String) {
        when (type) {
            1 -> {
                mPWallet?.let {
                    val coinList = select().where("pwallet_id = ?", "${it.id}").find<Coin>()
                    if (!ListUtils.isEmpty(coinList)) {
                        withContext(Dispatchers.Main) {
                            dismiss()
                            val walletManager = WalletManager()
                            walletManager.chooseChain(this@WalletDetailsActivity, coinList)
                            walletManager.setOnItemClickListener(object :
                                WalletManager.OnItemClickListener {
                                override fun onItemClick(position: Int) {
                                    if (position < coinList.size) {
                                        val coin = coinList[position]

                                        if (it.type == PWallet.TYPE_NOMAL) {
                                            val mnem = getMnem(password)
                                            WalletManager().exportContent(
                                                this@WalletDetailsActivity,
                                                coin.getPrivkey(coin.chain, mnem),
                                                "${coin.name}私钥"
                                            )
                                        } else if (it.type == PWallet.TYPE_PRI_KEY) {
                                            WalletManager().exportContent(
                                                this@WalletDetailsActivity,
                                                coin.getPrivkey(password),
                                                "${coin.name}私钥"
                                            )
                                        }
                                    }


                                }
                            })
                        }
                    }
                }


            }

            2 -> {
                dismiss()
                withContext(Dispatchers.Main) {
                    doingDelete()
                }

            }

            3 -> {
                val mnem = getMnem(password)
                withContext(Dispatchers.Main) {
                    dismiss()
                    WalletManager().exportMnem(this@WalletDetailsActivity, mnem, mPWallet!!)
                }

            }

            4 -> {
                dismiss()
                showFingerPay(password)


            }
        }
    }

    private fun showFingerPay(password: String) {
        val fm = FingerManager()
        val authParam = fm.getAuthParam(this,false)
        SoterWrapperApi.requestAuthorizeAndSign({ result ->
            if (result.isSuccess) {
                mPWallet?.let {
                    val fid = result.extData.fid
                    val encPassword = AESForPayUtil.encrypt(fid, password)
                    it.fingerPassword = encPassword
                    it.fingerState = PWallet.OPEN
                    it.update(it.id)
                    binding.switchFingerPay.setCheckedNoEvent(true)
                    toast(getString(R.string.verify_suc))
                }

            } else {
                showFingerError(result)
            }
        }, authParam)
    }


    private fun showFingerError(result: SoterProcessAuthenticationResult) {
        if (result.errCode == 1013) {
            toast(getString(R.string.finger_only_tip))
        } else {
            toast(result.errMsg)
        }
    }

    private fun doingDelete() {
        commonBinding.tvResult.text = getString(R.string.my_wallet_detail_safe)
        commonBinding.tvResult.textColor = Color.RED
        commonBinding.tvResultDetails.text = getString(R.string.my_wallet_detail_delete_message)
        commonBinding.btnLeft.visibility = View.VISIBLE
        commonBinding.btnLeft.setOnClickListener {
            commonDialog.dismiss()
        }
        commonBinding.btnRight.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val job1 = lifecycleScope.launch(Dispatchers.IO) {
                    mPWallet?.let {
                        LitePal.delete<PWallet>(it.id)
                    }
                }
                job1.join()

                lifecycleScope.launch(Dispatchers.IO) {
                    val wallet = LitePal.findFirst<PWallet>()
                    MyWallet.setId(wallet?.id ?: MyWallet.ID_DEFAULT)
                    withContext(Dispatchers.Main) {
                        commonDialog.dismiss()
                        finish()
                    }
                }

            }

        }
        commonDialog.show()

    }

    private fun getMnem(password: String): String {
        return GoWallet.decMenm(GoWallet.encPasswd(password)!!, mPWallet!!.mnem)
    }

    private fun outPub() {
        mPWallet?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                val coinList = select().where("pwallet_id = ?", "${it.id}").find<Coin>()
                withContext(Dispatchers.Main) {
                    if (!ListUtils.isEmpty(coinList)) {
                        val walletManager = WalletManager()
                        walletManager.chooseChain(this@WalletDetailsActivity, coinList)
                        walletManager.setOnItemClickListener(object :
                            WalletManager.OnItemClickListener {
                            override fun onItemClick(position: Int) {
                                if (position < coinList.size) {
                                    val coin = coinList[position]

                                    if (it.type == PWallet.TYPE_NOMAL) {
                                        WalletManager().exportContent(
                                            this@WalletDetailsActivity,
                                            coin.pubkey,
                                            "${coin.name}公钥"
                                        )
                                    } else if (it.type == PWallet.TYPE_PRI_KEY) {
                                        if (coin.pubkey != null) {
                                            WalletManager().exportContent(
                                                this@WalletDetailsActivity,
                                                coin.pubkey,
                                                "${coin.name}公钥"
                                            )
                                        } else {
                                            toast(getString(R.string.try_imp))
                                        }

                                    }
                                }

                            }
                        })
                    }
                }
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