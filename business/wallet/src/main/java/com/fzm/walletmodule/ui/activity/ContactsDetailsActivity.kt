package com.fzm.walletmodule.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.db.entity.Address
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.Contacts
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletmodule.R
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.databinding.ActivityContactsDetailsBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.jeremyliao.liveeventbus.LiveEventBus
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find
import org.litepal.extension.findFirst

@Route(path = RouterPath.WALLET_CONTACTS_DETAILS)
class ContactsDetailsActivity : BaseActivity() {

    private val binding by lazy { ActivityContactsDetailsBinding.inflate(layoutInflater) }
    private lateinit var mCommonAdapter: CommonAdapter<*>
    private val mAddressList = mutableListOf<Address>()
    private var mCheckedPosition = -1

    @JvmField
    @Autowired(name = RouterPath.PARAM_CONTACTS_ID)
    var contactsId: Long = 0

    @JvmField
    @Autowired
    var coin: Coin? = null

    private lateinit var mContacts: Contacts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        title = getString(R.string.title_contacts_details)
        ARouter.getInstance().inject(this)
        initView()
        initListener()
        initData()
    }

    override fun initView() {
        super.initView()
        binding.rvList.layoutManager = LinearLayoutManager(this)
        mCommonAdapter =
            object : CommonAdapter<Address>(this, R.layout.listitem_check_address, mAddressList) {
                override fun convert(holder: ViewHolder, address: Address, position: Int) {
                    holder.setText(R.id.tv_name, "${address.name}(${address.nickName})")
                    holder.setText(R.id.tv_address, address.address)
                    if (address.isChecked == 1) {
                        holder.setBackgroundRes(R.id.root, R.drawable.shape_address_check)
                        mCheckedPosition = position
                    } else {
                        holder.setBackgroundRes(R.id.root, R.drawable.bg_white_rectangle)
                    }
                    holder.getView<View>(R.id.iv_copy).setOnClickListener {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val mClipData = ClipData.newPlainText("Label", address.address)
                        cm.setPrimaryClip(mClipData)
                        toast(getString(R.string.copy_success))
                    }
                }
            }
        binding.rvList.adapter = mCommonAdapter
        binding.rvList.setOnItemClickListener { holder, position ->
            for (address in mAddressList) {
                address.isChecked = 0
            }
            val address = mAddressList[position]
            address.isChecked = 1
            mCommonAdapter.notifyDataSetChanged()
        }
    }

    override fun initListener() {
        super.initListener()
        binding.btnOk.setOnClickListener {
            val addr = mAddressList[mCheckedPosition]
            if (coin == null) {
                gotoOut(addr)
            } else {
                coin?.let { co ->
                    if(co.oldName == "BTY" || co.oldName == "YCC") {
                        if (co.oldName != addr.name) {
                            toast("${getString(R.string.p_choose_str)}${co.oldName}(${co.nickname})${getString(R.string.d_addr_str)}")
                            return@setOnClickListener
                        }

                    }else {
                        if ("${co.oldName}${co.platform}" != "${addr.name}${addr.platform}") {
                            toast("${getString(R.string.p_choose_str)}${co.oldName}(${co.nickname})${getString(R.string.d_addr_str)}")
                            return@setOnClickListener
                        }
                    }




                    ARouter.getInstance().build(RouterPath.WALLET_OUT)
                        .withString(RouterPath.PARAM_ADDRESS, addr.address).navigation()
                }

            }

        }
    }

    private fun gotoOut(addr: Address) {
        val pWallet = LitePal.find<PWallet>(MyWallet.getId())
        val coin =
            LitePal.where("name = ? and platform = ? and pwallet_id = ?", addr.name, addr.platform,pWallet?.id.toString()).findFirst<Coin>()
        if(coin == null){
            toast("${getString(R.string.con_add_str)}${addr.name}(${addr.nickName})")
            return
        }
        coin.setpWallet(pWallet)
        ARouter.getInstance().build(RouterPath.WALLET_OUT)
            .withSerializable(RouterPath.PARAM_COIN, coin)
            .withString("address", addr.address).navigation()
    }

    override fun initData() {
        super.initData()
        mContacts = LitePal.find<Contacts>(contactsId, true)
        binding.tvNickName.text = mContacts.nickName
        binding.tvPhone.text = mContacts.phone
        mAddressList.clear()
        mAddressList.addAll(mContacts.addressList)
        mAddressList[0].isChecked = 1
        mCommonAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val item = menu.add(0, 1, 0, getString(R.string.my_contact_detail_edit))
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            ARouter.getInstance().build(RouterPath.WALLET_UPDATE_CONTACTS)
                .withInt(RouterPath.PARAM_FROM, 1)
                .withLong(RouterPath.PARAM_CONTACTS_ID, contactsId).navigation()
        }
        return super.onOptionsItemSelected(item)
    }

}