package com.fzm.walletdemo.web3.bean;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;


public class Web3Transaction
{
    public final Address recipient;
    public final Address contract;
    public final Address sender;
    public final BigInteger value;
    public final BigInteger gasPrice;
    public final BigInteger gasLimit;

    // EIP1559
    public BigInteger maxFeePerGas;
    public BigInteger maxPriorityFeePerGas;

    public final long nonce;
    public final String payload;
    public final long leafPosition;
    public final String description;

    public Web3Transaction(
            Address sender,
            Address contract,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            String payload)
    {
        this.recipient = contract;
        this.sender = sender;
        this.contract = contract;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = 0;
        this.description = null;
        this.maxPriorityFeePerGas = BigInteger.ZERO;
        this.maxFeePerGas = BigInteger.ZERO;
    }

    public Web3Transaction(
            Address recipient,
            Address contract,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            String payload,
            String description)
    {
        this.sender = null;
        this.recipient = recipient;
        this.contract = contract;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = 0;
        this.description = description;
        this.maxFeePerGas = BigInteger.ZERO;
        this.maxPriorityFeePerGas = BigInteger.ZERO;
    }

    public Web3Transaction(
            Address recipient,
            Address contract,
            BigInteger value,
            BigInteger maxFee,
            BigInteger maxPriorityFee,
            BigInteger gasLimit,
            long nonce,
            String payload,
            String description)
    {
        this.recipient = recipient;
        this.contract = contract;
        this.sender = null;
        this.value = value;
        this.gasPrice = BigInteger.ZERO;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = 0;
        this.description = description;
        this.maxFeePerGas = maxFee;
        this.maxPriorityFeePerGas = maxPriorityFee;
    }

    public Web3Transaction(
            Address recipient,
            Address contract,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            String payload,
            long leafPosition)
    {
        this.recipient = recipient;
        this.contract = contract;
        this.sender = null;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = leafPosition;
        this.description = null;
        this.maxFeePerGas = BigInteger.ZERO;
        this.maxPriorityFeePerGas = BigInteger.ZERO;
    }

    public Transaction getWeb3jTransaction(String walletAddress, long nonce)
    {
        return new Transaction(
                walletAddress,
                BigInteger.valueOf(nonce),
                gasPrice,
                gasLimit,
                recipient.toString(),
                value,
                payload);
    }

    public boolean isLegacyTransaction()
    {
        return !gasPrice.equals(BigInteger.ZERO) || maxFeePerGas.compareTo(BigInteger.ZERO) <= 0;
    }

}
