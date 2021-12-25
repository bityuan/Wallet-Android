package com.fzm.walletdemo


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fzm.walletmodule.bean.Explore
import com.fzm.walletmodule.ui.base.BaseFragment
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