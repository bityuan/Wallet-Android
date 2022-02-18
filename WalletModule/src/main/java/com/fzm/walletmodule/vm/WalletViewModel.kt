package com.fzm.walletmodule.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.repo.WalletRepository
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
    private val _searchCoinList = MutableLiveData<HttpResult<List<Coin>>>()
    val searchCoinList: LiveData<HttpResult<List<Coin>>>
        get() = _searchCoinList

    fun searchCoinList(page: Int, limit: Int, keyword: String, chain: String, platform: String) {
        viewModelScope.launch {
            _searchCoinList.value = walletRepository.searchCoinList(page, limit, keyword, chain, platform)
        }
    }

    private val _getTabData = MutableLiveData<HttpResult<List<AddCoinTabBean>>>()
    val getTabData: LiveData<HttpResult<List<AddCoinTabBean>>>
        get() = _getTabData

    fun getTabData() {
        viewModelScope.launch {
            _getTabData.value = walletRepository.getTabData()
        }
    }
}