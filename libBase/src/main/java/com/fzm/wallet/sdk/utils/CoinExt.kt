package com.fzm.wallet.sdk.utils

import com.fzm.wallet.sdk.db.entity.Coin

/**
 * @author zhengjy
 * @since 2022/01/14
 * Description:
 */

inline val Coin.totalAsset: Double get() = rmb * balance.toDouble()

inline val Coin.isBty: Boolean get() = chain == "BTY"

inline val Coin.isBtyChild: Boolean get() = isBty && platform != "bty"

inline val Coin.isBtyCoins: Boolean get() = isBtyChild && isCoin

inline val Coin.isBtyToken: Boolean get() = isBtyChild && !isCoin

/**
 * 判断是coins还是token
 */
inline val Coin.isCoin: Boolean get() = if (isBtyChild) treaty == "2" else name == chain

/**
 * 查询交易记录时使用的tokenSymbol
 */
val Coin.tokenSymbol: String
    get() {
        return when {
            isBtyCoins -> "$platform.coins"
            isBtyToken -> "$platform.$name"
            else -> name
        }
    }

/**
 * 币种精度
 */
inline val Coin.decimalPlaces: Int get() = 4
    /*get() {
        return if (balance.contains(".")) {
            val array = balance.split(".")
            if (array.size == 2) array[1].length else 0
        } else {
            0
        }
    }*/

/**
 * 币种唯一表识符
 */
inline val Coin.uid: String get() = "$chain-$name-$platform"

/**
 * 完整链名
 */
inline val Coin.fullChain: String get() = if (isBtyChild) "user.p.$platform" else chain

/**
 * 红包等需要的执行器
 */
inline val Coin.fullExec: String get() = if (isCoin) "$fullChain.coins" else "$fullChain.token"

/**
 * 红包等需要的执行器
 */
inline val Coin.assetExec: String get() = if (isCoin) "coins" else "token"