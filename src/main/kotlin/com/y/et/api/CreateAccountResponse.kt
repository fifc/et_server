package com.y.et.api

class CreateAccountResponse {
    var account = ""
    // common params
    var status = "OK" // "OK": success, otherwise contain the error code, such as "AUTH_FAIL"
    var id: String? = null  // request for id
    var msg: String? = null // description message
}
