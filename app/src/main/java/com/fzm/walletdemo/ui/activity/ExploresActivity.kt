package com.fzm.walletdemo.ui.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletdemo.databinding.ActivityExploresBinding
import com.fzm.walletdemo.ui.adapter.ExploresAdapter
import com.fzm.walletmodule.ui.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.count


@Route(path = RouterPath.APP_EXPLORES)
class ExploresActivity : BaseActivity() {
    @JvmField
    @Autowired(name = RouterPath.PARAM_APPS_ID)
    var appsId: Int = 0

    private val apps = mutableListOf<ExploreBean.AppsBean>()

    private val binding by lazy { ActivityExploresBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        initView()
    }

    override fun initView() {
        super.initView()
        lifecycleScope.launch(Dispatchers.IO) {
            val list = BWallet.get().getExploreCategory(appsId)
            withContext(Dispatchers.Main) {
                binding.rvList.layoutManager = LinearLayoutManager(this@ExploresActivity)
                val app = list[0]
                apps.clear()
                apps.addAll(app.apps)
                title = app.name
                val exAdapter = ExploresAdapter(this@ExploresActivity, apps)
                exAdapter.setOnItemClickListener {
                    apps[it].let { appBean ->
                        if (appBean.type == 1) {
                            val count = LitePal.count<PWallet>()
                            if (count == 0) {
                                toast("请先创建钱包")
                                return@let
                            }
                        }
                        ARouter.getInstance().build(RouterPath.APP_DAPP)
                            .withString("name", appBean.name)
                            .withString("url", appBean.app_url).navigation()
                    }
                }
                binding.rvList.adapter = exAdapter
            }

        }

    }
}