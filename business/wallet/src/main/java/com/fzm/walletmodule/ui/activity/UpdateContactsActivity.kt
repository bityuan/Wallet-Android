package com.fzm.walletmodule.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.finalteam.loadingviewfinal.HeaderAndFooterRecyclerViewAdapter
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.Address
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.Contacts
import com.fzm.wallet.sdk.utils.RegularUtils
import com.fzm.wallet.sdk.widget.sidebar.CharacterParser
import com.fzm.walletmodule.BuildConfig
import com.fzm.walletmodule.R
import com.fzm.walletmodule.adapter.ContactAdapter
import com.fzm.walletmodule.databinding.ActivityContactsBinding
import com.fzm.walletmodule.databinding.ActivityUpdateContactsBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.KeyboardUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jiang.android.lib.adapter.expand.StickyRecyclerHeadersDecoration
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.litepal.LitePal
import org.litepal.extension.count
import org.litepal.extension.find
import org.litepal.extension.findAll
import java.util.Collections
import java.util.Locale

@Route(path = RouterPath.WALLET_UPDATE_CONTACTS)
class UpdateContactsActivity : BaseActivity() {

    private val binding by lazy { ActivityUpdateContactsBinding.inflate(layoutInflater) }

    @JvmField
    @Autowired(name = RouterPath.PARAM_FROM)
    var from: Int = 0

    @JvmField
    @Autowired(name = RouterPath.PARAM_CONTACTS_ID)
    var contactsId: Long = 0

    @JvmField
    @Autowired(name = RouterPath.PARAM_COIN)
    var coin: Coin? = null

    @JvmField
    @Autowired(name = RouterPath.PARAM_ADDRESS)
    var addr: String? = null

