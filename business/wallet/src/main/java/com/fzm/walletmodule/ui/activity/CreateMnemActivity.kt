package com.fzm.walletmodule.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletmodule.R
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.databinding.ActivityCreateMnemBinding
import com.fzm.walletmodule.utils.isFastClick

@Route(path = RouterPath.WALLET_CREATE_MNEM)
class CreateMnemActivity : BaseActivity() {


    @JvmField
    @Autowired(name = RouterPath.PARAM_WALLET)
    var mWallet: PWallet? = null

    private var mEnglishMnem: String? = null
    private var mChineseMnem: String? = null

    private val binding by lazy { ActivityCreateMnemBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        mStatusColor = Color.TRANSPARENT
        mCustomToobar = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        setToolBar(R.id.toolbar, R.id.tv_title)
        title = ""
        initData()
        initListener()
    }

    override fun initData() {
        try {
            mChineseMnem = GoWallet.createMnem(1)
            mEnglishMnem = GoWallet.createMnem(2)
            binding.tvMnem.text = configSpace(mChineseMnem!!)
            mWallet?.mnemType = PWallet.TYPE_CHINESE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun configSpace(mnem: String): String {
        val chineses = mnem.replace(" ".toRegex(), "")
        var chinese = ""
        for (i in chineses.indices) {
            val value = chineses[i].toString()
            val j = i + 1
            chinese += if (j % 3 == 0) {
                if (j == 9) {
                    "$value    \n"
                } else {
                    "$value    "
                }
            } else {
                value
            }
        }
        return chinese
    }

    override fun initListener() {
        binding.btnReplaceMnem.setOnClickListener {
            try {
                var mnem: String? = ""
                if (binding.viewChinese.visibility == View.VISIBLE) {
                    mChineseMnem = GoWallet.createMnem(1)
                    mnem = configSpace(mChineseMnem!!)
                } else {
                    mEnglishMnem = GoWallet.createMnem(2)
                    mnem = mEnglishMnem
                }
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
        binding.lvChinese.setOnClickListener {
            showChineseView()
        }
        binding.lvEnglish.setOnClickListener {
            showEnglishView()
        }
    }

    private fun showEnglishView() {
        binding.tvChinese.setTextColor(resources.getColor(R.color.color_8E92A3))
        binding.tvEnglish.setTextColor(resources.getColor(R.color.white))
        binding.viewChinese.visibility = View.GONE
        binding.viewEnglish.visibility = View.VISIBLE
        binding.tvMnem.text = mEnglishMnem
        mWallet?.mnemType = PWallet.TYPE_ENGLISH
    }

    private fun showChineseView() {
        binding.tvChinese.setTextColor(resources.getColor(R.color.white))
        binding.tvEnglish.setTextColor(resources.getColor(R.color.color_8E92A3))
        binding.viewChinese.visibility = View.VISIBLE
        binding.viewEnglish.visibility = View.GONE
        binding.tvMnem.text = configSpace(mChineseMnem!!)
        mWallet?.mnemType = PWallet.TYPE_CHINESE
    }

    private fun gotoBackUpWalletActivity() {
        var mnem: String? = ""
        if (binding.viewChinese.visibility == View.VISIBLE) {
            mWallet?.mnem = mChineseMnem
            mnem = mChineseMnem
        } else if (binding.viewEnglish.visibility == View.VISIBLE) {
            mWallet?.mnem = mEnglishMnem
            mnem = mEnglishMnem
        }
        ARouter.getInstance().build(RouterPath.WALLET_BACKUP_WALLET)
            .withSerializable(RouterPath.PARAM_WALLET, mWallet)
            .withString(RouterPath.PARAM_VISIBLE_MNEM, mnem)
            .navigation()
    }

}