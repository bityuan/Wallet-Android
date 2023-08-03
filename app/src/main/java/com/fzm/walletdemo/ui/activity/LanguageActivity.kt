package com.fzm.walletdemo.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.utils.LocalManageUtil
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityDownLoadBinding
import com.fzm.walletdemo.databinding.ActivityLanguageBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder

@Route(path = RouterPath.APP_LANGUAGE)
class LanguageActivity : BaseActivity() {
    private val binding by lazy { ActivityLanguageBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        title = getString(R.string.my_lang)
        initView()

    }


    private lateinit var mCommonAdapter: CommonAdapter<*>
    //private var list = listOf("简体中文", "English", "日本語", "한국어")
    private var list = listOf("简体中文", "English")


    override fun initView() {
        super.initView()
        binding.rvList.layoutManager = LinearLayoutManager(this)
        mCommonAdapter = object : CommonAdapter<String>(this, R.layout.item_language, list) {
            override fun convert(holder: ViewHolder, s: String, position: Int) {
                holder.setText(R.id.tv_lang_name, s)
            }
        }
        binding.rvList.adapter = mCommonAdapter
        binding.rvList.setOnItemClickListener { viewHolder, i ->
            when (i) {
                0 -> LocalManageUtil.setApplicationLanguage(this, LocalManageUtil.CHINA)
                1 -> LocalManageUtil.setApplicationLanguage(this, LocalManageUtil.ENGLISH)
                2 -> LocalManageUtil.setApplicationLanguage(this, LocalManageUtil.JAPANESE)
                3 -> LocalManageUtil.setApplicationLanguage(this, LocalManageUtil.KOREAN)
            }
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }




}