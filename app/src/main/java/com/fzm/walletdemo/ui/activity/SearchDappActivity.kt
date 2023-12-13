package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import android.text.TextUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivitySearchDappBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.PreferencesUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kongzue.dialogx.dialogs.PopMenu
import com.kongzue.dialogx.interfaces.OnIconChangeCallBack
import com.kongzue.dialogx.interfaces.OnMenuItemClickListener
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.count
import org.litepal.extension.find

@Route(path = RouterPath.APP_SEARCH_DAPP)
class SearchDappActivity : BaseActivity() {
    private val mUrlList: MutableList<String> = ArrayList()
    private lateinit var mCommonAdapter: CommonAdapter<*>
    private val binding by lazy { ActivitySearchDappBinding.inflate(layoutInflater) }
    private val HISTORY_URL_KEY = "history_url_key"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        title = getString(R.string.my_search)
        initView()
        initData()
        binding.tvSearch.setOnClickListener {
            val url = binding.etSearch.text.toString()
            if (url.isEmpty()) {
                toast(getString(R.string.input_www))
                return@setOnClickListener
            }
            if (!mUrlList.contains(url)) {
                mUrlList.add(url)
                val urls = Gson().toJson(mUrlList)
                PreferencesUtils.putString(this, HISTORY_URL_KEY, urls)
                mCommonAdapter.notifyDataSetChanged()
            }

            try {
                if (MMkvUtil.decodeBoolean(url)) {
                    gotoDapp(url)
                } else {
                    MaterialDialog.Builder(this@SearchDappActivity)
                        .negativeText(getString(R.string.cancel))
                        .positiveText(getString(R.string.ok))
                        .title(getString(R.string.explore_title))
                        .content(getString(R.string.explore_disclaimer))
                        .checkBoxPrompt(
                            getString(R.string.no_dotip),
                            false
                        ) { buttonView, isChecked ->
                            MMkvUtil.encode(url, isChecked)
                        }
                        .onNegative { dialog, which ->
                            dismiss()
                        }
                        .onPositive { dialog, which ->
                            gotoDapp(url)
                        }.build().show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }


        }
    }

    private fun gotoDapp(url: String) {
        val count = LitePal.count<PWallet>()
        if (count == 0) {
            toast(getString(R.string.create_wallet_pre))
            return
        }
        val id = MyWallet.getId()
        val wallet = LitePal.find<PWallet>(id)
        if (wallet?.type == PWallet.TYPE_ADDR_KEY) {
            toast(getString(R.string.str_addr_no))
            return
        }
        val newUrl = GoWallet.getNewUrl(url)
        ARouter.getInstance().build(RouterPath.APP_DAPP)
            .withString(RouterPath.PARAM_URL, newUrl).navigation()

    }


    override fun initView() {
        super.initView()
        val linearLayoutManager = LinearLayoutManager(this)
        mCommonAdapter = object : CommonAdapter<String>(this, R.layout.layout_text_m, mUrlList) {
            override fun convert(holder: ViewHolder, str: String, position: Int) {
                holder.setText(R.id.tv_value, str)
            }
        }
        binding.rvList.layoutManager = linearLayoutManager
        binding.rvList.adapter = mCommonAdapter
        binding.rvList.setOnItemClickListener { viewHolder, i ->
            val url = mUrlList[i]
            binding.etSearch.setText(url)
            binding.etSearch.setSelection(url.length)
        }
        binding.rvList.setOnItemLongClickListener { viewHolder, position ->
            MaterialDialog.Builder(this)
                .onPositive { dialog, which ->
                    mUrlList.removeAt(position)
                    mCommonAdapter.notifyDataSetChanged()
                    val urls = Gson().toJson(mUrlList)
                    PreferencesUtils.putString(this, HISTORY_URL_KEY, urls)
                }
                .title(getString(R.string.del_str))
                .content(getString(R.string.del_tip_str))
                .positiveText(getString(R.string.ok))
                .negativeText(getString(R.string.cancel))
                .show();
            false
        }
    }


    override fun initData() {
        super.initData()
        val testurl = PreferencesUtils.getString(this, HISTORY_URL_KEY)
        if (!TextUtils.isEmpty(testurl)) {
            val stringList =
                Gson().fromJson<List<String>>(testurl, object : TypeToken<List<String?>?>() {}.type)
            mUrlList.clear()
            mUrlList.addAll(stringList)
            mCommonAdapter.notifyDataSetChanged()
        }
    }
}