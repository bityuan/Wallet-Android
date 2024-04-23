package com.fzm.walletmodule.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityCreateMnemBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.isFastClick

@Route(path = RouterPath.WALLET_CREATE_MNEM)
class CreateMnemActivity : BaseActivity() {


    @JvmField
    @Autowired(name = RouterPath.PARAM_WALLET)
    var mWallet: PWallet? = null

    private var mEnglishMnem: String? = null

    private val binding by lazy { ActivityCreateMnemBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        mStatusColor = Color.TRANSPARENT
        mCustomToobar = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        //禁止当前页面截屏
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        ARouter.getInstance().inject(this)
        setToolBar(R.id.toolbar, R.id.tv_title)
        title = ""
        initData()
        initListener()
    }

    override fun initData() {
        try {
            mEnglishMnem = GoWallet.createMnem(2)
            binding.tvMnem.text = mEnglishMnem
            mWallet?.mnemType = PWallet.TYPE_ENGLISH
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun initListener() {
        binding.btnReplaceMnem.setOnClickListener {
            try {
                var mnem: String? = ""
                mEnglishMnem = GoWallet.createMnem(2)
                mnem = mEnglishMnem
                binding.tvMnem.text = mnem
            } catch (e: Exception) {
            }
        }
        binding.btnOk.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            gotoBackUpWalletActivity()
        }
    }

    private fun gotoBackUpWalletActivity() {
        var mnem: String? = ""
        mnem = mEnglishMnem
        ARouter.getInstance().build(RouterPath.WALLET_BACKUP_WALLET)
            .withSerializable(RouterPath.PARAM_WALLET, mWallet)
            .withString(RouterPath.PARAM_VISIBLE_MNEM, mnem)
            .navigation()
    }

}