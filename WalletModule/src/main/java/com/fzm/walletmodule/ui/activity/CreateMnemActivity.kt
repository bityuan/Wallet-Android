package com.fzm.walletmodule.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import com.fzm.walletmodule.R
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.utils.isFastClick
import kotlinx.android.synthetic.main.activity_create_mnem.*

/**
 * 创建助记词页面
 */
class CreateMnemActivity : BaseActivity() {
    private val TAG: String = "CreateMnemActivityNew"
    private lateinit var mWallet: PWallet
    private var mEnglishMnem: String? = null
    private var mChineseMnem: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        mStatusColor = Color.TRANSPARENT
        mCustomToobar = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_mnem)
        setToolBar(R.id.toolbar, R.id.tv_title)
        title =""
        initIntent()
        initData()
        initListener()
    }

    override fun initIntent() {
        mWallet = intent.getSerializableExtra(PWallet::class.java.simpleName) as PWallet
    }

    override fun initData() {
        try {
            mChineseMnem = GoWallet.createMnem(1)
            mEnglishMnem = GoWallet.createMnem(2)
            tv_mnem.text = configSpace(mChineseMnem!!)
            mWallet.mnemType = PWallet.TYPE_CHINESE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun configSpace(mnem: String): String? {
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
        btn_replace_mnem.setOnClickListener {
            try {
                var mnem: String? = ""
                if (view_chinese.visibility == View.VISIBLE) {
                    mChineseMnem =  GoWallet.createMnem(1)
                    mnem = configSpace(mChineseMnem!!)
                } else {
                    mEnglishMnem = GoWallet.createMnem(2)
                    mnem = mEnglishMnem
                }
                tv_mnem.text = mnem
            } catch (e: Exception) {
                Log.e(TAG, "btn_replace_mnem 错误 = ${e.message}")
            }
        }
        btn_ok.setOnClickListener {
            if (isFastClick()){
                return@setOnClickListener
            }
            gotoBackUpWalletActivity()
        }
        lv_chinese.setOnClickListener {
            showChineseView()
        }
        lv_english.setOnClickListener {
            showEnglishView()
        }
    }

    private fun showEnglishView() {
        tv_chinese.setTextColor(resources.getColor(R.color.color_8E92A3))
        tv_english.setTextColor(resources.getColor(R.color.white))
        view_chinese.visibility = View.GONE
        view_english.visibility = View.VISIBLE
        tv_mnem.text = mEnglishMnem
        mWallet.mnemType = PWallet.TYPE_ENGLISH
    }

    private fun showChineseView() {
        tv_chinese.setTextColor(resources.getColor(R.color.white))
        tv_english.setTextColor(resources.getColor(R.color.color_8E92A3))
        view_chinese.visibility = View.VISIBLE
        view_english.visibility = View.GONE
        tv_mnem.text = configSpace(mChineseMnem!!)
        mWallet.mnemType = PWallet.TYPE_CHINESE
    }

    private fun gotoBackUpWalletActivity() {
        var mnem: String? = ""
        if (view_chinese.visibility == View.VISIBLE) {
            mWallet.mnem = mChineseMnem
            mnem = mChineseMnem
        } else if (view_english.visibility == View.VISIBLE) {
            mWallet.mnem = mEnglishMnem
            mnem = mEnglishMnem
        }
        BackUpWalletActivity.launch(
            this,
            mWallet,
            mnem!!,
            CreateMnemActivity::class.java.simpleName
        )
    }

}