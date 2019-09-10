package com.y.et.controller

import com.y.et.*
import  com.y.et.api.*
import com.google.common.hash.Hashing
import org.apache.commons.logging.LogFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import javax.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.function.client.WebClient
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthSyncing
import org.web3j.protocol.core.methods.response.EthTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.Exception

@RestController
//@RequestMapping("/0")
class WalletController {

    private val LOG = LogFactory.getLog(WalletController::class.java)

    // 创建/绑定以太坊账户
    @PostMapping("/create_account")
    fun createAccount(@Valid @RequestBody request: CreateAccount, @RequestParam("sign") sign: String?) : Mono<CreateAccountResponse> {
        LOG.info("CreateAccount: user ${request.userId} key ${request.key} sign $sign")
        var response = CreateAccountResponse()
        response.id = request.id
        if (request.password.isNullOrBlank() || request.password.length < 6) {
            response.status = "INVALID_PASSWORD"
            return Mono.just(response)
        }
        if (request.userId.isNullOrBlank()) {
            response.status = "INVALID_USERID"
            return Mono.just(response)
        }

        if (request.key == null || !Config.canOpenAccount(request.key!!)) {
             // ignore privilege checking for now
        }

        Health.assertInherentition()

        return Account.newAsync(request.password).map {
            response.status = "OK"
            response.account = it
            LOG.info("user ${request.userId} account ${response.account}")
            response
        }.onErrorResume {
            response.status = "CREATE_ACCOUNT_ERROR"
            response.msg = it.message.toString()
            LOG.info(it)
            //Mono.error(it)
            Mono.just(response)
        }
    }

    // 权限校验，设置默认返回值
    fun checkTransAuth(request: SendTrans, sign: String?): SendTransResponse {
        var response = SendTransResponse()
        response.id = request.id
        val key = request.key
        if (key == null || key.isNullOrBlank()) {
            response.status = "INVALID_KEY"
            return response
        }

        // 检查转账权限
        val auth = Config.authMap[key]
        if (auth == null || !Config.canDoTrans(key)) {
            response.status = "AUTH_FAIL"
            return response
        }

        if (request.password.isNullOrBlank()) {
            response.status = "INVALID_PASSWORD"
            return response
        }

        if (request.account.isNullOrBlank()) {
            response.status = "INVALID_ACCOUNT"
            return response
        }

        if (request.to.isNullOrBlank()) {
            response.status = "INVALID_DEST_ACCOUNT"
            return response
        }

        if (request.account == request.to) {
            response.status = "INVALID_DEST_ACCOUNT"
            return response
        }

         // 签名校验
        if (auth.second != 0xffffffffL && Config.enableHmac) {
            val text = "${request.account}&${request.to}&${request.value}&${request.nonce}}"
            val signString = Hashing.hmacSha256(auth.first.toByteArray()) .hashString(text, StandardCharsets.UTF_8).toString()
            if (signString != sign) {
                response.status = "SIGN_ERROR"
                return response
            }
        }

        response.account = request.account
        response.to = request.to
        response.value = request.value

        return response
    }

    // 以太币转账
    @PostMapping("/send_trans")
    fun sendTrans(@Valid @RequestBody request: SendTrans,
                  @RequestParam("sign") sign: String?) : Mono<SendTransResponse> {
        LOG.info("send_trans: id ${request.id} from ${request.account} to ${request.to} value ${request.value}")
        // 参数检查，权限验证
        var response = checkTransAuth(request, sign)
        response.msg = "请调用异步接口"
        if (!(response.status.isNullOrBlank() || response.status == "OK")) {
            LOG.info(response)
            return Mono.just(response)
        }

        val credentials = Account.loadCredentials(request.account, request.password)
        if (credentials == null) {
            response.status = "ACCOUNT_PASSWORD_ERROR"
            return Mono.just(response)
        }

        Health.assertInherentition()

        // 将交易请求提交至以太坊节点
        response.status = "FAIL"
        val transactionManager = RawTransactionManager(EtherBroker.broker, credentials)
        val transfer = Transfer(EtherBroker.broker, transactionManager)
        val gasPrice = transfer.requestCurrentGasPrice()
        val gasLimit = Transfer.GAS_LIMIT
        //return Transfer.sendFunds(EtherBroker.broker, credentials, request.to, BigDecimal(request.value), Convert.Unit.ETHER)
        return transfer.sendFunds(request.to, BigDecimal(request.value), Convert.Unit.ETHER, gasPrice, gasLimit)
                .sendAsync().toMono()
                .map {
                    // 交易被接收
                    val receipt = it.toString()
                    LOG.info("receipt: $receipt")
                    val status = it.status
                    if (status != "0x1") {
                        response.status = "ETHER_ERROR"
                        response.msg = "unknown status returned: $status"
                    } else {
                        response.status = "OK"
                        response.hash = it.transactionHash
                        checkTransactionReceipt(it, response)
                    }
                    LOG.info("send_trans: id ${request.id} status ${response.status} msg ${response.msg} hash ${response.hash}")
                    response
                }
                .onErrorResume {
                    // 发生错误
                    response.status = "ETHER_EXCEPTION"
                    response.msg = it.message
                    LOG.info("send_trans: id ${request.id} status ${response.status} msg ${response.msg}")
                    Mono.just(response)
                }
                .defaultIfEmpty (response)
    }

