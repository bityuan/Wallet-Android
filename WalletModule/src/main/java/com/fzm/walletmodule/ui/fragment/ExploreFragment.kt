package com.fzm.walletmodule.ui.fragment


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fzm.walletmodule.R
import com.fzm.walletmodule.bean.Explore
import com.fzm.walletmodule.ui.activity.WebAppDetailsActivity
import com.fzm.walletmodule.ui.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_explore.*
import kotlinx.android.synthetic.main.listitem_explore_vertical.view.*


class ExploreFragment : BaseFragment() {
    override fun getLayout(): Int {
        return R.layout.fragment_explore
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initObserver()
    }

    override fun initData() {
        super.initData()
        val list = arrayListOf<Explore>()
        val e1 = Explore(0, "去中心化交易所", "去中心化交易所详情", R.mipmap.ic_app)
        val e2 = Explore(1, "跨链桥", "跨链桥详情", R.mipmap.ic_app)
        list.add(e1)
        list.add(e2)
        val exAdapter = ExAdapter(list)
        exAdapter.setOnItemClickListener(object : ExAdapter.OnItemClickListener {
            override fun onItemClick(v: View, position: Int) {
                val ex = list[position]
                val intent = Intent(activity, WebAppDetailsActivity::class.java)
                when (ex.id) {
                    0 -> {
                        intent.putExtra(WebAppDetailsActivity.URL,"http://172.16.103.198:8078/home")
                        intent.putExtra(WebAppDetailsActivity.TITLE, "去中心化交易所")
                    }
                    1 -> {
                        intent.putExtra(WebAppDetailsActivity.URL,"http://121.40.18.70:8065/bridge")
                        intent.putExtra(WebAppDetailsActivity.TITLE, "跨链桥")
                    }
                }
                startActivity(intent)
            }

        })
        rv_list.layoutManager = LinearLayoutManager(activity)
        rv_list.adapter = exAdapter

    }


    companion object {
        class ExAdapter(var list: List<Explore>) : RecyclerView.Adapter<ExAdapter.ViewHolder>() {
            class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.listitem_explore_vertical, parent, false)
                return ViewHolder(view)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val explore = list[position]
                holder.itemView.tv_explore_vertical_title.text = explore.name
                holder.itemView.setOnClickListener {
                    onItemClickListener.onItemClick(holder.itemView, position)
                }
            }

            override fun getItemCount(): Int {
                return list.size
            }

            private lateinit var onItemClickListener: OnItemClickListener
            fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
                this.onItemClickListener = onItemClickListener
            }

            interface OnItemClickListener {
                fun onItemClick(v: View, position: Int)
            }
        }
    }
}