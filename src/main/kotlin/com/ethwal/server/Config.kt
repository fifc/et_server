package com.ethwal.server

import com.ethwal.server.api.GetMarketPriceResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
object Config {
    // network
    const val network = "rinkeby"
    // 是否全节点模式
    const val isFullNode = true
    //keystore目录,建议绝对路径
    const val keystoreDir = "C:\\u\\keystore"
    //const val keystoreDir = "/opt/keystore"

    // ipc/rpc 配置
    //const val web3jUrl = "/home/etherum/rinkeby/geth.ipc"
    //const val web3jUrl = "\\\\.\\pipe\\geth.ipc"
    const val web3jUrl = "http://eth.gboot.cc:8000"

    // url of balance query api
    val uriBase = when (network) {
        "kovan" -> "https://api-kovan.etherscan.io/api?module=account&action=balance"
        "rinkeby" -> "https://api-rinkeby.etherscan.io/api?module=account&action=balance"
        "ropsten" -> "https://api-ropsten.etherscan.io/api?module=account&action=balance"
        else -> "https://api.etherscan.io/api?module=account&action=balance"
    }

    // 鉴权配置
    const val enableHmac = false
    val authMap = mapOf(  // 公钥 => （私钥，权限）
            "." to Pair("09b5e75488c5690d8174ca714142a18793b9ef49f85104e6e89c774b63a4d525", 0xffffffffL),  // 测试账户, 全部权限
            "e8872fcdcf91dfdf3345efc7c82a309270323583" to Pair("11712347aef7e0d2079ecd63de587cee8eef7b2d4cf91f2fb25aae8a1db1dfc3", 0x0001L), // 查询
            "61341d0bb164d0658e573badf25ad56ab5771811" to Pair("6dc9257f02dc9732f21aedb9efbbcfe789ad40b180c2ff2badf36f7e5be91ce7", 0x0003L), // 查询，开户
            "becac26346d9711e39bddc87acc699997ddc7ff8" to Pair("09b5e75488c5690d8174ca714142a18793b9ef49f85104e6e89c774b63a4d525", 0x000fL)  // 查询，开户，转账，系统
    )

    // 是否具有查询权限
    fun canQuery(key: String): Boolean {
        val auth = this.authMap[key]?.second?:0L
        return (auth and 0x01L) != 0L
    }

    // 是否具有开户权限
    fun canOpenAccount(key: String): Boolean {
        val auth = this.authMap[key]?.second?:0L
        return (auth and 0x02L) != 0L
    }

    // 是否具有转账权限
    fun canDoTrans(key: String): Boolean {
        val auth = this.authMap[key]?.second?:0L
        return (auth and 0x04L) != 0L
    }

    // 是否具有转账权限
    fun canDoAdmin(key: String): Boolean {
        val auth = this.authMap[key]?.second?:0L
        return (auth and 0x08L) != 0L
    }

    const val etherscanKey = "BMI7CIXTWWYI99F9QVV41EMH85W9R9VYUR"

    const val priceUpdatePeriod = 15 // 行情刷新时间，秒

}