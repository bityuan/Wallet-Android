package com.fzm.walletdemo.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.PRE_X_RECOVER
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.db.entity.PWallet.*
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentHomeBinding
import com.fzm.walletdemo.ui.activity.MainActivity
import com.fzm.walletmodule.ui.activity.AddCoinActivity
import com.fzm.walletmodule.ui.activity.MyWalletsActivity
import com.fzm.walletmodule.ui.fragment.WalletFragment
import com.fzm.walletmodule.utils.ClickUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.NoneDelayTxParam
import walletapi.WalletRecover
import walletapi.Walletapi

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var cWalletId: Long = -1

    private val loading by lazy {
        activity?.let {
            val view =
                LayoutInflater.from(it).inflate(com.fzm.walletmodule.R.layout.dialog_loading, null)
            return@lazy AlertDialog.Builder(it).setView(view).create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = Adapter(childFragmentManager)
        adapter.addFragment(WalletFragment(), "资产")
        //adapter.addFragment(NFTFragment(), "NFT")
        binding.vpHome.adapter = adapter
        binding.tabHome.setupWithViewPager(binding.vpHome)

        /*if (BWallet.get().getCurrentWallet()?.type != 2) {
            if (binding.tabHome.tabCount > 1) {
                binding.tabHome.getTabAt(1)?.view?.visibility = View.GONE
            }
        } else {
            if (binding.tabHome.tabCount > 1) {
                binding.tabHome.getTabAt(1)?.view?.visibility = View.VISIBLE
            }
        }*/



        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    cWalletId = MyWallet.getId()
                    val pWallet = LitePal.find(PWallet::class.java, cWalletId, true)
                    if (pWallet != null) {
                        refreshWallet(pWallet)
                    } else {
                        activity?.let {
                            (it as MainActivity).setTabSelection(0)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(viewLifecycleOwner, Observer { scan ->
            if (!uiVisible) {
                return@Observer
            }
            if (scan.contains(PRE_X_RECOVER)) {
                val scans = scan.split(",")
                val chooseCoin = scans[1]
                val xAddress = scans[2]
                val toAddress = scans[3]
                val amount = scans[4]
                showPwdDialog(chooseCoin,xAddress, toAddress, amount)
            }


            ARouter.getInstance().build(RouterPath.APP_SCAN_RESULT)
                .withString(RouterPath.PARAM_SCAN, scan).navigation()
        })
        binding.header.ivScan.setOnClickListener {
            ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()
        }
        binding.header.ivChangeWallet.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            val intent = Intent()
            intent.setClass(requireActivity(), MyWalletsActivity::class.java)
            startActivity(intent)
        }
        binding.header.ivMore.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.WALLET_WALLET_DETAILS)
                .withLong(PWallet.PWALLET_ID, cWalletId)
                .navigation()
        }
        binding.header.ivAddCoin.setOnClickListener {
            if (ClickUtils.isFastDoubleClick()) {
                return@setOnClickListener
            }
            startActivity<AddCoinActivity>()
        }

    }


    private fun refreshWallet(pWallet: PWallet) {
        binding.header.tvName.text = pWallet.name
        when (pWallet.type) {
            TYPE_PRI_KEY -> {
                val chain = pWallet.coinList[0].chain
                binding.header.rlWalletBg.backgroundResource = getWalletBg(chain)
                binding.header.tvWalletName.text = "私钥账户"
                binding.header.ivAddCoin.visibility = View.VISIBLE
            }
            TYPE_RECOVER -> {
                val chain = pWallet.coinList[0].chain
                binding.header.rlWalletBg.backgroundResource = getWalletBg(chain)
                binding.header.tvWalletName.text = "找回账户"
                binding.header.ivAddCoin.visibility = View.GONE
            }
            TYPE_NOMAL -> {
                binding.header.tvWalletName.text = "助记词账户"
                binding.header.rlWalletBg.backgroundResource = R.mipmap.header_wallet_hd_wallet
                binding.header.ivAddCoin.visibility = View.VISIBLE
            }
        }
    }

    internal class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        fun addFragment(fragment: Fragment, title: String) {
            mFragments.add(fragment)
            mFragmentTitles.add(title)
        }

        fun removeFragment(fragment: Fragment, title: String) {
            mFragments.remove(fragment)
            mFragmentTitles.remove(title)
        }

        override fun getItem(position: Int): Fragment {
            return mFragments[position]
        }

        override fun getCount(): Int {
            return mFragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitles[position]
        }
    }


    private fun getWalletBg(chain: String): Int {
        return when (chain) {
            Walletapi.TypeBitcoinString -> R.mipmap.header_wallet_btc_wallet
            Walletapi.TypeBtyString -> R.mipmap.header_wallet_bty_wallet
            Walletapi.TypeDcrString -> R.mipmap.header_wallet_dcr_wallet
            Walletapi.TypeETHString -> R.mipmap.header_wallet_eth_wallet
            Walletapi.TypeLitecoinString -> R.mipmap.header_wallet_ltc_wallet
            Walletapi.TypeEtherClassicString -> R.mipmap.header_wallet_etc_wallet
            Walletapi.TypeZcashString -> R.mipmap.header_wallet_zec_wallet
            Walletapi.TypeNeoString -> R.mipmap.header_wallet_neo_wallet
            Walletapi.TypeBchString -> R.mipmap.header_wallet_bch_wallet
            Walletapi.TypeTrxString, Walletapi.TypeBnbString -> R.mipmap.header_wallet_trx_wallet
            Walletapi.TypeAtomString -> R.mipmap.header_wallet_atom_wallet
            Walletapi.TypeHtString -> R.mipmap.header_wallet_ht_wallet
            else -> R.mipmap.header_wallet_bty_wallet
        }
    }


    //recover wallet
    private fun showPwdDialog(chooseCoin:String,xAddress: String, toAddress: String, amount: String) {
        activity?.let {
            val view =
                LayoutInflater.from(it).inflate(com.fzm.walletmodule.R.layout.dialog_pwd, null)
            val dialog = AlertDialog.Builder(it).setView(view).create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                show()
            }
            val bindingDialog = DialogPwdBinding.bind(view)
            bindingDialog.ivClose.setOnClickListener {
                dialog.dismiss()
            }
            bindingDialog.btnOk.setOnClickListener {
                val password = bindingDialog.etInput.text.toString()
                if (password.isEmpty()) {
                    toast("请输入密码")
                    return@setOnClickListener
                }
                CoroutineScope(Dispatchers.IO).launch {
                    val wallet = LitePal.find<PWallet>(MyWallet.getId())
                    wallet?.let {
                        withContext(Dispatchers.Main) {
                            loading?.show()
                        }
                        val check = GoWallet.checkPasswd(password, it.password)
                        if (!check) {
                            withContext(Dispatchers.Main) {
                                toast("密码不正确")
                                loading?.dismiss()
                            }
                            return@let
                        }
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()

                        }

                        val bPassword = GoWallet.encPasswd(password)!!
                        val mnem: String = GoWallet.decMenm(bPassword, it.mnem)
                        val hdWallet = GoWallet.getHDWallet(Walletapi.TypeBtyString, mnem)
                        val privkey = Walletapi.byteTohex(hdWallet?.newKeyPriv(0))
                        val address = hdWallet?.newAddress_v2(0)
                        //当前钱包为控制地址，那么prikey就是控制地址的
                        //当前钱包为备份地址，那么prikey就是备份地址的
                        //所以我们只需要获取当前钱包的私钥即可
                        loading?.dismiss()
                        doRecover(chooseCoin,xAddress, toAddress, amount, privkey)
                    }

                }
            }
        }

    }

    private fun doRecover(chooseCoin:String,xAddress: String, toAddress: String, amount: String, privkey: String) {
        val walletRecoverParam = GoWallet.queryRecover(xAddress)
        val walletRecover = WalletRecover()
        walletRecover.param = walletRecoverParam
        val createRaw = GoWallet.createTran(
            chooseCoin,
            xAddress,
            toAddress,
            amount.toDouble(),
            0.1,
            "zh测试",
            ""
        )
        val strResult = JSON.parseObject(createRaw, StringResult::class.java)
        val createRawResult: String? = strResult.result
        if (!createRawResult.isNullOrEmpty()) {
            backSend(walletRecover, createRawResult, privkey)

        }
    }

    //备份地址（找回地址）提取资产
    private fun backSend(walletRecover: WalletRecover, result: String, privkey: String) {
        val noneDelayTxParam = NoneDelayTxParam().apply {
            execer = "none"
            addressID = 0
            chainID = 0
            fee = 0.003
        }
        val noneDelaytx = walletRecover.createNoneDelayTx(noneDelayTxParam)
        val signtx2 = walletRecover.signRecoverTxWithRecoverKey(
            Walletapi.stringTobyte(result),
            privkey,
            noneDelaytx
        )
        val sendRawTransaction2 = GoWallet.sendTran("BTY", signtx2, "")
        Log.v("wlike", "备份找回 == " + sendRawTransaction2)
    }

    private var uiVisible = true

    override fun onResume() {
        super.onResume()
        uiVisible = true
    }

    override fun onPause() {
        super.onPause()
        uiVisible = false
    }
}