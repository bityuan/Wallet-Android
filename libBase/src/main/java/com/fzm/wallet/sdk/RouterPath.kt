package com.fzm.wallet.sdk

object RouterPath {
    //nft
    const val NFT_TRAN_DETAIL = "/nft/NFTTranDetailsActivity"
    const val NFT_TRAN = "/nft/NFTTranActivity"
    const val NFT_OUT = "/nft/NFTOutActivity"
    const val NFT_IN = "/nft/NFTInActivity"

    //wallet
    const val WALLET_CREATE_WALLET = "/wallet/CreateWalletActivity"
    const val WALLET_CREATE_MNEM = "/wallet/CreateMnemActivity"
    const val WALLET_CREATE_MNEM_TIP = "/wallet/CreateMnemTipActivity"
    const val WALLET_OUT = "/wallet/OutActivity"
    const val WALLET_HISTORY = "/wallet/HistoryActivity"
    const val WALLET_IN = "/wallet/InActivity"
    const val WALLET_TRANSACTIONS = "/wallet/TransactionsActivity"
    const val WALLET_TRANSACTION_DETAILS = "/wallet/TransactionDetailsActivity"
    const val WALLET_CHOOSE_CHAIN = "/wallet/ChooseChainActivity"
    const val WALLET_WALLET_DETAILS = "/wallet/WalletDetailsActivity"
    const val WALLET_CHECK_MNEM = "/wallet/CheckMnemActivity"
    const val WALLET_SET_PASSWORD = "/wallet/SetPasswordActivity"
    const val WALLET_CHANGE_PASSWORD = "/wallet/ChangePasswordActivity"
    const val WALLET_CAPTURE = "/wallet/CaptureCustomActivity"
    const val WALLET_BACKUP_WALLET = "/wallet/BackUpWalletActivity"
    const val WALLET_NEW_RECOVER_ADDRESS = "/wallet/NewRecoverAddressActivity"
    const val WALLET_RECOVER = "/wallet/RecoverActivity"
    const val WALLET_CHECKEMAIL = "/wallet/CheckEmailActivity"
    const val WALLET_CONTACTS = "/wallet/ContactsActivity"
    const val WALLET_UPDATE_CONTACTS = "/wallet/UpdateContactsActivity"
    const val WALLET_CONTACTS_DETAILS = "/wallet/ContactsDetailsActivity"
    const val WALLET_IMPORTWALLET = "/wallet/ImportWalletActivity"

    //app
    const val APP_SPLASH = "/app/SplashActivity"
    const val APP_MAIN = "/app/MainActivity"
    const val APP_DAPP = "/app/DappActivity"
    const val APP_SEARCH_DAPP = "/app/SearchDappActivity"
    const val APP_MESSAGES = "/app/MessagesActivity"
    const val APP_MsgDetails = "/app/MsgDetailsActivity"
    const val APP_EXPLORES = "/app/ExploresActivity"
    const val APP_ABOUT = "/app/AboutActivity"
    const val APP_NODE_SETTINGS = "/app/NodeSettingsActivity"
    const val APP_RPC = "/app/RPCActivity"
    const val APP_SCAN_RESULT = "/app/ScanResultActivity"
    const val APP_DOWNLOAD = "/app/DownLoadActivity"
    const val APP_LANGUAGE = "/app/LanguageActivity"
    const val APP_WEBTEST = "/app/WebTestActivity"
    const val APP_WCONNECT = "/app/WConnectActivity"
    const val APP_SETFEE = "/app/SetFeeActivity"

    //param
    const val PARAM_COIN = "coin"
    const val PARAM_TRANSACTIONS = "transactions"
    const val PARAM_SCAN = "scan"
    const val PARAM_FROM = "from"
    const val PARAM_ADDRESS = "address"
    const val PARAM_NFT_TRAN = "nft_tran"
    const val PARAM_APPS_ID = "apps_id"
    const val PARAM_WALLET = "wallet"
    const val PARAM_VISIBLE_MNEM = "visible_mnem"
    const val PARAM_WC_URL = "wc_url"
    const val PARAM_CONTACTS_ID = "contacts_id"
    const val PARAM_FEE_POSITION = "fee_position"
    const val PARAM_CHAIN_ID = "chain_id"
    const val PARAM_ORIG_GAS = "orig_gas"
    const val PARAM_GAS = "gas"
    const val PARAM_GAS_PRICE = "gas_price"
    const val PARAM_ORIG_GAS_PRICE = "orig_gas_price"
    const val PARAM_URL = "url"
}