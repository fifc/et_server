package com.ethwal.server

object Config {
    const val priceUpdatePeriod = 15 // 行情刷新时间，秒

    const val isFullNode = true
    const val keystoreDir = "w3j\\keystore"
    const val isWindowsPlatform = true
    const val isUsingHttp = false
    //const val web3jUrl = "/root/geth/fast/geth.ipc"
    const val web3jUrl = "\\\\.\\pipe\\geth.ipc"

    const val etherscanKey = "BMI7CIXTWWYI99F9QVV41EMH85W9R9VYUR"

    var marketPrice = com.ethwal.server.api.GetMarketPriceResponse()

}