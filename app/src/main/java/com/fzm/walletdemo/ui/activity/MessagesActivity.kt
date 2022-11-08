package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import android.text.TextUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.bean.Notice
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityMessagesBinding
import com.fzm.walletdemo.databinding.ActivitySearchDappBinding
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.PreferencesUtils
import com.fzm.walletmodule.vm.WalletViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject

@Route(path = RouterPath.APP_MESSAGES)
class MessagesActivity : BaseActivity() {
    private var mPage = 1
    private val mNoticeList: MutableList<Notice> = ArrayList()
    private lateinit var mCommonAdapter: CommonAdapter<*>
    private val binding by lazy { ActivityMessagesBinding.inflate(layoutInflater) }
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initData()
        initView()
        initRefresh()
    }


    override fun initView() {
        super.initView()
        binding.rvList.layoutManager = LinearLayoutManager(this)
        mCommonAdapter = object : CommonAdapter<Notice>(this, R.layout.item_message, mNoticeList) {
            override fun convert(holder: ViewHolder, notice: Notice, position: Int) {
                with(notice) {
                    holder.setText(R.id.tv_title, title)
                    holder.setText(R.id.tv_time, create_time)
                }

            }
        }
        binding.rvList.adapter = mCommonAdapter
        binding.rvList.setOnItemClickListener { holder, position ->
            val notice = mNoticeList[position]
            ARouter.getInstance().build(RouterPath.APP_MsgDetails).withInt(Notice.KEY_ID, notice.id)
                .navigation()
        }
    }

    override fun initData() {
        super.initData()
        walletViewModel.getNoticeList.observe(this, androidx.lifecycle.Observer {
            if (it.isSucceed()) {
                val data = it.data()
                if (data != null) {
                    if (mPage == 1) {
                        mNoticeList.clear()
                    }
                    mPage++;
                    mNoticeList.addAll(data.list)
                    val isCanLoadMore = data.list.size < Constants.PAGE_LIMIT
                    binding.rvList.setHasLoadMore(!isCanLoadMore)
                    mCommonAdapter.notifyDataSetChanged()
                }

            } else {
                toast(it.error())
            }
            binding.swlLayout.onRefreshComplete()
            binding.rvList.onLoadMoreComplete()

        })
    }

    override fun initRefresh() {
        super.initRefresh()
        binding.swlLayout.setOnRefreshListener {
            mPage = 1;
            walletViewModel.getNoticeList(mPage, 20, 0)
        }
        binding.swlLayout.autoRefresh()
        binding.rvList.setOnLoadMoreListener {
            walletViewModel.getNoticeList(mPage, 20, 0)
        }
    }
}