    // 显示交易信息，调试用途
    private fun checkTransactionReceipt(receipt: TransactionReceipt, response: SendTransResponse) {
        try {
            LOG.info("...................................................  getting block ${receipt.blockHash} .........")
            val bret = EtherBroker.broker.ethGetBlockByHash(receipt.blockHash, true).send()
            if (bret.hasError()) {
                LOG.info(">>>>>>>>>>>>>>>>>> hash: ${receipt.blockHash} error = ${bret.error.code} msg = ${bret.error.message}")
            } else {
                val block = bret.block
                LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> block ${block.hash}, miner ${block.miner} trans ${block.transactions.size}")
            }
        } catch (e: Exception) {
            LOG.info("!! exception get block by hash ${receipt.blockHash}: ${e.message}")
        }

        val hash = receipt.transactionHash
        try {
            LOG.info("...................................................  getting transaction $hash .........")
            val ret = EtherBroker.broker.ethGetTransactionByHash(hash).send()
            if (ret.hasError()) {
                LOG.info(">>>>>>>>>>>>>>>>>> transaction: $hash error = ${ret.error.code} msg = ${ret.error.message}")
            } else {
                if (!ret.transaction.isPresent) {
                    LOG.info(">>>>>>>>>>>>>>>>>>  transaction: $hash trans = <not presented>")
                } else {
                    val trans = ret.transaction.get()
                    LOG.info(">>>>>>>>>>>>>>>>>>  transaction $hash block: ${trans.blockHash} value = ${trans.value} gas = ${trans.gas} gasPrice = ${trans.gasPrice}")
                    var gas = trans.gas.multiply(trans.gasPrice).toBigDecimal()
                    response.gas = Convert.fromWei(gas, Convert.Unit.ETHER).toString()
                    response.value = Convert.fromWei(trans.value.toBigDecimal(), Convert.Unit.ETHER).toString()
                }
            }
        } catch (e: Exception) {
            LOG.info("!! exception get transaction by hash $hash: ${e.message}")
        }
    }

    // 以太币转账 - 不等待结果，由调用端通过批处理查询结果
    @PostMapping("/trans_async")
    fun transAsync(@Valid @RequestBody request: SendTrans,
                  @RequestParam("sign") sign: String?) : Mono<SendTransResponse> {
        LOG.info("trans_async: id ${request.id} from ${request.account} to ${request.to} value ${request.value}")
        // 参数检查，权限验证
        var response = checkTransAuth(request, sign)
        if (!(response.status.isNullOrBlank() || response.status == "OK")) {
            LOG.info(response)
            return Mono.just(response)
        }

        Health.assertInherentition()

        response.status = "ACCOUNT_PASSWORD_ERROR"
        return Account.checkoutCredentials(request.account, request.password).flatMap {
            // 将交易请求提交至以太坊节点
            response.status = "FAIL"
            val transactionManager = RawTransactionManager(EtherBroker.broker, it)
            val transfer = Transfer(EtherBroker.broker, transactionManager)
            val gasPrice = transfer.requestCurrentGasPrice()
            val gasLimit = Transfer.GAS_LIMIT
            //return Transfer.sendFunds(EtherBroker.broker, credentials, request.to, BigDecimal(request.value), Convert.Unit.ETHER)
            transfer.sendFunds(request.to, BigDecimal(request.value), Convert.Unit.ETHER, gasPrice, gasLimit)
                        .sendAsync().toMono()
                    .map {
                        // 交易被接收
                        val receipt = it.toString()
                        LOG.info("receipt: $receipt")
                        val status = it.status
                        if (status != "0x1") {
                            response.status = "ETHER_ERROR"
                            response.msg = "unknown status returned: $status"
                        } else {
                            response.status = "OK"
                            response.hash = it.transactionHash
                            checkTransactionReceipt(it, response)
                        }
                        LOG.info("trans_async: id ${request.id} status ${response.status} msg ${response.msg} hash ${response.hash}")
                        response
                    }
                    .onErrorResume {
                        // 发生错误
                        response.status = "ETHER_EXCEPTION"
                        response.msg = it.message

                        var errType: String = when (it) {
                            is Exception /*TransactionTimedOutException*/ -> {
                                "timeout"
                            }
                            else -> {
                                "exception"
                            }
                        }

                        LOG.info("trans_async $errType: id ${request.id} status ${response.status} msg ${response.msg}")

                        Mono.just(response)
                    }
                    .defaultIfEmpty (response)
        }.onErrorResume {
            response.msg = it.message
            Mono.just(response)
        }.defaultIfEmpty(response)
    }

