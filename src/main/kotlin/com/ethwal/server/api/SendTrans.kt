package com.ethwal.server.api

class SendTrans {
    var account: String = ""   // ethereum account
    var password: String = ""  // account password
    var to: String = ""        // dest account
    var value: String = ""     // amount of ether to transfer
    var gas: String = ""      // 交易费
    var gasLimit: String = "" // gas限额
    // common params
    var nonce: Long = 0L // random number
    var key: String? = null   // auth key
    var id: String? = null   // request id
}