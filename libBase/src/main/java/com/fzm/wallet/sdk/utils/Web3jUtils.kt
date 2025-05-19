package com.fzm.wallet.sdk.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.fzm.wallet.sdk.base.logDebug
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Optional


class Web3jUtils {

    companion object {
        //erc20 usdt 合约地址
        const val CONTRACT_ADDRESS_ERC20USDT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        const val CONTRACT_ADDRESS_BEP20USDT = "0x55d398326f99059ff775485246999027b3197955"

        //第三方服务 高频率请求时。或者是否有公共节点可用，比如默认的trongrid不需要密钥，但可能有速率限制。
        //https://api.trongrid.io/v1/accounts/{address}
        fun sendBtyTransaction(
            privateKey: String,
            toAddress: String,
            amount: BigDecimal
        ) {
            val chainId: Long = 2999
            val web3j = Web3j.build(HttpService(GoWallet.WEB3_BTY))
            val credentials = Credentials.create(privateKey)
            val count =
                web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST)
                    .send()
            val nonce = count.transactionCount
            val gasPrice = Convert.toWei("20", Convert.Unit.GWEI).toBigInteger()
            val gasLimit = BigInteger.valueOf(21000)
            val value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger()
            val rawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, toAddress, value
            )
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
            val hexValue = Numeric.toHexString(signedMessage)
            logDebug("签名后的数据 $hexValue")
            val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()
            if (ethSendTransaction.hasError()) {
                println("发送 ETH 交易时出错: ${ethSendTransaction.error.message}")
            } else {
                println("ETH 交易哈希: ${ethSendTransaction.transactionHash}")
            }

        }


        fun sendErc20USDTTransaction(
            privateKey: String,
            toAddress: String,
            amount: BigInteger
        ) {
            //erc20 usdt 合约地址
            val contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
            val web3j = Web3j.build(HttpService(GoWallet.WEB3_ETH))
            val credentials = Credentials.create(privateKey)

            val count = web3j.ethGetTransactionCount(
                credentials.address, DefaultBlockParameterName.LATEST
            ).send()
            val nonce = count.transactionCount
            val gasPrice = Convert.toWei("20", Convert.Unit.GWEI).toBigInteger()
            val gasLimit = BigInteger.valueOf(200000)

            val function = Function(
                "transfer",
                listOf(Address(toAddress), Uint256(amount)),
                emptyList()
            )
            val data = FunctionEncoder.encode(function)

            val rawTransaction = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, contractAddress, data
            )

            val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
            val hexValue = Numeric.toHexString(signedMessage)

            val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()
            if (ethSendTransaction.hasError()) {
                println("发送 ERC - 20 代币交易时出错: ${ethSendTransaction.error.message}")
            } else {
                println("ERC - 20 代币交易哈希: ${ethSendTransaction.transactionHash}")
            }

        }

        fun getCoinBalance(url: String, address: String) {
            Thread {
                val web3j = Web3j.build(HttpService(url))
                val ethBalance = web3j.ethGetBalance(
                    address,
                    DefaultBlockParameterName.LATEST
                ).send()

                val balance = Convert.fromWei(ethBalance.balance.toBigDecimal(), Convert.Unit.ETHER)
                logDebug("url = $url ba = $balance")
            }.start()


        }

        fun getContractBalance(url: String, address: String, contractAddress: String) {
            Thread {
                val web3j = Web3j.build(HttpService(url))
                // 构造 balanceOf 函数调用
                val function = Function(
                    "balanceOf",
                    listOf(Address(address)),
                    listOf(object : TypeReference<Uint256>() {})
                )
                val encodedFunction = FunctionEncoder.encode(function)

                try {
                    // 调用合约方法
                    val response = web3j.ethCall(
                        org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            address,
                            contractAddress,
                            encodedFunction
                        ),
                        DefaultBlockParameterName.LATEST
                    ).send()

                    if (!response.hasError()) {
                        val result = response.value
                        val decoded =
                            FunctionReturnDecoder.decode(result, function.outputParameters)
                        if (decoded.isNotEmpty()) {
                            val ban = (decoded[0] as Uint256).value
                            val balance = Convert.fromWei(ban.toBigDecimal(), Convert.Unit.ETHER)
                            logDebug("usdt  url = $url ba = $balance")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

        }

        // 获取最新区块的交易列表
        fun getBlock(url: String) {
            val web3j = Web3j.build(HttpService(url))
            val txs = web3j.ethGetBlockByNumber(
                DefaultBlockParameterName.LATEST, true
            ).send().block.transactions

            // 遍历交易并提取详细信息
            for (tx in txs) {
                val transaction = tx.get() as EthBlock.TransactionObject
                println("From: " + transaction.from);
                println("To: " + transaction.to);
                println("Value: " + transaction.value);
            }
        }

        //可以指定区块号（如最新区块的前3个块）获取更早的交易记录：
        fun getTranForBlock(url: String){
            val web3j = Web3j.build(HttpService(url))
            val latestBlock: BigInteger = web3j.ethBlockNumber().send().blockNumber
            val block: EthBlock = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(latestBlock.subtract(BigInteger.valueOf(3))), true
            ).send()
            val transactions = block.block.transactions
        }

        //通过交易哈希查询单笔交易
        @RequiresApi(Build.VERSION_CODES.N)
        fun getTranByHash(url: String){
            val web3j = Web3j.build(HttpService(url))
            val transaction: Optional<Transaction> = web3j.ethGetTransactionByHash("0x交易哈希")
                .send().transaction
            if (transaction.isPresent) {
                System.out.println("Gas Price: " + transaction.get().getGasPrice())
                System.out.println("Input Data: " + transaction.get().getInput())
            }
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getTranState(url: String){
            val web3j = Web3j.build(HttpService(url))
            val receipt: EthGetTransactionReceipt =
                web3j.ethGetTransactionReceipt("0x交易哈希").send()
            if (receipt.transactionReceipt.isPresent) {
                val result = receipt.result
                //交易回执中的status字段为十六进制值，需转换为十进制判断状态（如0x1表示成功）
                println("Status: " + result.status) // 0x1表示成功
                println("Gas Used: " + result.gasUsed)
            }
        }

        private fun getTranByAddress(web3j: Web3j,
                                     targetAddress: String,
                                     startBlock: Int,
                                     endBlock: Int
        ): List<EthBlock.TransactionResult<Any>>{
            val transactions = mutableListOf<EthBlock.TransactionResult<Any>>()

            // 遍历指定范围内的区块
            for (blockNumber in startBlock..endBlock) {
                val block = web3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(blockNumber.toBigInteger()),
                    true
                ).send()

                for (txResult in block.block?.transactions!!){
                    val tx = (txResult.get() as EthBlock.TransactionObject)
                    // 检查发送方或接收方是否匹配目标地址
                    if (tx.from.equals(targetAddress, ignoreCase = true) ||
                        tx.to.equals(targetAddress, ignoreCase = true)) {
                        transactions.add(txResult)
                    }
                }
            }
            return transactions
        }

        fun doGetTrans(url: String,targetAddress:String){
            val web3j = Web3j.build(HttpService(url))
            val latestBlock = web3j.ethBlockNumber().send().blockNumber
            val startBlock = latestBlock.subtract(BigInteger.valueOf(100)) // 查询最近100个区块

            val transactions = getTranByAddress(web3j, targetAddress, startBlock.toInt(), latestBlock.toInt())
            for (tx in transactions){
                val txObj = tx.get() as EthBlock.TransactionObject
                println("交易哈希: ${txObj.hash}")
                println("发送方: ${txObj.from}, 接收方: ${txObj.to}, 金额: ${txObj.value}")
            }
        }


    }
}