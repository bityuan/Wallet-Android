package com.fzm.walletmodule.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.fzm.walletmodule.R
import com.fzm.walletmodule.ui.widget.LoadingView


abstract class BaseFragment : Fragment() {

    private lateinit var loadingView: LoadingView
    @LayoutRes
    abstract fun getLayout(): Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(getLayout(), container, false)
        setHasOptionsMenu(true)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingView = LoadingView()
    }

    fun showLoading() {
        loadingView.show(childFragmentManager, "showLoading")
    }

    fun dismiss() {
        loadingView.dismiss()
    }

    fun showInLoading() {
        val flLoading = activity?.findViewById<FrameLayout>(R.id.fl_loading)
        flLoading?.visibility = View.VISIBLE
    }

    fun disInLoading() {
        val flLoading = activity?.findViewById<FrameLayout>(R.id.fl_loading)
        flLoading?.visibility = View.GONE
    }

    protected open fun configWallets() {}
    protected open fun initView() {}
    protected open fun initIntent() {}
    protected open fun initData() {}
    protected open fun initListener() {}
    protected open fun initRefresh() {}
    protected open fun initObserver() {}

}