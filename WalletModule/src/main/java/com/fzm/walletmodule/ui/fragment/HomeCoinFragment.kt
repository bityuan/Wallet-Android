package com.fzm.walletmodule.ui.fragment

import android.annotation.SuppressLint
import android.app.Service
import android.graphics.Color
import android.os.Bundle
import android.os.Vibrator
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.db.entity.PWallet
import com.fzm.wallet.sdk.net.walletQualifier
import com.fzm.walletmodule.R
import com.fzm.walletmodule.event.AddCoinEvent
import com.fzm.walletmodule.ui.base.BaseFragment
import com.fzm.walletmodule.utils.WalletUtils
import com.fzm.walletmodule.vm.WalletViewModel
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import kotlinx.android.synthetic.main.fragment_home_coin.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.support.v4.toast
import org.koin.android.ext.android.inject
import org.litepal.LitePal.where
import java.lang.String
import java.util.*


/**
 *
 * create an instance of this fragment.
 */
class HomeCoinFragment : BaseFragment() {
    private var mCommonAdapter: CommonAdapter<Coin>? = null
    private var data = mutableListOf<Coin>()
    private lateinit var mPWallet: PWallet
    private val walletViewModel: WalletViewModel by inject(walletQualifier)
    var needUpdate = false
    override fun getLayout(): Int {
        return R.layout.fragment_home_coin
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
        refresh()
    }

    override fun initView() {
        mPWallet = WalletUtils.getUsingWallet()
        data = where("pwallet_id = ? ", String.valueOf(mPWallet.id)).find(Coin::class.java, true)
        swipe_target.layoutManager = LinearLayoutManager(activity)
        mCommonAdapter = object : CommonAdapter<Coin>(activity, R.layout.listitem_addcoin, data) {
            override fun convert(holder: ViewHolder, coin: Coin, position: Int) {
                val ivAddCoin = holder.getView<ImageView>(R.id.iv_addcoin_icon)
                Glide.with(mContext)
                    .load(coin.icon)
                    .into(ivAddCoin)
                holder.setText(R.id.tv_addcoin_name, coin.getName())
                holder.setText(R.id.tv_addcoin_name_cn, coin.getNickname())
                holder.setImageResource(
                    R.id.iv_add_remove_coin,
                    if (coin.status == Coin.STATUS_ENABLE) R.mipmap.icon_removecoin else R.mipmap.icon_addcoin
                )
                holder.setOnClickListener(R.id.iv_add_remove_coin) {
                    if (coin.status == Coin.STATUS_ENABLE) {
                        coin.status = Coin.STATUS_DISABLE
                        holder.setImageResource(R.id.iv_add_remove_coin, R.mipmap.icon_addcoin)
                    } else {
                        coin.status = Coin.STATUS_ENABLE
                        holder.setImageResource(R.id.iv_add_remove_coin, R.mipmap.icon_removecoin)
                    }
                    updateCoin(holder, coin)
                }
            }
        }
        swipe_target.adapter = mCommonAdapter
        //拖曳排序
        helper.attachToRecyclerView(swipe_target)
    }

    override fun initData() {
        walletViewModel.getCoinList.observe(viewLifecycleOwner, {
            if (it.isSucceed()) {
                val coinLists = it.data()
                if (coinLists.isNullOrEmpty()) {
                    return@observe
                }
                for (newCoin in coinLists){
                    for (oldCoin in data){
                        if (TextUtils.equals(
                                oldCoin.name + oldCoin.chain + oldCoin.platform,
                                newCoin.name + newCoin.chain + newCoin.platform
                            )
                        ) {
                            //更新图标等信息,通过go查出来有余额的币种没有图标
                            newCoin.update(oldCoin.id)
                        }
                    }
                }
                refresh()
            } else {
                toast(it.error())
            }
        })
        val stringList: MutableList<kotlin.String> = ArrayList()
        for (coin in data) {
            stringList.add(coin.name + "," + coin.platform)
        }
        walletViewModel.getCoinList(stringList)
    }

    //拖曳排序
    private val helper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            var dragFrlg = 0
            if (recyclerView.layoutManager is GridLayoutManager) {
                dragFrlg =
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            } else if (recyclerView.layoutManager is LinearLayoutManager) {
                dragFrlg = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            }
            return makeMovementFlags(dragFrlg, 0)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            //得到当拖拽的viewHolder的Position
            val fromPosition = viewHolder.adapterPosition
            //拿到当前拖拽到的item的viewHolder
            val toPosition = target.adapterPosition
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(data, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(data, i, i - 1)
                }
            }
            needUpdate = true
            mCommonAdapter!!.notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            //侧滑删除可以使用；
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }

        /**
         * 长按选中Item的时候开始调用
         * 长按高亮
         * @param viewHolder
         * @param actionState
         */
        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                viewHolder!!.itemView.setBackgroundColor(Color.WHITE)
                //获取系统震动服务//震动70毫秒
                val vib = activity!!.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
                vib.vibrate(70)
            } else {
                for (i in data.indices) {
                    val coin: Coin = data.get(i)
                    if (i == 0) {
                        coin.setToDefault("sort")
                    } else {
                        coin.sort = i
                    }
                    coin.update(coin.id)
                    //                    Log.d("nyb", "coinName:" + coin.getName());
                }
            }
            super.onSelectedChanged(viewHolder, actionState)
        }

        /**
         * 手指松开的时候还原高亮
         * @param recyclerView
         * @param viewHolder
         */
        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.setBackgroundColor(0)
            //  mCommonAdapter.notifyDataSetChanged();  //完成拖动后刷新适配器，这样拖动后删除就不会错乱
        }
    })

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        data.clear()
        data.addAll(
            where(
                "pwallet_id = ? ",
                String.valueOf(mPWallet.id)
            ).find(Coin::class.java, true)
        )
        //        data = LitePal.where("pwallet_id = ? ", String.valueOf(mPWallet.getId())).find(Coin.class, true);
        data.sort()
        mCommonAdapter?.notifyDataSetChanged()
    }


    private fun updateCoin(holder: ViewHolder, coin: Coin) {
        holder.setImageResource(
            R.id.iv_add_remove_coin,
            if (coin.status == Coin.STATUS_ENABLE) R.mipmap.icon_removecoin else R.mipmap.icon_addcoin
        )
        coin.status =
            if (coin.status == Coin.STATUS_ENABLE) Coin.STATUS_ENABLE else Coin.STATUS_DISABLE
        for (homeCoin in data) {
            if (coin.chain.equals(homeCoin.chain)) {
                coin.address = homeCoin.address
                break
            }
        }
        //更新
        coin.update(coin.id)
        EventBus.getDefault().post(AddCoinEvent())
    }
}