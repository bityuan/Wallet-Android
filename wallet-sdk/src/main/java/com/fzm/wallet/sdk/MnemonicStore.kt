package com.fzm.wallet.sdk

/**
 * @author zhengjy
 * @since 2022/02/21
 * Description:
 */
interface MnemonicStore {

    /**
     * 获取助记词
     */
    suspend fun getMnemonicWords(password: String): String

    /**
     * 保存助记词
     */
    suspend fun saveMnemonicWords(mnemonic: String, password: String): Boolean

    suspend fun checkPassword(password: String): Boolean

    fun hasPassword(): Boolean
}