package com.fzm.nft

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fzm.wallet.sdk.net.HttpResult
import kotlinx.coroutines.launch
import walletapi.Walletapi

class NFTViewModel constructor(private val nftRepository: NFTRepository) : ViewModel() {


    private val _getNFTValue = MutableLiveData<NftValue>()
    val getNFTValue: LiveData<NftValue>
        get() = _getNFTValue

    private val _getNftTran = MutableLiveData<List<NftTran>>()
    val getNftTran: LiveData<List<NftTran>>
        get() = _getNftTran

    private val _outNFT = MutableLiveData<String>()
    val outNFT: LiveData<String>
        get() = _outNFT

    private val _getNFTList = MutableLiveData<List<String>>()
    val getNFTList: LiveData<List<String>>
        get() = _getNFTList

    fun getNFTBalance(
        position: Int,
        cointype: String = Walletapi.TypeETHString,
        tokensymbol: String = "",
        from: String,
        contractAddr: String
    ) {
        viewModelScope.launch {
            val result = nftRepository.getNFTBalance(cointype, tokensymbol, from, contractAddr)
            if (result.isSucceed()) {
                result.data()?.let {
                    _getNFTValue.value = NftValue(position, contractAddr, it)
                }

            }

        }
    }


    fun getNFTTran(
        cointype: String = Walletapi.TypeETHString,
        tokensymbol: String = "",
        contractAddr: String,
        address: String,
        index: Int,
        count: Int,
        direction: Int,
        type: Int
    ) {
        viewModelScope.launch {
            val result = nftRepository.getNFTTran(
                cointype,
                tokensymbol,
                contractAddr,
                address,
                index,
                count,
                direction,
                type
            )
            if (result.isSucceed()) {
                result.data()?.let {
                    _getNftTran.value = it
                }
            }
        }
    }

    fun outNFT(
        cointype: String = Walletapi.TypeETHString,
        tokenId: String,
        contractAddr: String,
        from: String,
        to: String,
        fee: Double
    ) {
        viewModelScope.launch {
            val result = nftRepository.outNFT(cointype, tokenId, contractAddr, from, to, fee)
            if (result.isSucceed()) {
                result.data()?.let {
                    _outNFT.value = it
                }
            }
        }
    }

    fun getNFTList(
        contractAddr: String,
        from: String,
    ) {
        viewModelScope.launch {
            val result = nftRepository.getNFTList(contractAddr = contractAddr, from = from)
            if (result.isSucceed()) {
                result.data()?.let {
                    _getNFTList.value = it
                }
            }
        }
    }

}