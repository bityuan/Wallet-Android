package com.fzm.walletmodule.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.walletmodule.R
import com.fzm.walletmodule.adapter.BackUpWalletAdapter
import com.fzm.walletmodule.base.Constants
import com.fzm.walletmodule.bean.WalletBackUp
import com.fzm.walletmodule.event.BackUpEvent
import com.fzm.walletmodule.event.InitPasswordEvent
import com.fzm.walletmodule.event.MyWalletEvent
import com.fzm.walletmodule.manager.WalletManager
import com.fzm.walletmodule.ui.base.BaseActivity
import com.fzm.walletmodule.ui.widget.AutoLineFeedLayoutManager
import com.fzm.walletmodule.ui.widget.FlowTagLayout
import com.fzm.walletmodule.ui.widget.TestDividerItemDecoration
import com.fzm.walletmodule.utils.*
import com.zhy.adapter.abslistview.CommonAdapter
import com.zhy.adapter.abslistview.ViewHolder
import kotlinx.android.synthetic.main.activity_back_up_wallet.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.*

/**
 * 创建账户时验证助记词和导入钱包页面
 */
class BackUpWalletActivity : BaseActivity() {
    private lateinit var mPWallet: PWallet
    private var mFrom: String? = null
    private var mnemFrom: String? = null
    private var mMnemAdapter: CommonAdapter<WalletBackUp>? = null
    private val mMnemList: ArrayList<WalletBackUp> = ArrayList()
    private val mMnemResultList: ArrayList<WalletBackUp> = ArrayList()
    private var mMnemResultAdapter: BackUpWalletAdapter? = null
    var onItemDragListener: OnItemDragListener = object : OnItemDragListener {
        override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {}
        override fun onItemDragMoving(
            source: RecyclerView.ViewHolder,
            from: Int,
            target: RecyclerView.ViewHolder,
            to: Int
        ) {
        }

        override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder, pos: Int) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        mConfigFinish = true
        mCustomToobar = true
        mStatusColor = Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_back_up_wallet)
        setToolBar(R.id.toolbar, R.id.tv_title)
        title = ""
        initIntent()
        initData()
        initMnem()
        initMnemResult()
        initListener()
    }

    override fun initIntent() {
        mPWallet = intent.getSerializableExtra(PWallet::class.java.simpleName) as PWallet
        mnemFrom = intent.getStringExtra(MNEM_TAG)
        if (mnemFrom == null) {
            mnemFrom = ""
        }
        mFrom = intent.getStringExtra(Constants.FROM)

    }

    override fun initData() {
        val mnemArrays = mnemFrom!!.split(" ").toTypedArray()
        for (i in mnemArrays.indices) {
            val backUp = WalletBackUp()
            backUp.mnem = mnemArrays[i]
            backUp.select = 0
            mMnemList.add(backUp)
        }
    }

    fun initMnem() {
        ftl_mnem.setTagCheckedMode(FlowTagLayout.FLOW_TAG_CHECKED_MULTI)
        mMnemAdapter = object :
            CommonAdapter<WalletBackUp>(this, R.layout.listitem_tag_mnem_chinese, mMnemList) {
            override fun convert(viewHolder: ViewHolder, backUp: WalletBackUp, position: Int) {
                val view: TextView = viewHolder.getView(R.id.tv_tag)
                if (mPWallet.mnemType == PWallet.TYPE_CHINESE) {
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
        ftl_mnem.adapter = mMnemAdapter
        mMnemList.shuffle()
        mMnemAdapter?.notifyDataSetChanged()
        ftl_mnem.setOnTagSelectListener { parent, selectedList, position, isSelect ->
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
                checkButton()
            }
        }
    }

    private fun checkButton() {
        var clickEnable = true
        for (i in mMnemList.indices) {
            if (mMnemList[i].select == WalletBackUp.UN_SELECTED) {
                clickEnable = false
                break
            }
        }
        if (clickEnable) {
            btn_ok.setTextColor(resources.getColor(R.color.white))
            btn_ok.setBackgroundResource(R.drawable.bg_button_word_press)
        } else {
            btn_ok.setTextColor(resources.getColor(R.color.color_9EA2AD))
            btn_ok.setBackgroundResource(R.drawable.bg_button_word)
        }
        btn_ok.isEnabled = clickEnable
    }

    private fun updateMenmResult(backUp: WalletBackUp) {
        mMnemResultList.add(backUp)
        mMnemResultAdapter?.notifyDataSetChanged()
        mMnemAdapter?.notifyDataSetChanged()
    }

    private fun initMnemResult() {
        if (mPWallet.mnemType == PWallet.TYPE_CHINESE) {
            val layoutManager = AutoLineFeedLayoutManager()
            ftl_mnem_result.layoutManager = layoutManager
            ftl_mnem_result.addItemDecoration(TestDividerItemDecoration())
        } else {
            // 设置布局管理器
            val layoutManager = AutoLineFeedLayoutManager()
            layoutManager.isAutoMeasureEnabled = true
            ftl_mnem_result.layoutManager = layoutManager
        }

        // 设置适配器
        mMnemResultAdapter = BackUpWalletAdapter(
            this@BackUpWalletActivity,
            R.layout.activity_back_up_wallet_item,
            mMnemResultList,
            mPWallet.mnemType
        )
        //给RecyclerView设置适配器
        ftl_mnem_result.adapter = mMnemResultAdapter
        mMnemResultAdapter?.notifyDataSetChanged()
        //val itemDragAndSwipeCallback = ItemDragAndSwipeCallback(mMnemResultAdapter)
        //val itemTouchHelper = ItemTouchHelper(itemDragAndSwipeCallback)
        //itemTouchHelper.attachToRecyclerView(ftl_mnem_result)

        // 开启拖拽
        //mMnemResultAdapter?.enableDragItem(itemTouchHelper, R.id.recycle_text, true)
        //mMnemResultAdapter?.setOnItemDragListener(onItemDragListener)

        mMnemResultAdapter?.setOnItemClickListener { adapter, view, position ->
            val backUp = adapter.getItem(position) as WalletBackUp
            backUp.select = WalletBackUp.UN_SELECTED
            updateMenm(backUp)
            checkButton()
        }
    }

    private fun updateMenm(backUp: WalletBackUp) {
        mMnemResultList.remove(backUp)
        mMnemResultAdapter?.notifyDataSetChanged()
        mMnemAdapter?.notifyDataSetChanged()
    }


    override fun initListener() {
        btn_ok.setOnClickListener {
            if (isFastClick()) {
                return@setOnClickListener
            }
            val mnemString: String? = getMnemString()
            val mnem = mnemFrom!!.replace(" ", "")
            if (mnemString != mnem) {
                ToastUtils.show(this, getString(R.string.mnemonic_wrong))
                return@setOnClickListener
            }
            if (mFrom == WalletManager::class.java.simpleName) {
                EventBus.getDefault().post(BackUpEvent())
                ToastUtils.show(this, getString(R.string.backup_success))
                finish()
                return@setOnClickListener
            }

            EventBus.getDefault().post(InitPasswordEvent(mPWallet.password))
            showLoading()

            lifecycleScope.launch {
                val id = BWallet.get().importWallet(
                    WalletConfiguration.mnemonicWallet(
                        mnemFrom!!,
                        mPWallet.name,
                        mPWallet.password,
                        "",
                        Constants.getCoins()
                    ), true
                )
                val pWallet = BWallet.get().findWallet(id)

                dismiss()
                WalletUtils.setUsingWallet(pWallet)
                EventBus.getDefault().postSticky(MyWalletEvent(pWallet))
                closeSomeActivitys()
            }

        }
    }

    private fun getMnemString(): String? {
        var string = ""
        for (backUp in mMnemResultList) {
            string += backUp.mnem
        }
        return string
    }


    companion object {
        const val MNEM_TAG = "mnem"

        fun launch(context: Context, pWallet: PWallet, from: String) {
            val intent = Intent(context, BackUpWalletActivity::class.java)
            intent.putExtra(PWallet::class.java.simpleName, pWallet)
            intent.putExtra(Constants.FROM, from)
            context.startActivity(intent)
        }

        fun launch(context: Context, pWallet: PWallet?, mnem: String?, from: String?) {
            val `in` = Intent(context, BackUpWalletActivity::class.java)
            `in`.putExtra(PWallet::class.java.simpleName, pWallet)
            `in`.putExtra(MNEM_TAG, mnem)
            `in`.putExtra(Constants.FROM, from)
            context.startActivity(`in`)
        }
    }
}