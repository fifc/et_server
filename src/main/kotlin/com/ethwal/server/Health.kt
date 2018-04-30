package com.ethwal.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.experimental.sync.Mutex
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.time.ZoneOffset

data class LicenseInfo (var expire: Long = -1L, var name: String = "", var key: String = "")

object Health {
    var checkTime = 0L
    var license = LicenseInfo()
    lateinit var appContext: ApplicationContext
    private val lock = Mutex()
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

    private fun licenseExpired(): Boolean  {
        val mapper = ObjectMapper(YAMLFactory())
        if (license.expire == 0L)
            return false
        val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (license.expire == -1L || license.expire <= now) {
            try {
                license = mapper.readValue<LicenseInfo>("{ expire: 1526111000, name: ultimate_edition, key: foojsd997093ss98a }")
            } catch (e: FileNotFoundException) {

            } catch (e: Exception) {

            }
        }

        if (license.expire == 0L)
            return false

        if (license.expire == -1L || license.expire <= now) {
            // expired
            //return true
            return false
        }

        if (license.expire <= now + 3600 * 24 * 30) {
            val days = (license.expire - now) / 3600 / 24
            println("\n\n!!!!!!!!!!!!!!!!!!!!!!!! License Will Expire in $days Day(s)!!!\n\n")
        }

        return false
    }
}