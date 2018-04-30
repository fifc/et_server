package com.ethwal.server

import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.locks.ReentrantLock

object Health {
    var checkTime = 0L
    lateinit var appContext: ApplicationContext
    val lock = ReentrantLock()
    fun assertInherentition() {
        val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (timestamp < checkTime) {
            if (timestamp + 3600 * 12 >= checkTime)
                return
        }

        if (lock.tryLock()) {
            checkTime = timestamp + 3600
            if (licenseExpired()) {
                println("license expired! exiting ...")
                SpringApplication.exit(appContext, ExitCodeGenerator { 0 })
            } else {
                println("inherent assert success!")
            }
        }
    }

    private fun licenseExpired(): Boolean = false
}