package com.fzm.walletmodule.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.base.LIVE_KEY_WALLET
import com.fzm.wallet.sdk.base.MyWallet
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.BuildConfig
import com.fzm.walletmodule.R
import com.fzm.walletmodule.adapter.BackUpWalletAdapter
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.bean.WalletBackUp
import com.fzm.walletmodule.databinding.ActivityBackUpWalletBinding
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.AutoLineFeedLayoutManager
import com.fzm.walletmodule.ui.widget.FlowTagLayout
import com.fzm.walletmodule.ui.widget.TestDividerItemDecoration
import com.fzm.walletmodule.utils.ListUtils
import com.fzm.walletmodule.utils.ScreenUtils
import com.fzm.walletmodule.utils.ToastUtils
import com.fzm.walletmodule.utils.isFastClick
import com.jeremyliao.liveeventbus.LiveEventBus
import com.zhy.adapter.abslistview.CommonAdapter
import com.zhy.adapter.abslistview.ViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import org.litepal.LitePal
import org.litepal.extension.find

@Route(path = RouterPath.WALLET_BACKUP_WALLET)
class BackUpWalletActivity : BaseActivity() {


    @JvmField
    @Autowired(name = RouterPath.PARAM_WALLET)
    var mPWallet: PWallet? = null

    @JvmField
    @Autowired(name = RouterPath.PARAM_VISIBLE_MNEM)
    var visibleMnem: String? = null

    private var mMnemAdapter: CommonAdapter<WalletBackUp>? = null
    private val mMnemList: ArrayList<WalletBackUp> = ArrayList()
    private val mMnemResultList: ArrayList<WalletBackUp> = ArrayList()
    private var mMnemResultAdapter: BackUpWalletAdapter? = null
    private val binding by lazy { ActivityBackUpWalletBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        mCustomToobar = true
        mStatusColor = Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        setToolBar(R.id.toolbar, R.id.tv_title)
        initIntent()
        initData()
        initMnem()
        initMnemResult()
        initListener()
    }

    override fun initData() {
        visibleMnem?.let {
            val mnemArrays = it.split(" ").toTypedArray()
            for (i in mnemArrays.indices) {
                val backUp = WalletBackUp()
                backUp.mnem = mnemArrays[i]
                backUp.select = 0
                mMnemList.add(backUp)
            }
        }

    }

