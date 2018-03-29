package com.ethwal.server.api

class GetTransResultResponse {
    var completed = false  // true 表示block已经生成并交由矿工处理
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