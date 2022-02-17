package com.fzm.walletmodule.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.event.AddCoinEvent
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.WalletUtils
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.android.synthetic.main.fragment_token_coin.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import org.litepal.LitePal.select
import org.litepal.LitePal.where
import walletapi.HDWallet
import java.util.HashMap


/**
 * create an instance of this fragment.
 */
class TokenCoinFragment : BaseFragment() {
    private val data = ArrayList<Coin>()
    private var mCommonAdapter: CommonAdapter<Coin>? = null
    private var mPWallet: PWallet? = null
    private val mStatusMap = HashMap<String, Int>()
    private val mCoinsMap = HashMap<String, Coin>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val paramCoins = it.getSerializable(ADDCOIN_CHAIN_ITEM) as ArrayList<Coin>
            if (!paramCoins.isNullOrEmpty()) {
                data.clear()
                data.addAll(paramCoins)
            }
        }
    }

    override fun getLayout(): Int {
        return R.layout.fragment_token_coin
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun initView() {
        swipe_target.layoutManager = LinearLayoutManager(activity)
        mCommonAdapter = object : CommonAdapter<Coin>(activity, R.layout.listitem_addcoin, data) {
            override fun convert(holder: ViewHolder, coin: Coin, position: Int) {
                val ivAddIcon = holder.getView<ImageView>(R.id.iv_addcoin_icon)
                Glide.with(mContext)
                    .load(coin.icon)
                    .into(ivAddIcon)
                holder.setText(R.id.tv_addcoin_name, coin.name)
                holder.setText(R.id.tv_addcoin_name_cn, coin.nickname)
                val status: Int? = getStatus(coin.netId)
                if (null == status) {
                    coin.status = 0
                } else {
                    coin.status = status
                    coin.id = getCoin(coin.netId)!!.id
                }
                //                Log.w("nyb", "列表-" + coin.getName() + ",id:" + coin.getNetId() + ",status" + coin.getStatus());
                holder.setImageResource(
                    R.id.iv_add_remove_coin,
                    if (coin.status == Coin.STATUS_ENABLE) R.mipmap.icon_removecoin else R.mipmap.icon_addcoin
                )
                holder.setOnClickListener(R.id.iv_add_remove_coin) {
                    if (coin.status == Coin.STATUS_ENABLE) {
                        updateCoin(coin, false, false)
                    } else {
                        //0表示本地没有，那就是保存，1或者-1表示本地有，就更新
                        val isSave = coin.status == 0
                        if (isSave) {
                            checkCoin(coin)
                        } else {
                            updateCoin(coin, false, true)
                        }
                    }
                }
            }
        }
        swipe_target.adapter = mCommonAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateCoin(coin: Coin, save: Boolean, visible: Boolean) {
        val homeData = where(
            "pwallet_id = ?",
            java.lang.String.valueOf(mPWallet?.id)
        ).find(Coin::class.java, true)
        coin.status = if (visible) Coin.STATUS_ENABLE else Coin.STATUS_DISABLE
        mStatusMap[coin.netId] = coin.status
        mCoinsMap[coin.netId] = coin
        for (homeCoin in homeData) {
            if (TextUtils.equals(coin.chain, homeCoin.chain)) {
                coin.address = homeCoin.address
                coin.pubkey = homeCoin.pubkey
                coin.setPrivkey(homeCoin.encPrivkey)
                break
            }
        }
        if (save) {
            //新增
            coin.setpWallet(mPWallet)
            coin.sort = homeData.size
            coin.save()
        } else {
            //更新
            coin.update(coin.id)
        }
        mCommonAdapter!!.notifyDataSetChanged()
        EventBus.getDefault().post(AddCoinEvent())
    }

    private fun getCoin(sid: String): Coin? = mCoinsMap[sid]
    private fun getStatus(sid: String): Int? = mStatusMap[sid]

    private fun checkCoin(coin: Coin) {
        val chainCoin = select().where(
            "chain = ? and pwallet_id = ?",
            coin.chain,
            java.lang.String.valueOf(mPWallet?.id)
        ).findFirst(
            Coin::class.java
        )
        if (chainCoin == null) {
            showPwdDialog(coin)
        } else {
            updateCoin(coin, true, true)
        }
    }

    open fun refresh() {
        refreshPWallet()
        mCommonAdapter!!.notifyDataSetChanged()
    }

    private fun refreshPWallet() {
        mPWallet = WalletUtils.getUsingWallet()
        val homeData = where(
            "pwallet_id = ?",
            java.lang.String.valueOf(mPWallet?.id)
        ).find(Coin::class.java, true)
        mStatusMap.clear()
        mCoinsMap.clear()
        for (coin in homeData) {
            mStatusMap[coin.netId] = coin.status
            mCoinsMap[coin.netId] = coin
            //            Log.d("nyb", "初始化-" + coin.getName() + ",id:" + coin.getNetId() + ",status" + coin.getStatus());
        }
    }

    private fun showPwdDialog(coin: Coin) {
        //小账户代码
        if (TextUtils.isEmpty(mPWallet!!.password)) {
            handlePasswordAfter(coin, mPWallet!!.password)
            return
        }
        val editDialogFragment = EditDialogFragment()
        editDialogFragment.setTitle(getString(R.string.my_wallet_detail_password))
        editDialogFragment.setHint(getString(R.string.my_wallet_detail_password))
        editDialogFragment.setAutoDismiss(false)
        editDialogFragment.setType(1)
            .setRightButtonStr(getString(R.string.ok))
            .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                override fun onLeftButtonClick(v: View?) {}
                override fun onRightButtonClick(v: View?) {
                    val etInput: EditText = editDialogFragment.getEtInput()
                    val value = etInput.text.toString()
                    if (TextUtils.isEmpty(value)) {
                        ToastUtils.show(activity, getString(R.string.rsp_dialog_input_password))
                        return
                    }
                    editDialogFragment.dismiss()
                    handlePasswordAfter(coin, value)
                }
            })
        editDialogFragment.showDialog("tag", childFragmentManager)
    }

    private fun handlePasswordAfter(coin: Coin, password: String) {
        showLoading()
        doAsync {
            val bPassword: ByteArray? = GoWallet.encPasswd(password)
            val mnem: String = GoWallet.decMenm(bPassword!!, mPWallet!!.mnem)
            if (!TextUtils.isEmpty(mnem)) {
                val hdWallet: HDWallet? = GoWallet.getHDWallet(coin.chain, mnem)
                val address = hdWallet!!.newAddress_v2(0)
                val pubkey = hdWallet.newKeyPub(0)
                val pubkeyStr: String = GoWallet.encodeToStrings(pubkey)
                coin.address = address
                coin.pubkey = pubkeyStr
                // GoManager.importAddress(coin.chain, address)
                uiThread {
                    dismiss()
                    updateCoin(coin, true, true)
                }
            } else {
                uiThread {
                    dismiss()
                    toast(getString(R.string.my_wallet_detail_wrong_password))
                   // ToastUtils.show(activity, getString(R.string.my_wallet_detail_wrong_password))
                }
            }
        }
    }


    companion object {
        const val ADDCOIN_CHAIN_ITEM = "addcoin_chain_item"

        @JvmStatic
        fun newInstance(list: ArrayList<Coin>) =
            TokenCoinFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ADDCOIN_CHAIN_ITEM, list)
                }
            }
    }
}