    private lateinit var mCommonAdapter: CommonAdapter<*>
    private lateinit var mDrawAdapter: CommonAdapter<*>
    private val mCoinList = mutableListOf<Coin>()
    private val mAddressList = mutableListOf<Address>()
    private val delAddrList = mutableListOf<Address>()
    private var mCurrentPositioin = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initObserver()
        initView()
        initListener()
        initData()
    }

    override fun initObserver() {
        super.initObserver()
        //扫一扫
        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            mAddressList[mCurrentPositioin].address = scan
            mCommonAdapter.notifyItemChanged(mCurrentPositioin)
        })
    }

    override fun initView() {
        super.initView()
        binding.rvAddressList.layoutManager = LinearLayoutManager(this)
        val footerView = LayoutInflater.from(this).inflate(R.layout.footer_edit_address_add, null)
        val llAddressAdd = footerView.findViewById<LinearLayout>(R.id.ll_address_add)
        llAddressAdd.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
        binding.rvAddressList.addFooterView(footerView)

        mCommonAdapter =
            object : CommonAdapter<Address>(this, R.layout.listitem_edit_address, mAddressList) {
                override fun convert(holder: ViewHolder, address: Address, position: Int) {
                    holder.setText(R.id.tv_coin_type, "${address.name}(${address.nickName})")
                    holder.setText(R.id.et_address, address.address)

                    holder.setOnClickListener(R.id.iv_scan) {
                        //删除后，再扫码，如果用position可能会把结果放在其他item
                        mCurrentPositioin = holder.adapterPosition
                        ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()

                    }
                    holder.setOnClickListener(R.id.iv_delete) {
                        val addr = mAddressList[holder.adapterPosition]
                        mAddressList.removeAt(holder.adapterPosition)
                        mCommonAdapter.notifyItemRemoved(holder.adapterPosition)
                        if (!address.isAdd) {
                            delAddrList.add(addr)
                        }
                        toast(getString(R.string.my_delete_success))
                    }
                    val etAddress = holder.getView<EditText>(R.id.et_address)
                    etAddress.doOnTextChanged { text, start, before, count ->
                        val address = mAddressList[holder.adapterPosition]
                        address.address = text.toString()
                    }

                }
            }
        binding.rvAddressList.adapter = mCommonAdapter
        initDraw()

        when (from) {
            0 -> {
                title = getString(R.string.my_contact_add)
            }

            1 -> {
                title = getString(R.string.title_edit_contacts)
                lifecycleScope.launch(Dispatchers.IO) {
                    val contacts = LitePal.find<Contacts>(contactsId, true)
                    withContext(Dispatchers.Main) {
                        binding.etNickName.setText(contacts.nickName)
                        binding.etPhone.setText(contacts.phone)
                        mAddressList.clear()
                        mAddressList.addAll(contacts.addressList)
                        mCommonAdapter.notifyDataSetChanged()
                    }
                }
            }

            2 -> {
                title = getString(R.string.my_contact_add)
                coin?.let { co ->
                    val address = Address()
                    address.name = co.name
                    address.platform = co.platform
                    address.nickName = co.nickname
                    address.address = addr.toString()
                    address.isAdd = true
                    mAddressList.add(address)
                    mCommonAdapter.notifyItemInserted(mAddressList.size - 1)
                }


            }
        }
    }


    override fun initData() {
        super.initData()
        lifecycleScope.launch(Dispatchers.IO) {
            val id = MyWallet.getId()
            val coinsLocal =
                LitePal.where("pwallet_id = ?", id.toString()).find<Coin>()
            withContext(Dispatchers.Main) {
                mCoinList.clear()
                mCoinList.addAll(coinsLocal)
                mDrawAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun initDraw() {
        binding.incDraw.rvList.layoutManager = LinearLayoutManager(this)
        mDrawAdapter =
            object : CommonAdapter<Coin>(this, R.layout.listitem_coin_contacts, mCoinList) {
                override fun convert(holder: ViewHolder, coin: Coin, position: Int) {
                    holder.setText(R.id.tv_name, coin.name)
                    holder.setText(R.id.tv_nick_name, "（" + coin.nickname + "）")
                    val ivCoin = holder.getView<ImageView>(R.id.iv_coin)
                    Glide.with(mContext)
                        .load(coin.icon)
                        .into(ivCoin)
                }
            }
        binding.incDraw.rvList.adapter = mDrawAdapter
        binding.incDraw.rvList.setOnItemClickListener { _, position ->
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            val coin: Coin = mCoinList[position]
            val address = buildAddress(coin.name, coin.platform, coin.nickname)
            mAddressList.add(address)
            mCommonAdapter.notifyItemInserted(mAddressList.size - 1)
        }

        binding.incDraw.etSearch.doOnTextChanged { text, start, before, count ->
            lifecycleScope.launch(Dispatchers.IO) {
                val id = MyWallet.getId()
                val coinList =
                    LitePal.where("pwallet_id = ? and name like ?", id.toString(), "%$text%")
                        .find(Coin::class.java)
                withContext(Dispatchers.Main) {
                    mCoinList.clear()
                    mCoinList.addAll(coinList)
                    mDrawAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun buildAddress(name: String, platform: String, nickname: String): Address {
        val address = Address()
        address.name = name
        address.platform = platform
        address.nickName = nickname
        address.isAdd = true
        return address
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val item = menu.add(0, 1, 0, getString(R.string.contacts_save))
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            val nickName = binding.etNickName.text.toString()
            val phone = binding.etPhone.text.toString()
            if (checked(nickName, phone)) {
                val contacts = Contacts()
                contacts.nickName = nickName
                contacts.phone = phone
                contacts.addressList = mAddressList
                if (from == 0 || from == 2) {
                    LitePal.saveAll(mAddressList)
                    contacts.save()

                } else if (from == 1) {
                    for (addr in mAddressList) {
                        addr.saveOrUpdate(
                            "id = ? and contacts_id = ?",
                            "${addr.id}",
                            "$contactsId"
                        )
                    }
                    for (del in delAddrList) {
                        del.delete()
                    }
                    contacts.saveOrUpdate("id = ?", "$contactsId")
                }
                finish()
                ARouter.getInstance().build(RouterPath.WALLET_CONTACTS).navigation()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun checked(nickName: String, phone: String): Boolean {
        var checked = true
        if (TextUtils.isEmpty(nickName)) {
            Toast.makeText(this, getString(R.string.my_add_name), Toast.LENGTH_SHORT).show()
            checked = false
        } else if (nickName.length >= 16) {
            Toast.makeText(this, getString(R.string.my_add_less_16), Toast.LENGTH_SHORT).show()
            checked = false
        } else if (!TextUtils.isEmpty(phone) && !RegularUtils.isMobileSimple(phone)) {
            Toast.makeText(this, getString(R.string.tip_phone_str), Toast.LENGTH_SHORT).show()
            checked = false
        } else if (mAddressList.isEmpty()) {
            Toast.makeText(this, getString(R.string.my_add_add_address), Toast.LENGTH_SHORT).show()
            checked = false
        } else {
            for (address in mAddressList) {
                val addr = address.address
                if (TextUtils.isEmpty(addr)) {
                    Toast.makeText(
                        this,
                        getString(R.string.my_add_add) + "${address.name}(${address.nickName})" + getString(
                            R.string.my_add_address
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    checked = false
                    break
                } else if (addr.length < 20 || !RegularUtils.isAddress(addr)) {
                    Toast.makeText(
                        this,
                        "${address.name}(${address.nickName})${getString(R.string.my_add_address_illegal)}",
                        Toast.LENGTH_SHORT
                    ).show()
                    checked = false
                    break
                }
            }
        }
        return checked
    }

}