package com.ethwal.server

import org.web3j.protocol.Web3j
import org.web3j.protocol.admin.Admin
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.infura.InfuraHttpService
import org.web3j.protocol.ipc.UnixIpcService
import org.web3j.protocol.ipc.WindowsIpcService


object EtherBroker {
    var broker = when {
        Config.web3jUrl.startsWith("http") -> {
            if (Config.useInfura)
                Web3j.build(InfuraHttpService(Config.web3jUrl))
            else
                Web3j.build(HttpService(Config.web3jUrl))
        }
        System.getProperty("os.name").startsWith("Windows") -> Web3j.build(WindowsIpcService(Config.web3jUrl))
        else -> Web3j.build( UnixIpcService(Config.web3jUrl))
    }

    var admin = when {
        Config.web3jUrl.startsWith("http")-> Admin.build(HttpService(Config.web3jUrl))
        System.getProperty("os.name").startsWith("Windows") -> Admin.build(WindowsIpcService(Config.web3jUrl))
        else -> Admin.build( UnixIpcService(Config.web3jUrl))
    }

    fun getBalance(address: String): String {
        return try {
            var response = broker.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get()
            if (response.hasError())
                """"""
            else
                response.balance.toString()
        } catch (e: Exception) {
            println(e)
            """"""
        }
    }
}