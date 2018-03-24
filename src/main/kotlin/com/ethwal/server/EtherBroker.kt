package com.ethwal.server

import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.ipc.UnixIpcService
import org.web3j.protocol.ipc.WindowsIpcService


object EtherBroker {
    var broker =  if (Config.isUsingHttp)
        Web3j.build(HttpService(Config.web3jUrl))
    else if (Config.isWindowsPlatform)
        Web3j.build(WindowsIpcService(Config.web3jUrl))
    else
        Web3j.build( UnixIpcService(Config.web3jUrl))

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