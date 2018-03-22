package com.ethwal.server.api

class SendTransResponse {
    var account = ""
    // common params
    var result = "OK" // "OK": success, otherwise contain the error code, such as "AUTH_FAIL"
    var id: String? = null  // request for id
    var msg: String? = null // description message
}