package com.fzm.walletmodule.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fzm.wallet.sdk.bean.ExchangeFee
import com.fzm.wallet.sdk.bean.WithHold
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.repo.ExchangeRepository
import com.fzm.wallet.sdk.repo.WalletRepository
import kotlinx.coroutines.launch

class ExchangeViewModel constructor(private val exchangeRepository: ExchangeRepository) :
    ViewModel() {
    private val _flashExchange = MutableLiveData<HttpResult<String>>()
    val flashExchange: LiveData<HttpResult<String>>
        get() = _flashExchange

    private val _getExLimit = MutableLiveData<HttpResult<Double>>()
    val getExLimit: LiveData<HttpResult<Double>>
        get() = _getExLimit

    private val _getExFee = MutableLiveData<HttpResult<ExchangeFee>>()
    val getExFee: LiveData<HttpResult<ExchangeFee>>
        get() = _getExFee

    fun flashExchange(
        cointype: String,
        tokensymbol: String,
        bindAddress: String,
        rawTx: String,
        amount: Double,
        to: String,
        gasfee: Boolean
    ) {
        viewModelScope.launch {
            _flashExchange.value = exchangeRepository.flashExchange(
                cointype,
                tokensymbol,
                bindAddress,
                rawTx,
                amount,
                to,
                gasfee
            )
        }
    }

    fun getExLimit(
        address: String,
        cointype: String,
        tokensymbol: String
    ) {
        viewModelScope.launch {
            _getExLimit.value = exchangeRepository.getExLimit(address, cointype, tokensymbol)
        }

    }

    fun getExFee(
        cointype: String,
        tokensymbol: String
    ) {
        viewModelScope.launch {
            _getExFee.value = exchangeRepository.getExFee(cointype, tokensymbol)
        }

    }
}