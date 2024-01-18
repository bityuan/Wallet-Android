package com.fzm.walletmodule.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.RadioGroup.OnCheckedChangeListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_WALLET
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.db.entity.PWallet.*
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.base.Constants.Companion.getWalletBg
import com.fzm.walletmodule.base.Constants.Companion.getWalletIcon
import com.fzm.walletmodule.databinding.ActivityMyWalletsBinding
import com.fzm.walletmodule.databinding.ItemMyWalletBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.isFastClick
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.startActivity
import org.litepal.LitePal
import org.litepal.extension.findAll
import walletapi.Walletapi

class MyWalletsActivity : BaseActivity() {
    private lateinit var mAdapter: Adapter
    private var mSelectedId: Long = 0
    private val addressList: MutableList<PWallet> = ArrayList()
    private val list: MutableList<PWallet> = ArrayList()
    private val adapterList: MutableList<PWallet> = ArrayList()
    private val binding by lazy { ActivityMyWalletsBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        mCustomToobar = true
        mStatusColor = Color.WHITE
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initData()
        refresh()
        initListener()
    }

    override fun initData() {
        binding.rvList.layoutManager = LinearLayoutManager(this)

        mAdapter = Adapter(adapterList)
        binding.rvList.adapter = mAdapter
        mAdapter.setOnItemClickListener { position ->
            val wallet = adapterList[position]
            MyWallet.setId(wallet.id)
            LiveEventBus.get<Long>(LIVE_KEY_WALLET).post(wallet.id)
            finish()
        }
    }

    private fun refresh() {
        mSelectedId = MyWallet.getId()
        addressList.clear()
        list.clear()
        lifecycleScope.launch(Dispatchers.IO) {
            val walletList = LitePal.findAll<PWallet>(true)
            walletList.forEach {
                if (it.type == TYPE_ADDR_KEY) {
                    if (it.id == mSelectedId) {
                        addressList.add(0, it)
                    } else {
                        addressList.add(it)

                    }
                } else {
                    if (it.id == mSelectedId) {
                        list.add(0, it)
                    } else {
                        list.add(it)

                    }
                }
            }
            withContext(Dispatchers.Main) {
                adapterList.clear()
                adapterList.addAll(list)
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    private var menuCheck = 0
    override fun initListener() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.flCreate.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.WALLET_CREATE_WALLET).navigation()
        }
        binding.tvImport.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.WALLET_IMPORTWALLET)
                .withInt(RouterPath.PARAM_FROM, menuCheck).navigation()

        }
        binding.rgMenu.setOnCheckedChangeListener { radioGroup, checkedId ->
            if (checkedId == R.id.rb_choose1) {
                menuCheck = 0
                binding.flCreate.visibility = View.VISIBLE
                adapterList.clear()
                adapterList.addAll(list)
                mAdapter.notifyDataSetChanged()

            } else if (checkedId == R.id.rb_choose2) {
                menuCheck = 1
                binding.flCreate.visibility = View.GONE
                adapterList.clear()
                adapterList.addAll(addressList)
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        refresh()
    }


    inner class Adapter(val list: List<PWallet>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding =
                ItemMyWalletBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ViewHolder) {
                val wallet = list[position]
                holder.binding.apply {
                    tvCurrentWallet.visibility =
                        if (wallet.id == mSelectedId) View.VISIBLE else View.GONE
                    tvWalletName.text = wallet.name

                    when (wallet.type) {
                        TYPE_NOMAL -> {
                            ivWalletType.visibility = View.VISIBLE
                            tvWalletType.text = getString(R.string.wallet_mnem)
                            ivWalletType.imageResource = R.mipmap.my_wallet_coins
                            rlWallet.backgroundResource = R.mipmap.my_wallet_bg_black
                        }

                        TYPE_PRI_KEY -> {
                            ivWalletType.visibility = View.VISIBLE
                            tvWalletType.text = getString(R.string.wallet_priv)
                            val chain = wallet.coinList[0].chain
                            ivWalletType.imageResource = getWalletIcon(chain)
                            rlWallet.backgroundResource = getWalletBg(chain)
                        }
                        TYPE_ADDR_KEY -> {
                            ivWalletType.visibility = View.VISIBLE
                            tvWalletType.text = wallet.coinList[0].address
                            val chain = wallet.coinList[0].chain
                            ivWalletType.imageResource = getWalletIcon(chain)
                            rlWallet.backgroundResource = getWalletBg(chain)
                        }

                        TYPE_RECOVER -> {
                            tvWalletType.text = getString(R.string.wallet_recover)
                            ivWalletType.visibility = View.GONE
                            rlWallet.backgroundResource = R.mipmap.my_wallet_bg_recover
                        }

                        else -> {}
                    }
                    holder.itemView.setOnClickListener { clickListener(position) }

                }
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

        inner class ViewHolder(val binding: ItemMyWalletBinding) :
            RecyclerView.ViewHolder(binding.root)

        lateinit var clickListener: (Int) -> Unit

        fun setOnItemClickListener(listener: (Int) -> Unit) {
            this.clickListener = listener
        }
    }


}