package com.y.et.model

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import java.util.Date

class Wallet {
    var account: String = ""

    @Size(max = 60) @NotNull
    var userId: String = ""

    @Size(max = 255) @NotNull
    var description: String = ""

    @NotNull
    var createdAt = Date()

    constructor()

    constructor(account: String) {
        this.account = account
    }
}
