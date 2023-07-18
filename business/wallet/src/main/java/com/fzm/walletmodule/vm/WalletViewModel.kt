package com.fzm.walletmodule.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fzm.wallet.sdk.base.logDebug
import com.fzm.wallet.sdk.bean.AppVersion
import com.fzm.wallet.sdk.bean.ExploreBean
import com.fzm.wallet.sdk.bean.Notice
import com.fzm.wallet.sdk.bean.Notices
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.net.HttpResult
import com.fzm.wallet.sdk.repo.WalletRepository
import com.fzm.wallet.sdk.utils.GoWallet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.litepal.LitePal
import org.litepal.extension.find

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

    private val _getNoticeList = MutableLiveData<HttpResult<Notices>>()
    val getNoticeList: LiveData<HttpResult<Notices>>
        get() = _getNoticeList

    private val _getNoticeDetail = MutableLiveData<HttpResult<Notice>>()
    val getNoticeDetail: LiveData<HttpResult<Notice>>
        get() = _getNoticeDetail

    private val _getDNSResolve = MutableLiveData<HttpResult<List<String>>>()
    val getDNSResolve: LiveData<HttpResult<List<String>>>
        get() = _getDNSResolve

    private val _getUpdate = MutableLiveData<HttpResult<AppVersion>>()
    val getUpdate: LiveData<HttpResult<AppVersion>>
        get() = _getUpdate


    fun getCoins(id: Long): Flow<List<Coin>> = flow {
        supervisorScope {
            val coinsLocal =
                LitePal.where("pwallet_id = ? and status = 1", id.toString()).find<Coin>(true)
            //第一次返回：数据库数据
            //logDebug("一：钱包id====$id")
            emit(coinsLocal)
            val names = coinsLocal.map { "${it.name},${it.platform}" }
            val result = walletRepository.getCoinList(names)
            if (result.isSucceed()) {
                val coinsNet = result.data()
                coinsNet?.let {
                    for (net in it) {
                        for (local in coinsLocal) {
                            if (local.netId == net.netId) {
                                local.rmb = net.rmb
                                local.icon = net.icon
                                local.nickname = net.nickname
                                local.platform = net.platform
                                local.treaty = net.treaty
                                local.optionalName = net.optionalName
                                local.update(local.id)
                            }
                        }
                    }
                    //第二次返回：图标行情
                    //logDebug("二：钱包id====$id")
                    emit(coinsLocal)

                    val jobs = mutableListOf<Job>()
                    for (coin in coinsLocal) {
                        val job = viewModelScope.launch(Dispatchers.IO) {
                            coin.balance = GoWallet.handleBalance(coin)
                            coin.update(coin.id)
                            //logDebug("当前余额===${coin.name}  ${coin.balance}")
                        }
                        jobs.add(job)
                    }
                    jobs.joinAll()
                    //logDebug("。。。。查询余额完成。。。。。")


                    //logDebug("三：钱包id====$id")
                    emit(coinsLocal)

                }
            }

        }

    }.flowOn(Dispatchers.IO)


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

    fun getNoticeList(page: Int, limit: Int, type: Int) {
        viewModelScope.launch {
            _getNoticeList.value = walletRepository.getNoticeList(page, limit, type)
        }
    }

    fun getNoticeDetail(id: Int) {
        viewModelScope.launch {
            _getNoticeDetail.value = walletRepository.getNoticeDetail(id)
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



     suspend fun getExploreList(): List<ExploreBean> {
        return walletRepository.getExploreList().dataOrNull() ?: emptyList()
    }

     suspend fun getExploreCategory(id: Int): List<ExploreBean> {
        return walletRepository.getExploreCategory(id).dataOrNull() ?: emptyList()
    }


}