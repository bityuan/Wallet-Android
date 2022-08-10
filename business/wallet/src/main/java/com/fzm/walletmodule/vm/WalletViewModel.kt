package com.fzm.walletmodule.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fzm.wallet.sdk.WalletBean
import com.fzm.wallet.sdk.bean.AppVersion
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.repo.WalletRepository
import kotlinx.coroutines.launch

class WalletViewModel constructor(private val walletRepository: WalletRepository) : ViewModel() {
    private val _getCoinList = MutableLiveData<HttpResult<List<Coin>>>()
    val getCoinList: LiveData<HttpResult<List<Coin>>>
        get() = _getCoinList

    private val _searchCoinList = MutableLiveData<HttpResult<List<Coin>>>()
    val searchCoinList: LiveData<HttpResult<List<Coin>>>
        get() = _searchCoinList

    private val _getTabData = MutableLiveData<HttpResult<List<AddCoinTabBean>>>()
    val getTabData: LiveData<HttpResult<List<AddCoinTabBean>>>
        get() = _getTabData

    private val _getSupportedChain = MutableLiveData<HttpResult<List<Coin>>>()
    val getSupportedChain: LiveData<HttpResult<List<Coin>>>
        get() = _getSupportedChain

    private val _getDNSResolve = MutableLiveData<HttpResult<List<String>>>()
    val getDNSResolve: LiveData<HttpResult<List<String>>>
        get() = _getDNSResolve

    private val _getUpdate = MutableLiveData<HttpResult<AppVersion>>()
    val getUpdate: LiveData<HttpResult<AppVersion>>
        get() = _getUpdate


    fun getCoinList(names: List<String>) {
        viewModelScope.launch {
            _getCoinList.value = walletRepository.getCoinList(names)
        }
    }


    fun searchCoinList(page: Int, limit: Int, keyword: String, chain: String, platform: String) {
        viewModelScope.launch {
            _searchCoinList.value =
                walletRepository.searchCoinList(page, limit, keyword, chain, platform)
        }
    }


    fun getTabData() {
        viewModelScope.launch {
            _getTabData.value = walletRepository.getTabData()
        }
    }


    fun getSupportedChain() {
        viewModelScope.launch {
            _getSupportedChain.value = walletRepository.getSupportedChain()
        }
    }

    //通过域名查询地址的时候，type字段用不上
    //通过地址查询域名的时候，type字段只支持1，所以type默认设置为1
    fun getDNSResolve(type: Int = 1, key: String, kind: Int) {
        viewModelScope.launch {
            _getDNSResolve.value = walletRepository.getDNSResolve(type, key, kind)
        }
    }
    fun getUpdate() {
        viewModelScope.launch {
            _getUpdate.value = walletRepository.getUpdate()
        }
    }


}