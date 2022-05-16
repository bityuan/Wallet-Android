package com.fzm.wallet.sdk

import com.fzm.wallet.sdk.db.entity.PWallet
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2022/01/20
 * Description:
 */
data class WalletBean(
    /**
     * 钱包id
     */
    val id: Long,
    /**
     * 钱包名称
     */
    val name: String,
    /**
     * 钱包所属用户
     */
    val user: String,
    /**
     * 钱包类型
     * 2：普通钱包
     */
    val type: Int,
): Serializable

internal fun PWallet.toWalletBean(): WalletBean {
    return WalletBean(id, name, user, type)
}