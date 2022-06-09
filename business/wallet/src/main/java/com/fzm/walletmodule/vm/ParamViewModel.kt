package com.fzm.walletmodule.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ParamViewModel:ViewModel() {

    val walletName = MutableLiveData<String>()
    val walletMoney = MutableLiveData<String>()
}