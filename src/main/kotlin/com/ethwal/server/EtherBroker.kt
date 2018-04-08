package com.ethwal.server

import org.web3j.protocol.Web3j
import org.web3j.protocol.admin.Admin
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.infura.InfuraHttpService
import org.web3j.protocol.ipc.UnixIpcService
import org.web3j.protocol.ipc.WindowsIpcService
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono


object EtherBroker {
    var broker = when {
        Config.web3jUrl.startsWith("http://", true) || Config.web3jUrl.startsWith("https://", true) -> {
            val url = Config.web3jUrl
            if (Config.useInfura)
                Web3j.build(InfuraHttpService(url))
            else
                Web3j.build(HttpService(url))
        }
        System.getProperty("os.name").startsWith("Windows", true) -> Web3j.build(WindowsIpcService(Config.web3jUrl))
        else -> Web3j.build( UnixIpcService(Config.web3jUrl))
    }

    var admin = when {
        Config.useInfura -> null
        Config.web3jUrl.startsWith("http")-> Admin.build(HttpService(Config.web3jUrl))
        System.getProperty("os.name").startsWith("Windows") -> Admin.build(WindowsIpcService(Config.web3jUrl))
        else -> Admin.build( UnixIpcService(Config.web3jUrl))
    }

    fun getBalance(address: String): Mono<String> {
        return broker.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().toMono().map {
            if (it.hasError())
                ""
            else
                it.balance.toString()
        }.onErrorResume {
            Mono.just("")
        }
    }
}