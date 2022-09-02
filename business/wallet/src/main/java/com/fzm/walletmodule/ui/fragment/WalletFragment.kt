package com.fzm.walletmodule.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.rootScope
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletmodule.adapter.CoinAdapter
import com.fzm.walletmodule.adapter.CoinDiffCallBack
import com.fzm.walletmodule.databinding.FragmentWalletBinding
import com.fzm.walletmodule.vm.WalletViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*

class WalletFragment : Fragment() {
    private lateinit var binding: FragmentWalletBinding
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    private val walletRepository by lazy { rootScope.get<WalletRepository>(walletQualifier) }

    private var oldList: MutableList<Coin>? = null
    private var newList: MutableList<Coin>? = null
    private lateinit var adapter: CoinAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            oldList = mutableListOf()
            binding.rvList.layoutManager = LinearLayoutManager(it)
            adapter = CoinAdapter(it)
            binding.rvList.adapter = adapter
            initListener()
        }

    }

    private fun initListener() {
        adapter.setOnItemClickListener { position ->
            oldList?.let {
                val coin = it[position]
                val coinToken = GoWallet.newCoinType(coin.chain, coin.name, coin.netId.toInt())
                coin.chain = coinToken.cointype
                ARouter.getInstance().build(RouterPath.WALLET_TRANSACTIONS)
                    .withSerializable(RouterPath.PARAM_COIN, coin).navigation()
            }

        }

        binding.swipeList.setOnRefreshListener {
            getCoins()
        }
    }

    private fun getCoins() {
        lifecycleScope.launch(Dispatchers.IO) {
            //使用flowOn处理背压
            walletViewModel.getCoins().flowOn(Dispatchers.IO).collect { list ->
                logDebug("collect：" + list[0].name + list[0].nickname + list[0].balance)
                withContext(Dispatchers.Main) {
                    binding.swipeList.isRefreshing = false
                    // adapter.setData(list)
                    //adapter.notifyDataSetChanged()
                    refreshData(list)
                }

            }
        }
    }

    private fun refreshData(list: List<Coin>) {
        newList = null
        newList = mutableListOf()
        newList!!.addAll(list)

        oldList?.let {
            logDebug("oldList长度：" + it.size)
            if (it.size > 0) {
                logDebug("oldList：" + it[0].name + it[0].nickname + it[0].balance)
            }

        }
        newList?.let {
            logDebug("newList：" + it[0].name + it[0].nickname + it[0].balance)
        }
        val diffCallBack = CoinDiffCallBack(oldList!!, newList!!)
        val diffResult = DiffUtil.calculateDiff(diffCallBack)
        diffResult.dispatchUpdatesTo(adapter)
        adapter.setData(newList!!)
        oldList = deepCopy(newList!!)

    }

    //深度拷贝
    private fun deepCopy(src: MutableList<Coin>): MutableList<Coin>? {
        try {
            val byteOut = ByteArrayOutputStream();
            val out = ObjectOutputStream(byteOut);
            out.writeObject(src);
            val byteIn = ByteArrayInputStream(byteOut.toByteArray());
            val input = ObjectInputStream(byteIn);
            val dest = input.readObject() as MutableList<Coin>
            return dest;
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}