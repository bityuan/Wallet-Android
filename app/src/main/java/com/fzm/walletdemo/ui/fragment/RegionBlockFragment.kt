package com.fzm.walletdemo.ui.fragment

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fzm.wallet.sdk.utils.RegionHelper
import com.fzm.walletdemo.R
import com.fzm.walletdemo.ui.activity.MainActivity
import kotlinx.coroutines.launch

class RegionBlockFragment : Fragment() {

    private var iconSpinAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_region_block, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ivIcon = view.findViewById<ImageView>(R.id.iv_region_icon)
        val btnRetry = view.findViewById<View>(R.id.btn_retry)
        btnRetry.setOnClickListener {
            lifecycleScope.launch {
                btnRetry.isEnabled = false
                startIconSpin(ivIcon)
                try {
                    RegionHelper.checkRegion(requireContext())
                } finally {
                    stopIconSpin(ivIcon)
                    if (RegionHelper.isChinaUser()) {
                        btnRetry.isEnabled = true
                    } else {
                        (activity as? MainActivity)?.showExploreAfterRegionUnblocked()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        iconSpinAnimator?.cancel()
        iconSpinAnimator = null
        super.onDestroyView()
    }

    private fun startIconSpin(iv: ImageView) {
        iconSpinAnimator?.cancel()
        iconSpinAnimator = ObjectAnimator.ofFloat(iv, View.ROTATION, 0f, 360f).apply {
            duration = 1000L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopIconSpin(iv: ImageView) {
        iconSpinAnimator?.cancel()
        iconSpinAnimator = null
        iv.rotation = 0f
    }
}
