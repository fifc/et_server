package com.ethwal.server.api

class SendTransResponse {
    var account = ""
    // common params
    var status = "OK" // "OK": success, otherwise contain the error code, such as "AUTH_FAIL"
    var msg: String? = null // description message
    var id: String? = null  // request for id
}