package com.fzm.walletmodule.ui.fragment


import android.os.Bundle
import android.view.View
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.base.BaseFragment


class ExploreFragment : BaseFragment() {
    override fun getLayout(): Int {
        return R.layout.fragment_explore
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initObserver()
    }

}