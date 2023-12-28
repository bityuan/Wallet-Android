package com.fzm.wallet.sdk.bean;

import java.util.List;

public class TxTotal {
private String totalamount;
private List<Transactions> txs;

    public String getTotalamount() {
        return totalamount;
    }

    public void setTotalamount(String totalamount) {
        this.totalamount = totalamount;
    }

    public List<Transactions> getTxs() {
        return txs;
    }

    public void setTxs(List<Transactions> txs) {
        this.txs = txs;
    }
}
