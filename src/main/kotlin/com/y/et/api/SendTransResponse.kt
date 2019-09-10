package com.y.et.api

class SendTransResponse {
    var account = ""
    var to = ""
    var value = ""
    var gas = ""
    var hash = ""
    // common params
    var status = "OK" // "OK": submit success, otherwise contain the error code, such as "AUTH_FAIL"
    var msg: String? = null // description message
    var id: String? = null  // request for id
}
