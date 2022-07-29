package com.fzm.walletdemo.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityWebTestBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.utils.PreferencesUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import java.util.ArrayList

@Route(path = RouterPath.APP_WEBTEST)
class WebTestActivity : BaseActivity() {
    private val mUrlList: MutableList<String> = ArrayList()
    private lateinit var mCommonAdapter: CommonAdapter<*>
    private val binding by lazy { ActivityWebTestBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        initData()
        binding.btnToWeb.setOnClickListener {
            val url = binding.etUrl.text.toString()
            if (!mUrlList.contains(url)) {
                mUrlList.add(url)
                val urls = Gson().toJson(mUrlList)
                PreferencesUtils.putString(this, "testurl", urls)
            }
            ARouter.getInstance().build(RouterPath.APP_DAPP).withString("name", "web测试")
                .withString("url", binding.etUrl.text.toString()).navigation()
        }
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
            binding.etUrl.setText(mUrlList[i])
        }
        binding.rvList.setOnItemLongClickListener { viewHolder, position ->
            MaterialDialog.Builder(this)
                .onPositive { dialog, which ->
                    mUrlList.removeAt(position)
                    mCommonAdapter.notifyDataSetChanged()
                    val urls = Gson().toJson(mUrlList)
                    PreferencesUtils.putString(this, "testurl", urls)
                }
                .title("删除")
                .content("是否删除")
                .positiveText("确定")
                .negativeText("取消")
                .show();
            false
        }
    }

    override fun initData() {
        super.initData()
        val testurl = PreferencesUtils.getString(this, "testurl")
        if (!TextUtils.isEmpty(testurl)) {
            val stringList =
                Gson().fromJson<List<String>>(testurl, object : TypeToken<List<String?>?>() {}.type)
            mUrlList.clear()
            mUrlList.addAll(stringList)
            mCommonAdapter.notifyDataSetChanged()
        }
    }
}