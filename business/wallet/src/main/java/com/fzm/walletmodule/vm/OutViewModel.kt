package com.fzm.walletmodule.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.repo.OutRepository
import androidx.lifecycle.viewModelScope
import com.fzm.wallet.sdk.bean.CreateRaw
import kotlinx.coroutines.launch

class OutViewModel constructor(private val outRepository: OutRepository) : ViewModel() {
    private val _getMiner = MutableLiveData<HttpResult<Miner>>()
    val getMiner: LiveData<HttpResult<Miner>>
        get() = _getMiner

    private val _createByContract = MutableLiveData<HttpResult<CreateRaw>>()
    val createByContract: LiveData<HttpResult<CreateRaw>>
        get() = _createByContract

    fun getMiner(name: String) {
        viewModelScope.launch {
            _getMiner.value = outRepository.getMiner(name)
        }
    }

    fun createByContract(
        cointype: String,
        tokensymbol: String,
        from: String,
        to: String,
        amount: Double,
        fee: Double,
        contractAddress: String
    ) {
        viewModelScope.launch {
            _createByContract.value = outRepository.createRawTransaction(
                cointype,
                tokensymbol,
                from,
                to,
                amount,
                fee,
                contractAddress
            )
        }
    }


}