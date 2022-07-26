package com.fzm.wallet.sdk

object RouterPath {
    //nft
    const val NFT_TRAN_DETAIL = "/nft/NFTTranDetailsActivity"
    const val NFT_TRAN = "/nft/NFTTranActivity"
    const val NFT_OUT = "/nft/NFTOutActivity"
    const val NFT_IN = "/nft/NFTInActivity"
    //wallet
    const val WALLET_OUT = "/wallet/OutActivity"
    const val WALLET_IN = "/wallet/InActivity"
    const val WALLET_CHOOSE_CHAIN = "/wallet/ChooseChainActivity"
    const val WALLET_WALLET_DETAILS = "/wallet/WalletDetailsActivity"
    const val WALLET_CHECK_MNEM = "/wallet/CheckMnemActivity"
    const val WALLET_SET_PASSWORD = "/wallet/SetPasswordActivity"
    const val WALLET_CHANGE_PASSWORD = "/wallet/ChangePasswordActivity"
    const val WALLET_CAPTURE = "/wallet/CaptureCustomActivity"
    //app
    const val APP_SPLASH = "/app/SplashActivity"
    const val APP_MAIN = "/app/MainActivity"
    const val APP_DAPP = "/app/DappActivity"
    const val APP_ABOUT = "/app/AboutActivity"
    const val APP_DOWNLOAD = "/app/DownLoadActivity"

    const val PARAM_COIN = "coin"
    const val PARAM_NFT_TRAN = "nft_tran"
}