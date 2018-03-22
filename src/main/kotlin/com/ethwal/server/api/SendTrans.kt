package com.ethwal.server.api

class SendTrans {
    var account: String = ""   // ethereum account
    var password: String = ""  // account password
    var toAccount: String = "" // dest account
    var value: String = ""  // amount of ether to transfer
    // common params
    var key: String? = null // auth key
    var id: String? = null // request for id
}