package com.y.et.model

import java.util.Date

class Wallet {
    var account: String = ""

    var userId: String = ""

    var description: String = ""

    var createdAt = Date()

    constructor()

    constructor(account: String) {
        this.account = account
    }
}
