package com.fzm.walletmodule.ui.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_CHOOSE_CHAIN
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.walletmodule.R
import com.fzm.walletmodule.databinding.ActivityChooseChainBinding
import com.fzm.walletmodule.databinding.ItemChooseChainBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.vm.WalletViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject

@Route(path = RouterPath.WALLET_CHOOSE_CHAIN)
class ChooseChainActivity : BaseActivity() {


    private lateinit var mAdapter: Adapter
    private var list: MutableList<Coin> = ArrayList()
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    private val binding by lazy { ActivityChooseChainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        title = getString(R.string.my_wallet_detail_choose_chain)
        initIntent()
        initView()
        initObserver()
        initListener()
        initData()
    }


    override fun initView() {
        super.initView()
        binding.rvList.layoutManager = LinearLayoutManager(this)
        mAdapter = Adapter(this, list)
        binding.rvList.adapter = mAdapter
    }

    override fun initObserver() {
        super.initObserver()
        walletViewModel.getSupportedChain.observe(this, Observer {
            dismiss()
            if (it.isSucceed()) {
                val data = it.data()
                if (data != null) {
                    list.clear()
                    list.addAll(data)
                    mAdapter.notifyDataSetChanged()
                }
            } else {
                toast(it.error())
            }
        })
    }

    override fun initListener() {
        super.initListener()
        mAdapter.setOnItemClickListener { position ->
            LiveEventBus.get<Coin>(LIVE_KEY_CHOOSE_CHAIN).post(list[position])
            finish()
        }
    }

    override fun initData() {
        super.initData()
        showLoading()
        walletViewModel.getSupportedChain()
    }

    inner class Adapter(
        private val context: Context,
        private val list: List<Coin>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding = ItemChooseChainBinding.inflate(LayoutInflater.from(parent.context))
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ViewHolder) {
                val item = list[position]
                holder.binding.tvChain.text = item.name
                Glide.with(context).load(item.icon).into(holder.binding.ivIcon)
                holder.itemView.setOnClickListener { clickListener(position) }
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }

        lateinit var clickListener: (Int) -> Unit

        fun setOnItemClickListener(listener: (Int) -> Unit) {
            this.clickListener = listener
        }


        inner class ViewHolder(val binding: ItemChooseChainBinding) :
            RecyclerView.ViewHolder(binding.root)

    }
}
