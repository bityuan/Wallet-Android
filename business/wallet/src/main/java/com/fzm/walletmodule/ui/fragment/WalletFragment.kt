package com.fzm.walletmodule.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.rootScope
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.adapter.CoinAdapter
import com.fzm.walletmodule.adapter.CoinDiffCallBack
import com.fzm.walletmodule.databinding.FragmentWalletBinding
import com.fzm.walletmodule.vm.WalletViewModel
import com.noober.background.view.Const
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.find
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class WalletFragment : Fragment() {
    private lateinit var binding: FragmentWalletBinding
    private val walletViewModel: WalletViewModel by inject(walletQualifier)

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
                ARouter.getInstance().build(RouterPath.WALLET_TRANSACTIONS)
                    .withSerializable(RouterPath.PARAM_COIN, coin).navigation()
            }

        }
        getCoins()
    }

    /*  一：钱包id====6
        ====刷新UI
        二：钱包id====6
        ====刷新UI
        一：钱包id====2
        ====刷新UI
        二：钱包id====2
        ====刷新UI
        三：钱包id====6
        ====刷新UI
        三：钱包id====2
        ====刷新UI
        ===========延迟5秒
        */
    var getCoinJob: Job? = null
    private fun getCoins() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val id = MyWallet.getId()
                if (id == MyWallet.ID_DEFAULT) {
                    return@repeatOnLifecycle
                }
                while (true) {
                    getCoinJob?.cancel()
                    getCoinJob = lifecycleScope.launch(Dispatchers.IO) {
                        walletViewModel.getCoins(id).flowOn(Dispatchers.IO).collect { list ->
                            withContext(Dispatchers.Main) {
                                nomalRefreshData(list)
                                //refreshData(list)

                                for (cc in list){
                                    logDebug("====刷新UI${cc.name} ${cc.balance}")
                                }

                            }
                        }
                    }

                    getCoinJob?.join()
                    logDebug("===========延迟5秒")
                    delay(5000)
                }

            }
        }
    }


    private fun nomalRefreshData(list: List<Coin>) {
        oldList?.let {
            it.clear()
            it.addAll(list)
        }
        adapter.setData(oldList!!)
        adapter.notifyDataSetChanged()

    }

    private fun refreshData(list: List<Coin>) {
        newList = null
        newList = mutableListOf()
        newList!!.addAll(list)
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
            return input.readObject() as MutableList<Coin>;
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}