package com.ethwal.server.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import java.util.Date

@Document(collection = "wall")
class Wallet {
    @Id @Size(max = 60)
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
