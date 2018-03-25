package com.ethwal.server.api

// 请求参数采用url传递， /get_balance/{ether_account}

class GetBalanceResponse {
    var account = ""
    var balance = ""
    var status = ""
    var msg: String? = null // description message
}