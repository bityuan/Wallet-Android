package com.fzm.walletdemo.ui.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BuildConfig
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.LIVE_KEY_SCAN
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.PRE_X_RECOVER
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityWebTestBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.EditDialogFragment
import com.fzm.walletmodule.utils.ClipboardUtils
import com.fzm.walletmodule.utils.PreferencesUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.litepal.LitePal
import org.litepal.extension.find
import walletapi.HDWallet
import walletapi.Walletapi
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
                .withString(RouterPath.PARAM_URL, binding.etUrl.text.toString()).navigation()
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
                .title(getString(R.string.del_str))
                .content(getString(R.string.del_tip_str))
                .positiveText(getString(R.string.ok))
                .negativeText(getString(R.string.cancel))
                .show();
            false
        }

        binding.btnFromPara.setOnClickListener {
            val intent = Intent(this, GcTestActivity::class.java)
            startActivity(intent)
        }

        binding.btnFromLock.setOnClickListener {
            val intent = Intent(this, LockTestActivity::class.java)
            startActivity(intent)

        }

        binding.btnPriv.setOnClickListener {
            val editDialogFragment =
                EditDialogFragment()
            editDialogFragment.setTitle(getString(com.fzm.walletmodule.R.string.my_wallet_detail_password))
            editDialogFragment.setHint(getString(com.fzm.walletmodule.R.string.my_wallet_detail_password))
            editDialogFragment.setAutoDismiss(false)
            editDialogFragment.setType(1)
                .setRightButtonStr(getString(com.fzm.walletmodule.R.string.ok))
                .setOnButtonClickListener(object : EditDialogFragment.OnButtonClickListener {
                    override fun onLeftButtonClick(v: View?) {}
                    override fun onRightButtonClick(v: View?) {
                        val etInput: EditText = editDialogFragment.etInput
                        val value = etInput.text.toString()
                        if (TextUtils.isEmpty(value)) {
                            toast(getString(com.fzm.walletmodule.R.string.rsp_dialog_input_password))
                            return
                        }
                        editDialogFragment.dismiss()
                        handlePasswordAfter(value)
                    }
                })
            editDialogFragment.showDialog("tag", supportFragmentManager)
        }
    }


    fun handlePasswordAfter(password: String) {
        showLoading()
        doAsync {
            try {
                val pWallet = LitePal.find<PWallet>(MyWallet.getId())
                val bPassword: ByteArray? = GoWallet.encPasswd(password)
                val mnem: String = GoWallet.decMenm(bPassword!!, pWallet!!.mnem)
                if (!TextUtils.isEmpty(mnem)) {
                    val hdWallet: HDWallet? = GoWallet.getHDWallet("BTC", mnem)
                    val priv = Walletapi.byteTohex(hdWallet!!.newKeyPriv(100))
                    logDebug("ppp === $priv")
                    dismiss()
                } else {
                    uiThread {
                        dismiss()
                        toast(getString(com.fzm.walletmodule.R.string.my_wallet_detail_wrong_password))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        LiveEventBus.get<String>(LIVE_KEY_SCAN).observe(this, Observer { scan ->
            ARouter.getInstance().build(RouterPath.APP_SCAN_RESULT)
                .withString(RouterPath.PARAM_SCAN, scan).navigation()

        })
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuItem = menu.add(0, 1, 0, "邮箱验证")
        val menuItem2 = menu.add(0, 2, 0, "扫一扫")
        if(BuildConfig.DEBUG){
            val menuItem3 = menu.add(0, 3, 0, "测试")
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                ARouter.getInstance().build(RouterPath.WALLET_CHECKEMAIL).navigation()
            }
            2 -> {
                ARouter.getInstance().build(RouterPath.WALLET_CAPTURE).navigation()

            }
        }
        return super.onOptionsItemSelected(item)
    }
}