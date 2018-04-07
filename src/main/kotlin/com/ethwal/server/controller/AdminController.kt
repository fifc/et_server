package com.ethwal.server.controller

import com.ethwal.server.Config
import org.springframework.core.io.FileSystemResource
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Controller
@RequestMapping("/")
class AdminController {
    @GetMapping("/")
    fun homepage(model: Model): Mono<String> {
        return Mono.just("index")
    }

    @GetMapping("/testkey")
    @ResponseBody
    fun getTestKey(model: Model): Map<String, String> {
        val address = "0xbecac26346d9711e39bddc87acc699997ddc7ff8"
        val acl = Config.authMap[address.substring(2)]
        return if (acl == null)
            mapOf(Pair("ERROR", ""))
        else
            mapOf(
                    Pair("network", Config.network),
                    Pair("account", address),
                    Pair("private key", acl.first),
                    Pair("public key", address.substring(2)),
                    Pair("os", System.getProperty("os.name")),
                    Pair("cwd", System.getProperty("user.dir")),
                    Pair("node", Config.web3jUrl),
                    Pair("keystore", "internal"/*Config.keystoreDir*/),
                    Pair("clock", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            )
    }

    @GetMapping("/doc/{name}", produces = ["application/octet-stream"])
    fun streamToZip(@PathVariable("name") name: String?): Mono<ResponseEntity<FileSystemResource>> {
        if (name == null || name.isBlank()) {
            return Mono.just(ResponseEntity(HttpStatus.NOT_FOUND))
        }

        val sep = System.getProperty("file.separator")
        val file = FileSystemResource("doc$sep$name")

        return when {
            !file.exists() || !file.isFile -> {
                println("file ${file.path} not exists!")
                Mono.just(ResponseEntity(HttpStatus.NOT_FOUND))
            }
            !file.isReadable -> Mono.just(ResponseEntity(HttpStatus.FORBIDDEN))
            else -> Mono.just(ResponseEntity
                    .ok().cacheControl(CacheControl.noCache())
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=$name")
                    .body(file))
        }
    }

    @RequestMapping(
            value = ["*"],
            method = [ RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE])
    @ResponseBody
    fun allFallback(): String {
        val sec = Config.authMap["becac26346d9711e39bddc87acc699997ddc7ff8"]?.second?:-1L
        return "this is fallback for all requests - $sec"
    }
}