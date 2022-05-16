package com.fzm.nft

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fzm.wallet.sdk.net.HttpResult
import kotlinx.coroutines.launch
import walletapi.Walletapi

class NFTViewModel constructor(private val nftRepository: NFTRepository) : ViewModel() {

    private val _getNFTBalance = MutableLiveData<HttpResult<String>>()
    val getNFTBalance: LiveData<HttpResult<String>>
        get() = _getNFTBalance

    fun getNFTBalance(
        cointype: String = Walletapi.TypeETHString,
        tokensymbol: String = "",
        from: String,
        contractAddr: String
    ) {
        viewModelScope.launch {
            _getNFTBalance.value =
                nftRepository.getNFTBalance(cointype, tokensymbol, from, contractAddr)
        }
    }

}