    private fun initMnem() {
        binding.ftlMnem.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_MULTI)
        mMnemAdapter = object :
            CommonAdapter<WalletBackUp>(this, R.layout.listitem_tag_mnem_chinese, mMnemList) {
            override fun convert(viewHolder: ViewHolder, backUp: WalletBackUp, position: Int) {
                val view: TextView = viewHolder.getView(R.id.tv_tag)
                if (mPWallet?.mnemType == PWallet.TYPE_CHINESE) {
                    val pra =
                        view.layoutParams as LinearLayout.LayoutParams
                    pra.width = ScreenUtils.dp2px(mContext, 40f)
                    pra.height = ScreenUtils.dp2px(mContext, 40f)
                    view.setPadding(0, 0, 0, 0)
                    val margin: Int = (ScreenUtils.getScreenWidth(mContext) - ScreenUtils.dp2px(
                        mContext,
                        34f
                    ) - 6 * ScreenUtils.dp2px(mContext, 40f)) / 5
                    if (position % 6 == 5) {
                        pra.rightMargin = 0
                    } else {
                        pra.rightMargin = margin
                    }
                    view.layoutParams = pra
                } else {
                    val pra = view.layoutParams as LinearLayout.LayoutParams
                    pra.width = LinearLayout.LayoutParams.WRAP_CONTENT
                    pra.height = LinearLayout.LayoutParams.WRAP_CONTENT
                    view.setPadding(
                        ScreenUtils.dp2px(mContext, 9f),
                        ScreenUtils.dp2px(mContext, 5f),
                        ScreenUtils.dp2px(mContext, 9f),
                        ScreenUtils.dp2px(mContext, 6f)
                    )
                    view.layoutParams = pra
                }
                view.isSelected = backUp.select != WalletBackUp.UN_SELECTED
                if (backUp.select == WalletBackUp.UN_SELECTED) {
                    view.setTextColor(resources.getColor(R.color.white))
                } else {
                    view.setTextColor(resources.getColor(R.color.color_8E92A3))
                }
                viewHolder.setText(R.id.tv_tag, backUp.mnem)
            }
        }
        binding.ftlMnem.adapter = mMnemAdapter
        mMnemList.shuffle()
        mMnemAdapter?.notifyDataSetChanged()
        binding.ftlMnem.setOnTagSelectListener { parent, selectedList, position, isSelect ->
            if (!ListUtils.isEmpty(selectedList)) {
                val backUp = parent.adapter.getItem(position) as WalletBackUp

                var isHave = false
                for (i in mMnemResultList.indices) {
                    for (j in mMnemList.indices) {
                        if (mMnemResultList[i].mnem.equals(backUp.mnem) && backUp.select == 1) {
                            isHave = true
                            break
                        }
                    }
                }
                backUp.select = WalletBackUp.SELECTED
                if (!isHave) {
                    updateMenmResult(backUp);
                }
            }
        }
    }


    private fun updateMenmResult(backUp: WalletBackUp) {
        mMnemResultList.add(backUp)
        mMnemResultAdapter?.notifyDataSetChanged()
        mMnemAdapter?.notifyDataSetChanged()
    }

    private fun initMnemResult() {
        if (mPWallet?.mnemType == PWallet.TYPE_CHINESE) {
            val layoutManager =
                AutoLineFeedLayoutManager()
            binding.ftlMnemResult.layoutManager = layoutManager
            binding.ftlMnemResult.addItemDecoration(TestDividerItemDecoration())
        } else {
            // 设置布局管理器
            val layoutManager =
                AutoLineFeedLayoutManager()
            layoutManager.isAutoMeasureEnabled = true
            binding.ftlMnemResult.layoutManager = layoutManager
        }

        // 设置适配器
        mPWallet?.let {
            mMnemResultAdapter = BackUpWalletAdapter(
                this@BackUpWalletActivity,
                R.layout.activity_back_up_wallet_item,
                mMnemResultList,
                it.mnemType
            )
            //给RecyclerView设置适配器
            binding.ftlMnemResult.adapter = mMnemResultAdapter
            mMnemResultAdapter?.notifyDataSetChanged()
        }

        mMnemResultAdapter?.setOnItemClickListener { adapter, view, position ->
            val backUp = adapter.getItem(position) as WalletBackUp
            backUp.select = WalletBackUp.UN_SELECTED
            updateMenm(backUp)
        }
    }

    private fun updateMenm(backUp: WalletBackUp) {
        mMnemResultList.remove(backUp)
        mMnemResultAdapter?.notifyDataSetChanged()
        mMnemAdapter?.notifyDataSetChanged()
    }


    override fun initListener() {
        binding.btnMnem.setOnClickListener {
            if (checked()) {
                gotoMain()
            }

        }
        binding.btnRecover.setOnClickListener {
            if (checked()) {
                ARouter.getInstance().build(RouterPath.WALLET_NEW_RECOVER_ADDRESS)
                    .withSerializable(RouterPath.PARAM_WALLET, mPWallet)
                    .withString(RouterPath.PARAM_VISIBLE_MNEM, visibleMnem)
                    .navigation()
            }
        }
    }

    private fun checked(): Boolean {
        if (BuildConfig.DEBUG) {
            return true
        }

        if (isFastClick()) {
            return false
        }
        val mnemString: String = getMnemString()
        val mnem = visibleMnem!!.replace(" ", "")
        if (mnemString != mnem) {
            toast(getString(R.string.mnemonic_wrong))
            return false
        }
        return true
    }

    private fun gotoMain() {
        lifecycleScope.launch(Dispatchers.Main) {
            showLoading()
            withContext(Dispatchers.IO) {
                mPWallet?.let {
                    val id = BWallet.get().importWallet(
                        WalletConfiguration.mnemonicWallet(
                            visibleMnem!!,
                            it.name,
                            it.password,
                            Constants.getCoins()
                        )
                    )
                    MyWallet.setId(id)
                    LiveEventBus.get<Long>(LIVE_KEY_WALLET).post(id)
                }
            }
            dismiss()
            closeSomeActivitys()
        }
    }

    private fun getMnemString(): String {
        var string = ""
        for (backUp in mMnemResultList) {
            string += backUp.mnem
        }
        return string
    }

}