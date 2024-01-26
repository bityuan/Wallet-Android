package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.wallet.sdk.utils.GoWallet
import com.fzm.wallet.sdk.utils.MMkvUtil
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.ActivityExploresBinding
import com.fzm.walletdemo.ui.adapter.ExploresAdapter
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.vm.WalletViewModel
import com.google.android.material.button.MaterialButton
import com.kongzue.dialogx.dialogs.PopMenu
import com.kongzue.dialogx.interfaces.OnIconChangeCallBack
import com.kongzue.dialogx.interfaces.OnMenuItemClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import org.koin.android.ext.android.inject
import org.litepal.LitePal
import org.litepal.extension.count
import org.litepal.extension.find


@Route(path = RouterPath.APP_EXPLORES)
class ExploresActivity : BaseActivity() {
    @JvmField
    @Autowired(name = RouterPath.PARAM_APPS_ID)
    var appsId: Int = 0

    private val apps = mutableListOf<ExploreBean.AppsBean>()

    private val binding by lazy { ActivityExploresBinding.inflate(layoutInflater) }
    private val walletViewModel: WalletViewModel by inject(walletQualifier)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initView()
    }


    override fun initView() {
        super.initView()
        lifecycleScope.launch(Dispatchers.IO) {
            val list = walletViewModel.getExploreCategory(appsId)
            withContext(Dispatchers.Main) {
                try {
                    binding.rvList.layoutManager = LinearLayoutManager(this@ExploresActivity)
                    val app = list[0]
                    apps.clear()
                    apps.addAll(app.apps)
                    title = app.name
                    val exAdapter = ExploresAdapter(this@ExploresActivity, apps)
                    exAdapter.setOnItemClickListener {
                        try {
                            val appId = "${app.apps[it].id}"
                            val type = app.apps[it].type
                            if (MMkvUtil.decodeBoolean(appId)) {
                                gotoDapp(it, type)
                            } else {
                                MaterialDialog.Builder(this@ExploresActivity)
                                    .negativeText(getString(R.string.cancel))
                                    .positiveText(getString(R.string.ok))
                                    .title(getString(R.string.explore_title))
                                    .content(getString(R.string.explore_disclaimer)).checkBoxPrompt(
                                        getString(R.string.no_dotip), false
                                    ) { buttonView, isChecked ->
                                        MMkvUtil.encode(appId, isChecked)
                                    }.onNegative { dialog, which ->
                                        dismiss()
                                    }.onPositive { dialog, which ->
                                        gotoDapp(it, type)
                                    }.build().show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }


                    }
                    binding.rvList.adapter = exAdapter

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

    }

    private fun gotoDapp(index: Int, type: Int) {
        apps[index].let { appBean ->
            if (appBean.type == 1) {
                val count = LitePal.count<PWallet>()
                if (count == 0) {
                    toast(getString(R.string.create_wallet_pre))
                    return@let
                }
            }
            val id = MyWallet.getId()
            val wallet = LitePal.find<PWallet>(id)
            if (wallet?.type == PWallet.TYPE_ADDR_KEY) {
                toast(getString(R.string.str_addr_no))
                return@let
            }

            ARouter.getInstance().build(RouterPath.APP_DAPP).withString("name", appBean.name)
                .withString(RouterPath.PARAM_URL, appBean.app_url).navigation()
        }
    }
}