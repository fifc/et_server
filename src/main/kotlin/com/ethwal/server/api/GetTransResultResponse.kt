package com.ethwal.server.api

class GetTransResultResponse {
    var confirmed = false  // true 表示交易已得到确认
    var from = ""
    var to = ""
    var value = ""
    var gas = ""
    var hash = ""
    // common params
    var status = "OK" // "OK": submit success, otherwise contain the error code, such as "AUTH_FAIL"
    var msg: String? = null // description message
    var id: String? = null  // request for id
}