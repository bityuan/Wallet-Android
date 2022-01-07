package com.fzm.walletmodule.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.repo.OutRepository
import androidx.lifecycle.viewModelScope
import com.fzm.wallet.sdk.bean.WithHold
import kotlinx.coroutines.launch

class OutViewModel constructor(private val outRepository: OutRepository) : ViewModel() {
    private val _getMiner = MutableLiveData<HttpResult<Miner>>()
    val getMiner: LiveData<HttpResult<Miner>>
        get() = _getMiner


    private val _getWithHold = MutableLiveData<HttpResult<WithHold>>()
    val getWithHold: LiveData<HttpResult<WithHold>>
        get() = _getWithHold

    fun getMiner(name: String) {
        viewModelScope.launch {
            _getMiner.value = outRepository.getMiner(name)
        }
    }


    fun getWithHold(paltform: String, coinName: String) {
        viewModelScope.launch {
            _getWithHold.value = outRepository.getWithHold(paltform, coinName)
        }
    }
}