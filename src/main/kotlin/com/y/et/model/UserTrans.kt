package com.y.et.model

import java.util.Date

data class UserTrans (
        val user: String = "", // user id
        val req: String = "", // request id
        val from: String = "",
        val to: String = "",
        val value: String = "",
        val time: Date = Date(),
        val hash: String = "",  // ethereum transaction hash
        val gas: String = "", // transaction gas(fee)
        var status: String = ""
)
