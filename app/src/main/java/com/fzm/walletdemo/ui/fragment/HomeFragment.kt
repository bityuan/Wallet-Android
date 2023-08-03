package com.fzm.walletdemo.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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
import com.fzm.wallet.sdk.RouterPath.PARAM_WC_URL
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.PRE_X_RECOVER
import com.fzm.wallet.sdk.bean.StringResult
import com.fzm.wallet.sdk.databinding.DialogPwdBinding
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.db.entity.PWallet.*
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.IApplication
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentHomeBinding
import com.fzm.walletdemo.ui.activity.MainActivity
import com.fzm.walletdemo.wcv2.WConnectActivity
import com.fzm.walletmodule.ui.activity.AddCoinActivity
import com.fzm.walletmodule.ui.activity.MyWalletsActivity
import com.fzm.walletmodule.ui.fragment.WalletFragment
import com.fzm.walletmodule.utils.ClickUtils
import com.fzm.walletmodule.vm.WalletViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.support.v4.toast
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.find
import timber.log.Timber
import walletapi.NoneDelayTxParam
import walletapi.WalletRecover
import walletapi.Walletapi

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var cWalletId: Long = -1

    private val walletViewModel: WalletViewModel by inject(walletQualifier)

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
            if (scan.contains(PRE_X_RECOVER)) {
                val scans = scan.split(",")
                val chooseCoin = scans[1]
                val xAddress = scans[2]
                val toAddress = scans[3]
                val amount = scans[4]
                showPwdDialog(chooseCoin, xAddress, toAddress, amount)
            }else if(scan.startsWith("wc:")) {
                ARouter.getInstance().build(RouterPath.APP_WCONNECT)
                    .withString(RouterPath.PARAM_WC_URL, scan).navigation()
            }

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

        initMsg()
    }


    private fun refreshWallet(pWallet: PWallet) {
        binding.header.tvName.text = pWallet.name
        when (pWallet.type) {
            TYPE_PRI_KEY -> {
                val chain = pWallet.coinList[0].chain
                binding.header.rlWalletBg.backgroundResource = getWalletBg(chain)
                binding.header.tvWalletName.text = getString(R.string.priv_account_str)
                binding.header.ivAddCoin.visibility = View.VISIBLE
            }
            TYPE_RECOVER -> {
                binding.header.tvWalletName.text = getString(R.string.recover_account_str)
                binding.header.rlWalletBg.backgroundResource = R.mipmap.header_wallet_recover_wallet
                binding.header.ivAddCoin.visibility = View.GONE
            }
            TYPE_NOMAL -> {
                binding.header.tvWalletName.text = getString(R.string.home_top_name)
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
            Walletapi.TypeYccString -> R.mipmap.header_wallet_eth_wallet
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
    private fun showPwdDialog(
        chooseCoin: String,
        xAddress: String,
        toAddress: String,
        amount: String
    ) {
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
                        val hdWallet = GoWallet.getHDWallet(Walletapi.TypeETHString, mnem)
                        val pubkey = Walletapi.byteTohex(hdWallet?.newKeyPub(0))
                        val privkey = Walletapi.byteTohex(hdWallet?.newKeyPriv(0))
                        val address = hdWallet?.newAddress_v2(0)
                        //当前钱包为控制地址，那么prikey就是控制地址的
                        //当前钱包为备份地址，那么prikey就是备份地址的
                        //所以我们只需要获取当前钱包的私钥即可
                        doRecover(chooseCoin, xAddress, toAddress, amount, privkey)
                    }

                }
            }
        }

    }

    private suspend fun doRecover(
        chooseCoin: String,
        xAddress: String,
        toAddress: String,
        amount: String,
        privkey: String
    ) {
        val walletRecoverParam = GoWallet.queryRecover(xAddress, chooseCoin)
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
            backSend(walletRecover, createRawResult, privkey, chooseCoin)

        }
    }

    //备份地址（找回地址）提取资产
    private suspend fun backSend(
        walletRecover: WalletRecover,
        result: String,
        privkey: String,
        chooseCoin: String
    ) {
        val noneDelayTxParam = NoneDelayTxParam().apply {
            execer = "none"
            addressID = 2
            chainID = if (chooseCoin == "YCC") 999 else 0
            fee = 0.003
        }
        val noneDelaytx = walletRecover.createNoneDelayTx(noneDelayTxParam)
        val signtx2 = walletRecover.signRecoverTxWithRecoverKey(
            Walletapi.stringTobyte(result),
            privkey,
            noneDelaytx
        )
        val sendRawTransaction2 = GoWallet.sendTran(chooseCoin, signtx2, "")
        val sendJson = JSONObject(sendRawTransaction2)
        val result = sendJson.getString("result")
        withContext(Dispatchers.Main) {
            loading?.dismiss()
            showBackTip(result)
        }

    }

    private fun showBackTip(result: String) {
        activity?.let {
            val view =
                LayoutInflater.from(it).inflate(R.layout.dialog_common, null)
            val tvResult = view.findViewById<TextView>(com.fzm.walletmodule.R.id.tv_result)
            val tvResultDetails = view.findViewById<TextView>(R.id.tv_result_details)
            val ivClose = view.findViewById<ImageView>(R.id.iv_close)
            val line = view.findViewById<View>(R.id.v_middle_line)
            line.visibility = View.GONE
            val btnRight = view.findViewById<Button>(R.id.btn_right)
            tvResult.text = "找回结果Hash"
            tvResultDetails.text = result
            btnRight.text = "复制"
            ivClose.visibility = View.VISIBLE
            val dialog = AlertDialog.Builder(it).setView(view).create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            dialog.setCancelable(false)
            ivClose.setOnClickListener {
                dialog.dismiss()
            }
            btnRight.setOnClickListener { view ->
                val cm = it.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;
                val mClipData = ClipData.newPlainText("Label", result)
                cm.setPrimaryClip(mClipData)
                toast("复制成功")
            }
            dialog.show()
        }

    }

    //type=1 首页跑马灯数据
    //type = 2 强制弹框数据
    //type = 0 全部数据
    private fun initMsg() {
        walletViewModel.getNoticeList.observe(viewLifecycleOwner, Observer {
            if (it.isSucceed()) {
                it.data()?.let {
                    binding.marqvMsg.startWithList(it.list)
                    binding.llMsg.setOnClickListener(View.OnClickListener {
                        ARouter.getInstance().build(RouterPath.APP_MESSAGES).navigation()
                    })
                    binding.marqvMsg.setOnItemClickListener { position, textView ->
                        ARouter.getInstance().build(RouterPath.APP_MESSAGES).navigation()
                    }
                }
            }
        })

        walletViewModel.getNoticeList(1, 3, 1)
    }

}