package com.fzm.walletdemo.ui.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.nft.databinding.FragmentNftBinding
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.bean.Brc20Balance
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.ui.adapter.Brc20BalanceAdapter
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.utils.isFastClick
import com.fzm.walletmodule.vm.WalletViewModel
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.HDWallet

class Brc20Fragment : BaseFragment() {

    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    private lateinit var binding: FragmentNftBinding
    private lateinit var brc20BalanceAdapter: Brc20BalanceAdapter
    private val list = mutableListOf<Brc20Balance>()
    private var brc20Addr: String? = ""
    private var brc20Pub: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            // Fragment 对用户可见
            initNewAddr()
            initObserver()
            initAddress()
            initView()
            initData()
        } else {
            // Fragment 对用户不可见
        }
    }

    private fun initNewAddr() {

        val brc20s = LitePal.where("name = ? and pwallet_id = ?","BTCBRC","${MyWallet.getId()}").find<Coin>()
        if(brc20s.isEmpty()) {
            val editDialogFragment =
                EditDialogFragment()
            editDialogFragment.setTitle(getString(R.string.my_wallet_detail_password))
            editDialogFragment.setHint(getString(R.string.my_wallet_detail_password))
            editDialogFragment.setAutoDismiss(false)
            editDialogFragment.setType(1)
                .setRightButtonStr(getString(R.string.ok))
                .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View?) {}
                    override fun onRightButtonClick(v: View?) {
                        val etInput: EditText = editDialogFragment.etInput
                        val value = etInput.text.toString()
                        if (TextUtils.isEmpty(value)) {
                            toast(getString(R.string.rsp_dialog_input_password))
                            return
                        }
                        editDialogFragment.dismiss()
                        handlePasswordAfter(value)
                    }
                })
            editDialogFragment.showDialog("tag", childFragmentManager)
        }else {
            brc20Addr = brc20s[0].address
            getBrc20Balance(brc20Addr!!)
        }

    }

    private var pWallet:PWallet? = null
    fun handlePasswordAfter(password: String) {
        showLoading()
        doAsync {
            try {
                pWallet = LitePal.find<PWallet>(MyWallet.getId())
                val bPassword: ByteArray? = GoWallet.encPasswd(password)
                val mnem: String = GoWallet.decMenm(bPassword!!, pWallet!!.mnem)
                if (!TextUtils.isEmpty(mnem)) {
                    val hdWallet: HDWallet? = GoWallet.getHDWallet("BTC", mnem)
                    val pubkey = hdWallet!!.newKeyPub(100)
                    brc20Pub = GoWallet.encodeToStrings(pubkey)
                    uiThread {
                        dismiss()
                        brc20Pub?.let {
                            walletViewModel.getGenBtcWitNessAddr(it)
                        }

                    }
                } else {
                    uiThread {
                        dismiss()
                        toast(getString(R.string.my_wallet_detail_wrong_password))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun initView() {
        binding.swipeList.onRefresh {
            brc20Addr?.let { getBrc20Balance(it) }
        }
        context?.let {
            brc20BalanceAdapter = Brc20BalanceAdapter(it, list)
        }
        binding.rvList.layoutManager = LinearLayoutManager(context)
        binding.rvList.adapter = brc20BalanceAdapter
        brc20BalanceAdapter.setOnItemClickListener {
            if (isFastClick()) {
                return@setOnItemClickListener
            }
            val coin = list[it]
            ARouter.getInstance().build(RouterPath.APP_BRC20TRANS)
                .withString("name",coin.ticker)
                .withString("address",coin.address)
                .withString("availableBalance",coin.availableBalance)
                .withString("transferableBalance",coin.transferableBalance)
                .navigation()
        }
    }


    override fun initObserver() {
        walletViewModel.genBtcWitNessAddr.observe(viewLifecycleOwner, Observer { addr ->
            brc20Addr = addr
            val coin = Coin()
            coin.name = "BTCBRC"
            coin.address = brc20Addr
            coin.pubkey = brc20Pub
            coin.setpWallet(pWallet)
            coin.save()
            brc20Addr?.let {
                getBrc20Balance(it)
            }

        })

        walletViewModel.getBrc20Balance.observe(viewLifecycleOwner, Observer {
            binding.swipeList.isRefreshing = false
            if (it.isSucceed()) {
                val details = it.data()?.detail
                list.clear()
                if (!details.isNullOrEmpty()) {
                    for (de in details){
                        de.address = brc20Addr
                    }
                    list.addAll(details)
                }else {
                    val de = Brc20Balance("ordi","0","0",brc20Addr)
                    list.clear()
                    list.add(de)
                }

                brc20BalanceAdapter.notifyDataSetChanged()
            }else {
                toast("$it")
            }
        })
    }

    override fun initData() {

    }

    private fun getBrc20Balance(addr: String) {
        walletViewModel.getBrc20Balance(addr)
    }

    private fun initAddress() {

    }
}