    // 查询交易确认信息
    @GetMapping("/result/{hash}")
    fun getTransResult(@PathVariable(value = "hash") hash: String,
                      @RequestParam("key") key: String?): Mono<ResponseEntity<GetTransResultResponse>> {
        if (key == null || !Config.canQuery(key)) {
            // privilege checking disabled for now
        }

        Health.assertInherentition()

        return EtherBroker.broker.ethGetTransactionByHash(hash).sendAsync().toMono().flatMap {
            var response = GetTransResultResponse()

            when {
                it.hasError() -> {
                    response.status = "ERROR"
                    response.msg = it.error.message
                    LOG.info("error get transaction result: (${response.status} $hash ${response.msg})")
                    Mono.just(ResponseEntity(response, HttpStatus.OK))
                }
                else -> if (!it.transaction.isPresent) {
                    response.status = "NOT_FOUND"
                    response.msg = "transaction not found"
                    LOG.info("error get transaction result: (${response.status} $hash ${response.msg})")
                    Mono.just(ResponseEntity(response, HttpStatus.OK))
                } else {
                    response.status = "OK"
                    val trans = it.transaction.get()
                    response.hash = trans.hash
                    response.from = trans.from
                    response.to = trans.to
                    response.value = Convert.fromWei(trans.value.toBigDecimal(), Convert.Unit.ETHER).toString()
                    var gas = trans.gas.multiply(trans.gasPrice).toBigDecimal()
                    response.gas = Convert.fromWei(gas, Convert.Unit.ETHER).toString()
                    EtherBroker.broker.ethBlockNumber().sendAsync().toMono().map {
                        if (it.hasError()) {
                            response.status = "ERROR"
                            response.msg = "failure get latest block number"
                        } else {
                            val confirm = it.blockNumber.subtract(trans.blockNumber).toLong()
                            response.confirmed = confirm >= 5
                            response.msg = "confirmation count = $confirm"
                        }
                        LOG.info("transaction result: (${response.status} $hash ${response.msg})")
                        ResponseEntity(response, HttpStatus.OK)
                    }
                }
            }
        }
    }

    // 查询账户余额 （从节点查询)
    @GetMapping("/balance/{account}")
    fun getBalance(@PathVariable(value = "account") account: String): Mono<ResponseEntity<GetBalanceResponse>> {
        var response = GetBalanceResponse()
        if (account.isNullOrBlank()) {
            response.status = "INVALID_ACCOUNT"
            return Mono.just(ResponseEntity(response, HttpStatus.BAD_REQUEST))
        }
        response.account = account
        return EtherBroker.getBalance(account).map {
            response.balance = if (it.isNullOrBlank()) "" else  Convert.fromWei(it.toBigDecimal(), Convert.Unit.ETHER).toString()
            response.status = if (it.isNullOrBlank()) "FAIL" else "OK"
            ResponseEntity(response, HttpStatus.OK)
        }
    }

    // 查询账户余额 （via EtherScan API)
    @GetMapping("/es_balance/{account}")
    fun getEsBalance(@PathVariable(value = "account") account: String): Mono<ResponseEntity<GetBalanceResponse>> {
        if (account.isNullOrBlank()) {
            var response = GetBalanceResponse()
            response.status = "INVALID_ACCOUNT"
            return Mono.just(ResponseEntity(response, HttpStatus.BAD_REQUEST))
        }

        val uriBase = Config.uriBase
        var webClient = WebClient.create()
        return webClient.get().uri("$uriBase&address=$account&tag=latest&apikey=${Config.etherscanKey}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .flatMap {
                    it.bodyToMono(AccountBalance::class.java)
                            .map {
                                var response = GetBalanceResponse()
                                response.account = account
                                if (it.status == "1") {
                                    response.status = "OK"
                                    response.balance = Convert.fromWei(it.result.toBigDecimal(), Convert.Unit.ETHER).toString()
                                    ResponseEntity(response, HttpStatus.OK)
                                } else {
                                    response.status = "FAIL"
                                    response.msg = "3rd party returned: ${it.message}"
                                    ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
                                }
                            }
                }
                .defaultIfEmpty(ResponseEntity(GetBalanceResponse(), HttpStatus.NOT_FOUND))
    }

