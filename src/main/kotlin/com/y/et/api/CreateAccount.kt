package com.y.et.api

class CreateAccount {
    var userId: String = ""
    var password: String = ""
    var forceCreate  = false
    // common params
    var nonce: Long = 0  // random number
    var key: String? = null // auth key
    var id: String? = null  // request for id
}
