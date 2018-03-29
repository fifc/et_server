package com.ethwal.server

import com.google.common.hash.Hashing
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@SpringBootApplication
class ServerApplication {
    var startTime = LocalDateTime.now()
    constructor() {
        startTime = LocalDateTime.now()
    }
    //init {
    //    val signString = Hashing.hmacSha256("key".toByteArray()).hashString("hello", StandardCharsets.UTF_8).toString()
    //    println("hmac('key', 'hello') = [$signString]")
    //}
}

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}
