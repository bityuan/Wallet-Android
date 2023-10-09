package com.fzm.walletmodule.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.Address
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.Contacts
import com.fzm.wallet.sdk.widget.sidebar.CharacterParser
import com.fzm.walletmodule.BuildConfig
import com.fzm.walletmodule.R
import com.fzm.walletmodule.adapter.ContactAdapter
import com.fzm.walletmodule.databinding.ActivityContactsBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.KeyboardUtils
import com.jiang.android.lib.adapter.expand.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.view_header_wallet.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.litepal.LitePal
import org.litepal.extension.count
import org.litepal.extension.delete
import org.litepal.extension.find
import org.litepal.extension.findAll
import java.util.Collections
import java.util.Locale

@Route(path = RouterPath.WALLET_CONTACTS)
class ContactsActivity : BaseActivity() {

    private val binding by lazy { ActivityContactsBinding.inflate(layoutInflater) }
    private var mDataContacts = listOf<Contacts>()
    private val mContactsList = mutableListOf<Contacts>()
    private lateinit var mContactAdapter: ContactAdapter

    @JvmField
    @Autowired
    var coin: Coin? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        title = getString(R.string.title_contacts)
        initView()
        initListener()
        initData()
    }


    override fun initView() {
        super.initView()
        binding.sidebarCharacter.setTextView(binding.tvDialog)
        binding.rvList.layoutManager = LinearLayoutManager(this)
        mContactAdapter = ContactAdapter(this, R.layout.listitem_contacts, mContactsList)
        binding.rvList.adapter = mContactAdapter
    }

    override fun initListener() {
        super.initListener()
        val headersDecor = StickyRecyclerHeadersDecoration(mContactAdapter)
        binding.rvList.addItemDecoration(headersDecor)
        mContactAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                headersDecor.invalidateHeaders()
            }
        })
        binding.sidebarCharacter.setOnTouchingLetterChangedListener { s ->
            mContactAdapter.closeSwipeLayouts()
            val position = mContactAdapter.getPositionForSection(s[0])
            if (position != -1) {
                binding.rvList.layoutManager?.scrollToPosition(position)
            }
        }
        binding.rvList.setOnItemClickListener { holder, position ->
            val contacts = mContactsList[position]
            ARouter.getInstance().build(RouterPath.WALLET_CONTACTS_DETAILS)
                .withLong(RouterPath.PARAM_CONTACTS_ID, contacts.id)
                .withSerializable(RouterPath.PARAM_COIN, coin)
                .navigation()
        }
        mContactAdapter.setSwipeDeleteListener(object : ContactAdapter.SwipeDeleteListener {
            override fun delete(position: Int) {
                val contacts = mContactsList[position]
                LitePal.delete<Contacts>(contacts.id)
                mContactsList.removeAt(position)
                mContactAdapter.notifyItemRemoved(position)
            }

        })

        binding.incSearch.llSearch.setOnClickListener {
            showSearch()
        }
        binding.incSearch.tvCancle.setOnClickListener {
            hideSearch()
        }

        binding.incSearch.etSearch.doOnTextChanged { text, start, count, after ->
            lifecycleScope.launch(Dispatchers.IO) {
                val contactsList = LitePal.where("nickName like ? or phone like ?","%$text%","%$text%").find(Contacts::class.java)
                mDataContacts = handleSortLetters(contactsList)
                withContext(Dispatchers.Main){
                    mContactsList.clear()
                    mContactsList.addAll(mDataContacts)
                    Collections.sort(mContactsList, PinyinComparator())
                    mContactAdapter.notifyDataSetChanged()
                }
            }
        }

    }


    private fun showSearch() {
        binding.incSearch.etSearch.requestFocus()
        mContactsList.clear()
        KeyboardUtils.showKeyboard(binding.incSearch.etSearch)
        binding.incSearch.llDoSearch.visibility = View.VISIBLE
        toolbar.visibility = View.GONE
        binding.incSearch.llSearch.visibility = View.GONE
    }

    private fun hideSearch() {
        KeyboardUtils.hideKeyboard(binding.incSearch.etSearch)
        mContactsList.addAll(mDataContacts)
        toolbar.visibility = View.VISIBLE
        binding.incSearch.llSearch.visibility = View.VISIBLE
        binding.flContent.visibility = View.VISIBLE
        binding.incSearch.llDoSearch.visibility = View.GONE
        binding.rvSearchList.visibility = View.GONE
        binding.incSearch.etSearch.setText("")
        //根据拼音首字母排序
        val pinyinComparator = PinyinComparator()
        Collections.sort(mContactsList, pinyinComparator)
        mContactAdapter.notifyDataSetChanged()
    }

    override fun initData() {
        super.initData()
        getContactsList()

    }

    private fun getContactsList(){
        lifecycleScope.launch(Dispatchers.IO) {
            val contactsList = LitePal.findAll<Contacts>()
            mDataContacts = handleSortLetters(contactsList)
            withContext(Dispatchers.Main) {
                mContactsList.clear()
                mContactsList.addAll(mDataContacts)
                Collections.sort(mContactsList, PinyinComparator())
                mContactAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun handleSortLetters(contactsList: List<Contacts>?): List<Contacts> {
        if (contactsList == null) {
            return ArrayList()
        }
        val list: MutableList<Contacts> = ArrayList()
        val characterParser = CharacterParser.getInstance()
        for (i in contactsList.indices) {
            val contacts = contactsList[i]
            val pinyin = characterParser.getSelling(contacts.nickName)
            val sortString = pinyin.substring(0, 1).uppercase(Locale.getDefault())
            val regex = Regex("[A-Z]")
            if (sortString.matches(regex)) {
                contacts.sortLetters = sortString.uppercase(Locale.getDefault())
            } else {
                contacts.sortLetters = "#"
            }
            list.add(contacts)
        }
        return list
    }

   inner class PinyinComparator : Comparator<Contacts> {
        override fun compare(o1: Contacts, o2: Contacts): Int {
            return if (o1.sortLetters == "@" || o2.sortLetters == "#") {
                -1
            } else if (o1.sortLetters == "#" || o2.sortLetters == "@") {
                1
            } else {
                o1.sortLetters.compareTo(o2.sortLetters)
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val item = menu.add(0, 1, 0, getString(R.string.my_contact_add))
        item.setIcon(R.mipmap.icon_add)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            ARouter.getInstance().build(RouterPath.WALLET_UPDATE_CONTACTS)
                .withInt(RouterPath.PARAM_FROM, 0).navigation()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getContactsList()
    }

}