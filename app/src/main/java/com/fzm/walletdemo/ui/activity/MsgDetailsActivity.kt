package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.bean.Notice
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityMessagesBinding
import com.fzm.walletdemo.databinding.ActivityMsgDetailsBinding
import com.fzm.walletdemo.databinding.ActivitySearchDappBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.PreferencesUtils
import com.fzm.walletmodule.vm.WalletViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject

@Route(path = RouterPath.APP_MsgDetails)
class MsgDetailsActivity : BaseActivity() {

    @JvmField
    @Autowired(name = "key_id")
    var id: Int = 0
    private val binding by lazy { ActivityMsgDetailsBinding.inflate(layoutInflater) }
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initData()
    }

    override fun initData() {
        walletViewModel.getNoticeDetail.observe(this, Observer {
            if (it.isSucceed()) {
                it.data()?.run {
                    binding.tvTitles.text = title
                    binding.tvTime.text = create_time
                    binding.tvContent.text = content
                }

            } else {
                toast(it.error())
            }
        })

        walletViewModel.getNoticeDetail(id)
    }


}