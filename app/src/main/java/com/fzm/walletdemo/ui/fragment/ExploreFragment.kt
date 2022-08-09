package com.fzm.walletdemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletdemo.databinding.FragmentExploreBinding
import com.fzm.walletdemo.ui.adapter.ExploreAdapter
import com.fzm.walletdemo.ui.adapter.ExploreDiffCallBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.support.v4.toast
import org.litepal.LitePal
import org.litepal.extension.count

class ExploreFragment : Fragment() {
    private lateinit var binding: FragmentExploreBinding
    private var oldList: MutableList<ExploreBean.AppsBean>? = null
    private var newList: MutableList<ExploreBean.AppsBean>? = null
    private lateinit var adapter: ExploreAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            oldList = mutableListOf()
            val gridLayoutManager = GridLayoutManager(context, 4, GridLayoutManager.VERTICAL, false)
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val item = newList!![position]
                    //style为1，则占用4列
                    return if (item.style == 1) {
                        4
                    } else {
                        1
                    }
                }

            }
            binding.rvExplore.layoutManager = gridLayoutManager
            adapter = ExploreAdapter(it)
            binding.rvExplore.adapter = adapter
        }
        binding.swipeExplore.setOnRefreshListener {
            getExploreAll()
        }
        getExploreAll()
    }


    private fun getExploreAll() {
        lifecycleScope.launch {
            val list = BWallet.get().getExploreList()
            withContext(Dispatchers.Main) {
                binding.swipeExplore.isRefreshing = false
                newList = null
                newList = mutableListOf()
                for (l in list) {
                    //在数据上做文章，添加第一条为title
                    //title的style都设置为1
                    val titleBean = ExploreBean.AppsBean().apply {
                        id = -1
                        name = l.name
                        style = 1
                    }
                    newList?.add(titleBean)

                    for (app in l.apps) {
                        app.style = l.style
                        newList?.add(app)
                    }


                }
                val diffCallBack = ExploreDiffCallBack(oldList!!, newList!!)
                val diffResult = DiffUtil.calculateDiff(diffCallBack)
                diffResult.dispatchUpdatesTo(adapter)
                adapter.setData(newList!!)
                oldList = newList
            }
        }

        adapter.setOnItemClickListener {
            oldList!![it].let { appBean ->
                if (appBean.type == 1) {
                    val count = LitePal.count<PWallet>()
                    if (count == 0) {
                        toast("请先创建钱包")
                        return@let
                    }
                }
                ARouter.getInstance().build(RouterPath.APP_DAPP).withString("name", appBean.name)
                    .withString("url", appBean.app_url).navigation()
            }

        }
    }


}