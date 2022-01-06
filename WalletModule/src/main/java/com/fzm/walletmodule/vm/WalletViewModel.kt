package com.fzm.walletmodule.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fzm.walletmodule.bean.Miner
import com.fzm.walletmodule.net.HttpResult
import com.fzm.walletmodule.repo.OutRepository
import androidx.lifecycle.viewModelScope
import com.fzm.walletmodule.bean.WithHold
import com.fzm.walletmodule.db.entity.Coin
import com.fzm.walletmodule.repo.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WalletViewModel constructor(private val walletRepository: WalletRepository) : ViewModel() {
    private val _getCoinList = MutableLiveData<HttpResult<List<Coin>>>()
    val getCoinList: LiveData<HttpResult<List<Coin>>>
        get() = _getCoinList

    fun getCoinList(names: List<String>) {
        viewModelScope.launch {
            _getCoinList.value = walletRepository.getCoinList(names)
        }
    }
}