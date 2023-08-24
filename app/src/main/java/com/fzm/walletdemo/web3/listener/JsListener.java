package com.fzm.walletdemo.web3.listener;


import com.fzm.walletdemo.web3.bean.Web3Call;
import com.fzm.walletdemo.web3.bean.Web3Transaction;

/**
 * Created by JB on 15/01/2022.
 */
public interface JsListener
{
    void onRequestAccounts(long callbackId);
    void onEthCall(Web3Call txdata);
    void onSignTransaction(Web3Transaction transaction);
}
