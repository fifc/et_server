package com.ethwal.server.api

class GetMarketPriceResponse {
    var usd = ""  // price in usd
    var btc = ""  // price in btc
    var time = 0L // 报价时间
    // common params
    var result = "OK" // "OK": success, otherwise contain the error code, such as "AUTH_FAIL", ...
    var id: String? = null  // request for id
    var msg: String? = null // description message
}