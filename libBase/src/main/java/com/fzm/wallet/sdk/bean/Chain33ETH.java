package com.fzm.wallet.sdk.bean;

public class Chain33ETH {

    private String cointype;
    private String tokenSymbol;
    private int decimal;
    private double amount;
    private double fee;
    private int chainID;
    private String execer;
    private String bridgeBankContractAddr;
    private String contractTokenAddr;
    //exchange新增
    private String coinTokenContractAddr;
    private String xgoBridgeBankContractAddr;
    private String xgoOracleAddr;

    public String getContractTokenAddr() {
        return contractTokenAddr;
    }

    public void setContractTokenAddr(String contractTokenAddr) {
        this.contractTokenAddr = contractTokenAddr;
    }

    public String getCoinTokenContractAddr() {
        return coinTokenContractAddr;
    }

    public void setCoinTokenContractAddr(String coinTokenContractAddr) {
        this.coinTokenContractAddr = coinTokenContractAddr;
    }

    public String getXgoBridgeBankContractAddr() {
        return xgoBridgeBankContractAddr;
    }

    public void setXgoBridgeBankContractAddr(String xgoBridgeBankContractAddr) {
        this.xgoBridgeBankContractAddr = xgoBridgeBankContractAddr;
    }

    public String getXgoOracleAddr() {
        return xgoOracleAddr;
    }

    public void setXgoOracleAddr(String xgoOracleAddr) {
        this.xgoOracleAddr = xgoOracleAddr;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getExecer() {
        return execer;
    }

    public void setExecer(String execer) {
        this.execer = execer;
    }

    public int getDecimal() {
        return decimal;
    }

    public void setDecimal(int decimal) {
        this.decimal = decimal;
    }

    public int getChainID() {
        return chainID;
    }

    public void setChainID(int chainID) {
        this.chainID = chainID;
    }

    public String getBridgeBankContractAddr() {
        return bridgeBankContractAddr;
    }

    public void setBridgeBankContractAddr(String bridgeBankContractAddr) {
        this.bridgeBankContractAddr = bridgeBankContractAddr;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public String getCointype() {
        return cointype;
    }

    public void setCointype(String cointype) {
        this.cointype = cointype;
    }

    public String getTokenSymbol() {
        return tokenSymbol;
    }

    public void setTokenSymbol(String tokenSymbol) {
        this.tokenSymbol = tokenSymbol;
    }
}
