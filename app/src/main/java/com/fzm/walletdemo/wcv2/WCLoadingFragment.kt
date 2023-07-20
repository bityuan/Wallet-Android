package com.fzm.walletdemo.wcv2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fzm.walletdemo.databinding.FragmentWcLoadingBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WCLoadingFragment: BottomSheetDialogFragment() {

    private lateinit var binding : FragmentWcLoadingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWcLoadingBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}