package com.fzm.walletmodule.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_WALLET
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.db.entity.PWallet.*
import com.fzm.walletmodule.R
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
    private val list: MutableList<PWallet> = ArrayList()
    private val binding by lazy { ActivityMyWalletsBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        mStatusColor = Color.WHITE
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initData()
        refresh()
        initListener()
    }

    override fun initData() {
        binding.rvList.layoutManager = LinearLayoutManager(this)

        mAdapter = Adapter(list)
        binding.rvList.adapter = mAdapter
        mAdapter.setOnItemClickListener { position ->
            val wallet = list[position]
            MyWallet.setId(wallet.id)
            LiveEventBus.get<Long>(LIVE_KEY_WALLET).post(wallet.id)
            finish()
        }
    }

    private fun refresh() {
        mSelectedId = MyWallet.getId()
        list.clear()
        lifecycleScope.launch(Dispatchers.IO) {
            val walletList = LitePal.findAll<PWallet>(true)
            walletList.forEach {
                if (it.id == mSelectedId) {
                    list.add(0, it)
                } else {
                    list.add(it)

                }
            }
            withContext(Dispatchers.Main) {
                mAdapter.notifyDataSetChanged()
            }
        }


    }

    override fun initListener() {
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
            startActivity<ImportWalletActivity>()
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
                            tvWalletType.text = "助记词账户"
                            ivWalletType.imageResource = R.mipmap.my_wallet_coins
                            rlWallet.backgroundResource = R.mipmap.my_wallet_bg_black
                        }
                        TYPE_PRI_KEY -> {
                            tvWalletType.text = "私钥账户"
                            val chain = wallet.coinList[0].chain
                            ivWalletType.imageResource = getWalletIcon(chain)
                            rlWallet.backgroundResource = getWalletBg(chain)
                        }
                        TYPE_RECOVER -> {
                            tvWalletType.text = "找回账户"
                            val chain = wallet.coinList[0].chain
                            ivWalletType.imageResource = getWalletIcon(chain)
                            rlWallet.backgroundResource = getWalletBg(chain)
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

    private fun getWalletIcon(chain: String): Int {
        return when (chain) {
            Walletapi.TypeBitcoinString -> R.mipmap.my_wallet_btc
            Walletapi.TypeBtyString -> R.mipmap.my_wallet_bty
            Walletapi.TypeDcrString -> R.mipmap.my_wallet_dcr
            Walletapi.TypeETHString -> R.mipmap.my_wallet_eth
            Walletapi.TypeLitecoinString -> R.mipmap.my_wallet_ltc
            Walletapi.TypeEtherClassicString -> R.mipmap.my_wallet_etc
            Walletapi.TypeZcashString -> R.mipmap.my_wallet_zec
            Walletapi.TypeNeoString -> R.mipmap.my_wallet_neo
            Walletapi.TypeBchString -> R.mipmap.my_wallet_bch
            Walletapi.TypeTrxString -> R.mipmap.my_wallet_trx
            Walletapi.TypeAtomString -> R.mipmap.my_wallet_atom
            Walletapi.TypeBnbString -> R.mipmap.my_wallet_bnb
            Walletapi.TypeHtString -> R.mipmap.my_wallet_ht
            else -> R.mipmap.my_wallet_bty
        }
    }

    private fun getWalletBg(chain: String): Int {
        return when (chain) {
            Walletapi.TypeBitcoinString -> R.mipmap.my_wallet_bg_btc
            Walletapi.TypeBtyString -> R.mipmap.my_wallet_bg_bty
            Walletapi.TypeDcrString -> R.mipmap.my_wallet_bg_dcr
            Walletapi.TypeETHString -> R.mipmap.my_wallet_bg_eth
            Walletapi.TypeLitecoinString -> R.mipmap.my_wallet_bg_ltc
            Walletapi.TypeEtherClassicString -> R.mipmap.my_wallet_bg_etc
            Walletapi.TypeZcashString -> R.mipmap.my_wallet_bg_zec
            Walletapi.TypeNeoString -> R.mipmap.my_wallet_bg_neo
            Walletapi.TypeBchString -> R.mipmap.my_wallet_bg_bch
            Walletapi.TypeTrxString, Walletapi.TypeBnbString -> R.mipmap.my_wallet_bg_trx
            Walletapi.TypeAtomString -> R.mipmap.my_wallet_bg_atom
            Walletapi.TypeHtString -> R.mipmap.my_wallet_bg_etc
            else -> R.mipmap.my_wallet_bg_bty
        }
    }
}