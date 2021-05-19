package com.y.et.controller

import com.y.et.api.*
import com.y.et.Account
import com.y.et.Config
import com.y.et.model.Wallet
import com.google.common.hash.Hashing
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

//import javax.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.function.client.WebClient
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.infura.InfuraHttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset

// JsonRPC接口，用于web3j封装不满足需求的情况。暂未启用
@RestController
@RequestMapping("/infura/api")
class InfuraController {
    var broker = Web3j.build(InfuraHttpService(Config.web3jUrl))

    private val LOG = LogFactory.getLog(InfuraController::class.java)

    // 创建/绑定以太坊账户
    @PostMapping("/create_account")
    fun createAccount(@RequestBody request: CreateAccount, @RequestParam("sign") sign: String?) : Mono<CreateAccountResponse> {
        LOG.info(request)
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

        val account = Account.new(request.password)
        if (account == null) {
            response.status = "CREATE_ACCOUNT_ERROR"
        }
        else {
            response.status = "OK"
            response.account = account
        }

        LOG.info(response)
        return Mono.just(response)
    }

    // 权限校验，设置默认返回值
    fun checkTransAuth(request: SendTrans, sign: String?): SendTransResponse {
        var response = SendTransResponse()
        response.id = request.id
        val key = request.key
        if (key.isNullOrBlank()) {
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
    fun sendTrans(@RequestBody request: SendTrans,
                  @RequestParam("sign") sign: String?) : Mono<SendTransResponse> {
        LOG.info(request)
        // 参数检查，权限验证
        var response = checkTransAuth(request, sign)
        if (!(response.status.isNullOrBlank() || response.status == "OK")) {
            LOG.info(response)
            return Mono.just(response)
        }

        val credentials = Account.loadCredentials(request.account, request.password)
        if (credentials == null) {
            response.status = "PRIVATE_KEY_ERROR"
            return Mono.just(response)
        }

        // 将交易请求提交至以太坊节点
        response.status = "FAIL"
        return Transfer.sendFunds(broker, credentials, request.to, BigDecimal(request.value), Convert.Unit.ETHER)
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
                    LOG.info(response)
                    response
                }
                .onErrorResume {
                    // 发生错误
                    response.status = "ETHER_EXCEPTION"
                    response.msg = it.message
                    Mono.just(response)
                }
                .defaultIfEmpty (response)
    }

    // 显示交易信息，调试用途
    private fun checkTransactionReceipt(it: TransactionReceipt, response: SendTransResponse) {
        LOG.info("...................................................  getting block ${it.blockHash} .........")
        val bret = broker.ethGetBlockByHash(it.blockHash, true).send()
        if (bret.hasError()) {
            LOG.info(">>>>>>>>>>>>>>>>>> hash: ${it.blockHash} error = ${bret.error.code} msg = ${bret.error.message}")
        } else {
            val block = bret.block
            LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> block ${block.hash}, miner ${block.miner} trans ${block.transactions.size}")
        }
        val hash = it.transactionHash
        LOG.info("...................................................  getting transaction $hash .........")
        val ret = broker.ethGetTransactionByHash(hash).send()
        if (ret.hasError()) {
            LOG.info(">>>>>>>>>>>>>>>>>> hash: $hash error = ${ret.error.code} msg = ${ret.error.message}")
        } else {
            if (!ret.transaction.isPresent) {
                LOG.info(">>>>>>>>>>>>>>>>>>  hash: $hash trans = <not presented>")
            } else {
                val trans = ret.transaction.get()
                LOG.info(">>>>>>>>>>>>>>>>>>  block: ${trans.blockHash} value = ${trans.value} gas = ${trans.gas} gasPrice = ${trans.gasPrice}")
                var gas = trans.gas.multiply(trans.gasPrice).toBigDecimal()
                response.gas = Convert.fromWei(gas, Convert.Unit.ETHER).toString()
                response.value = Convert.fromWei(trans.value.toBigDecimal(), Convert.Unit.ETHER).toString()
            }
        }
    }

    // 查询交易结果
    @GetMapping("/result/{hash}")
    fun getTransResult(@PathVariable(value = "hash") hash: String,
                      @RequestParam("key") key: String?): Mono<ResponseEntity<GetTransResultResponse>> {
        if (key == null || !Config.canQuery(key)) {
            // privilege checking disabled for now
        }
        return broker.ethGetTransactionByHash(hash).sendAsync().toMono().flatMap {
            var response = GetTransResultResponse()

            when {
                it.hasError() -> {
                    response.status = "ERROR"
                    response.msg = it.error.message
                    Mono.just(ResponseEntity(response, HttpStatus.OK))
                }
                else -> if (!it.transaction.isPresent) {
                    response.status = "NOT_FOUND"
                    response.msg = "transaction not found"
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
                    broker.ethBlockNumber().sendAsync().toMono().map {
                        if (it.hasError()) {
                            response.status = "ERROR"
                            response.msg = "failure get latest block number"
                        } else {
                            val confirm = it.blockNumber.subtract(trans.blockNumber).toLong()
                            response.confirmed = confirm >= 5
                            response.msg = "confirmation count = $confirm"
                        }
                        ResponseEntity(response, HttpStatus.OK)
                    }
                }
            }
        }
    }

    // 查询账户余额 （从本地节点查询)
    @GetMapping("/balance/{account}")
    fun getBalance(@PathVariable(value = "account") account: String): Mono<ResponseEntity<GetBalanceResponse>> {
        var response = GetBalanceResponse()
        if (account.isNullOrBlank()) {
            response.status = "INVALID_ACCOUNT"
            return Mono.just(ResponseEntity(response, HttpStatus.BAD_REQUEST))
        }
        response.account = account
        return getEtherBalance(account).map {
            response.balance = it
            response.status = if (it.isNullOrBlank()) "FAIL" else "OK"
            ResponseEntity(response, HttpStatus.OK)
        }
    }

    private fun getEtherBalance(account: String): Mono<String> {
        LOG.info("account $account")
        return Mono.just("0")
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
            .exchangeToMono {
               it.bodyToMono(AccountBalance::class.java)
                   .map {
                   var response = GetBalanceResponse()
                   response.account = account
                   if (it.status == "1") {
                       response.status = "OK"
                       response.balance = it.result
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
            .exchangeToMono() {
                it.bodyToMono(Array<CoinMarketPrice>::class.java)
                    .map {
                        var response = GetMarketPriceResponse()
                        if (it.isEmpty()) {
                            response.status = "FAIL"
                            ResponseEntity(response, HttpStatus.NOT_FOUND)
                        } else {
                            var price = it[0]
                            latest.time = timestamp
                            latest.btc = price.price_btc ?: ""
                            latest.usd = price.price_usd ?: ""
                            response.usd = latest.usd
                            response.btc = latest.btc
                            response.time = latest.time
                            var gasPrice = broker.ethGasPrice().send()
                            response.gasPrice =
                                if (gasPrice.hasError()) "1000000000" else gasPrice.gasPrice.toString()
                            if (response.gasPrice.isNotBlank())
                                latest.gasPrice = response.gasPrice
                            response.status = "OK"
                            ResponseEntity(response, HttpStatus.OK)
                        }
                    }
            }
            .defaultIfEmpty(ResponseEntity(GetMarketPriceResponse(), HttpStatus.NOT_FOUND))
    }

    // 返回区块信息
    @GetMapping("/block/{name}")
    fun getBlockInfo(@PathVariable(value = "name") name: String?): Mono<ResponseEntity<EthBlock.Block>> {
        return if (name == null || name.isNullOrBlank())
            broker.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), true)
                        .sendAsync().toMono().map {
                        ResponseEntity(it.result, HttpStatus.OK)
                    }
        else if (name.startsWith("0x", true))
            broker.ethGetBlockByHash(name,true).sendAsync().toMono().map {
                ResponseEntity(it.result, HttpStatus.OK)
            }
        else
            broker.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger(name)), true)
                        .sendAsync().toMono().map {
                        ResponseEntity(it.result, HttpStatus.OK)
                    }
    }

    // 返回最新区块信息
    @GetMapping("/block")
    fun getLatestBlockInfo(): Mono<ResponseEntity<EthBlock.Block>> {
        return broker.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), true)
                    .sendAsync().toMono().map {
                        ResponseEntity(it.result, HttpStatus.OK)
                    }
    }

    // 查询交易信息
    @GetMapping("/trans/{hash}")
    fun getTrans(@PathVariable(value = "hash") hash: String): Mono<ResponseEntity<EthTransaction>> {
        return broker.ethGetTransactionByHash(hash).sendAsync().toMono().map {
            ResponseEntity(it, HttpStatus.OK)
        }
    }
}
