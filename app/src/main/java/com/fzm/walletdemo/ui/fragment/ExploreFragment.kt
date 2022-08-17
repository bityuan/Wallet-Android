package com.fzm.walletdemo.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.RouterPath
import com.fzm.walletdemo.R
import com.fzm.walletdemo.databinding.FragmentExploreBinding
import com.fzm.walletdemo.databinding.FragmentExploreNewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.support.v4.toast
import java.io.Serializable

class ExploreFragment : Fragment() {
    private lateinit var binding: FragmentExploreNewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExploreNewBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeExplore.setOnRefreshListener {
            getExploreAll()
        }
        getExploreAll()
    }


    private fun getExploreAll() {
        lifecycleScope.launch {
            val list = BWallet.get().getExploreList()
            withContext(Dispatchers.Main) {
                binding.swipeExplore.isRefreshing = false


                for (ex in list) {
                    val tvTitle = TextView(context)
                    tvTitle.text = ex.name
                    val ivTitle = ImageView(context)
                    ivTitle.backgroundResource = R.mipmap.header_wallet_hd_wallet
                    ivTitle.setOnClickListener {
                        ARouter.getInstance().build(RouterPath.APP_EXPLORES)
                            .withInt(RouterPath.PARAM_APPS_ID, ex.id).navigation()

                    }
                    binding.llExplore.addView(tvTitle)
                    binding.llExplore.addView(ivTitle)
                }


            }
        }


    }


}