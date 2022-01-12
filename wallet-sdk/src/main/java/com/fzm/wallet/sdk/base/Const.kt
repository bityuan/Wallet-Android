package com.fzm.wallet.sdk.base

import com.fzm.wallet.sdk.db.entity.Coin

/**
 * @author zhengjy
 * @since 2022/01/12
 * Description:
 */

/**
 * 默认币种列表
 */
internal val DEFAULT_COINS = listOf(
    Coin().apply {
        chain = "BTY"
        name = "BTY"
    },
    Coin().apply {
        chain = "ETH"
        name = "ETH"
    },
    Coin().apply {
        chain = "BTC"
        name = "BTC"
    },
    Coin().apply {
        chain = "ETH"
        name = "YCC"
    },
    Coin().apply {
        chain = "DCR"
        name = "DCR"
    }
)