    // 查询行情
    @GetMapping("/market_price")
    fun getMarketPrice(/*@Valid @RequestBody request: GetMarketPrice,*/
            @RequestParam("key") key: String?): Mono<ResponseEntity<GetMarketPriceResponse>> {
        val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (key == null || !Config.canOpenAccount(key)) {
            // ignore privilege checking for now
        }

        val latest = GetMarketPriceResponse.latest
        if (timestamp < latest.time + Config.priceUpdatePeriod) { // 10分钟内读取本地缓存
            var response = GetMarketPriceResponse()
            response.usd = latest.usd
            response.btc = latest.btc
            response.time = latest.time
            response.gasPrice = latest.gasPrice
            response.status = "OK"
            return Mono.just(ResponseEntity(response, HttpStatus.OK))
        }

        // 缓存过期，从cainmarketcap.com拉取行情
        var webClient = WebClient.create()
        return webClient.get().uri("https://api.coinmarketcap.com/v1/ticker/ethereum/?convert=CNY")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .flatMap {
                    it.bodyToMono(Array<CoinMarketPrice>::class.java)
                            .map {
                                var response = GetMarketPriceResponse()
                                if (it.isEmpty()) {
                                    response.status = "FAIL"
                                    ResponseEntity(response, HttpStatus.NOT_FOUND)
                                } else {
                                    var price = it[0]
                                    latest.time = timestamp
                                    latest.btc = price.price_btc?:""
                                    latest.usd = price.price_usd?:""
                                    response.usd = latest.usd
                                    response.btc = latest.btc
                                    response.time = latest.time
                                    var gasPrice = EtherBroker.broker.ethGasPrice().send()
                                    response.gasPrice = if (gasPrice.hasError()) "1000000000" else gasPrice.gasPrice.toString()
                                    if (!response.gasPrice.isNullOrBlank())
                                        latest.gasPrice = response.gasPrice
                                    response.status = "OK"
                                    ResponseEntity(response, HttpStatus.OK)
                                }
                            }
                }
                .defaultIfEmpty(ResponseEntity(GetMarketPriceResponse(), HttpStatus.NOT_FOUND))
    }

    // 返回区块链同步状态
    @GetMapping("/sync")
    fun getSyncStatus(@RequestParam("sign") sign: String?): Mono<ResponseEntity<EthSyncing.Result>> {
        LOG.info("signature = <$sign>")
        val admin = EtherBroker.admin
        if (admin == null) {
            var response = EthSyncing.Result()
            response.isSyncing = false
            ResponseEntity(response, HttpStatus.OK)
        }

        return if (sign.equals("test"))
            Mono.just(ResponseEntity(HttpStatus.OK))
        else
            admin!!.ethSyncing().sendAsync().toMono()
                    .map {
                        ResponseEntity(it.result, HttpStatus.OK)
                    }
                    .defaultIfEmpty(ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR))
    }

    // 返回区块信息
    @GetMapping("/block/{name}")
    fun getBlockInfo(@PathVariable(value = "name") name: String?): Mono<ResponseEntity<EthBlock.Block>> {
        return if (name == null || name.isNullOrBlank())
            EtherBroker.broker.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), true)
                        .sendAsync().toMono().map {
                        ResponseEntity(it.result, HttpStatus.OK)
                    }
        else if (name.startsWith("0x", true))
            EtherBroker.broker.ethGetBlockByHash(name,true).sendAsync().toMono().map {
                ResponseEntity(it.result, HttpStatus.OK)
            }
        else
            EtherBroker.broker.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger(name)), true)
                        .sendAsync().toMono().map {
                        ResponseEntity(it.result, HttpStatus.OK)
                    }
    }

    // 返回最新区块信息
    @GetMapping("/block")
    fun getLatestBlockInfo(): Mono<ResponseEntity<EthBlock.Block>> {
        return EtherBroker.broker.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), true)
                    .sendAsync().toMono().map {
                        ResponseEntity(it.result, HttpStatus.OK)
                    }
    }

    // 查询交易信息
    @GetMapping("/trans/{hash}")
    fun getTrans(@PathVariable(value = "hash") hash: String): Mono<ResponseEntity<EthTransaction>> {
        return EtherBroker.broker.ethGetTransactionByHash(hash).sendAsync().toMono().map {
            ResponseEntity(it, HttpStatus.OK)
        }
    }
}
