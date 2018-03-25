package com.ethwal.server.controller

import  com.ethwal.server.api.*
import com.ethwal.server.Account
import com.ethwal.server.Config
import com.ethwal.server.EtherBroker
import com.ethwal.server.model.Wallet
import com.ethwal.server.repository.WalletRepository
//import jdk.incubator.http.HttpClient
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Example
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import javax.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.function.client.WebClient
import org.web3j.protocol.admin.methods.response.PersonalListAccounts
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthSyncing
import org.web3j.protocol.core.methods.response.EthTransaction
import reactor.core.publisher.toMono
import java.time.LocalDateTime
import java.time.ZoneOffset
//import java.util.function.Predicate

@RestController
class WalletController {
    @Autowired
    private val walletRepository: WalletRepository? = null

    private val LOG = LogFactory.getLog(WalletController::class.java)

    // 创建/绑定以太坊账户
    @PostMapping("/create_account")
    fun createAccount(@Valid @RequestBody request: CreateAccount) : Mono<CreateAccountResponse> {
        LOG.info(request)
        var response = CreateAccountResponse()
        response.id = request.id
        if (request.password.isNullOrEmpty() || request.password.length < 6) {
            response.status = "INVALID_PASSWORD"
            return Mono.just(response)
        }
        if (request.userId.isNullOrEmpty()) {
            response.status = "INVALID_USERID"
            return Mono.just(response)
        }

        val account = Account.new(request.password)
        if (account == null) {
            response.status = "CREATE_ACCOUNT_ERROR"
        }
        else {
            response.status = "OK"
            response.account = account
            if (walletRepository != null) {
                var wallet = Wallet(account)
                wallet.userId = request.userId
                walletRepository.save(Wallet(response.account))
            }
        }

        LOG.info(response)
        return Mono.just(response)
    }

    // 以太币转账
    @PostMapping("/send_trans")
    fun sendTrans(@Valid @RequestBody request: SendTrans) : Mono<SendTransResponse> {
        LOG.info(request)
        var response = SendTransResponse()
        response.id = request.id
        if (request.password.isNullOrEmpty()) {
            response.status = "INVALID_PASSWORD"
            return Mono.just(response)
        }

        if (request.account.isNullOrEmpty()) {
            response.status = "INVALID_ACCOUNT"
            return Mono.just(response)
        }

        if (request.toAccount.isNullOrEmpty()) {
            response.status = "INVALID_DEST_ACCOUNT"
            return Mono.just(response)
        }

        val account = Account.new(request.password)
        if (account == null) {
            response.status = "CREATE_ACCOUNT_ERROR"
        } else {
            response.status = "OK"
            response.account = account
        }

        LOG.info(response)
        return Mono.just(response)
    }

    // 查询账户余额 （本地节点)
    @GetMapping("/get_balance/{account}")
    fun getLocalBalance(@PathVariable(value = "account") account: String): Mono<ResponseEntity<GetBalanceResponse>> {
        var response = GetBalanceResponse()
        if (account.isNullOrBlank()) {
            response.status = "INVALID_ACCOUNT"
            return Mono.just(ResponseEntity(response, HttpStatus.BAD_REQUEST))
        }
        response.account = account
        response.balance = EtherBroker.getBalance(account)
        response.status = if (response.balance.isBlank()) "FAIL" else "OK"
        return Mono.just(ResponseEntity(response, HttpStatus.OK))
    }

    // 查询账户余额 （etherscan api)
    @GetMapping("/balance/{account}")
    fun getBalance(@PathVariable(value = "account") account: String): Mono<ResponseEntity<GetBalanceResponse>> {
        if (account.isNullOrBlank()) {
            var response = GetBalanceResponse()
            response.status = "INVALID_ACCOUNT"
            return Mono.just(ResponseEntity(response, HttpStatus.BAD_REQUEST))
        }

        var webClient = WebClient.create()

        //return webClient.get().uri("https://api.etherscan.io/api?module=account&action=balance&address=${account}&tag=latest&apikey=${Config.etherscanKey}")
        //return webClient.get().uri("https://api-rinkeby.etherscan.io/api?module=account&action=balance&address=${account}&tag=latest&apikey=${Config.etherscanKey}")
        return webClient.get().uri("https://api-ropsten.etherscan.io/api?module=account&action=balance&address=${account}&tag=latest&apikey=${Config.etherscanKey}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .flatMap {
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
    @GetMapping("/get_market_price")
    fun getMarketPrice(/*@Valid @RequestBody request: GetMarketPrice*/): Mono<ResponseEntity<GetMarketPriceResponse>> {
        val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (timestamp < Config.marketPrice.time + Config.priceUpdatePeriod) { // 10分钟内读取本地缓存
            var response = GetMarketPriceResponse()
            response.usd = Config.marketPrice.usd
            response.btc = Config.marketPrice.btc
            response.time = Config.marketPrice.time
            response.status = "OK"
            return Mono.just(ResponseEntity(response, HttpStatus.OK))
        }
        // 缓存过期，从网上拉取行情
        var webClient = WebClient.create()
        return webClient.get().uri("https://api.coinmarketcap.com/v1/ticker/ethereum/?convert=EUR")
                .accept(MediaType.APPLICATION_JSON_UTF8)
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
                                    Config.marketPrice.time = timestamp
                                    Config.marketPrice.btc = price.price_btc?:""
                                    Config.marketPrice.usd = price.price_usd?:""
                                    response.usd = Config.marketPrice.usd
                                    response.btc = Config.marketPrice.btc
                                    response.time = Config.marketPrice.time
                                    var gasPrice = EtherBroker.broker.ethGasPrice().send()
                                    response.gasPrice = if (gasPrice.hasError()) "" else gasPrice.gasPrice.toString()
                                    response.status = "OK"
                                    ResponseEntity(response, HttpStatus.OK)
                                }
                            }
                }
                .defaultIfEmpty(ResponseEntity(GetMarketPriceResponse(), HttpStatus.NOT_FOUND))
    }

    // 返回区块链同步状态
    @GetMapping("/")
    fun getHomepage(): Mono<ResponseEntity<EthSyncing.Result>> {
        return EtherBroker.admin.ethSyncing().sendAsync().toMono().map {
            ResponseEntity(it.result, HttpStatus.OK)
        }
    }
    // 返回最新区块
    @GetMapping("/block")
    fun getLatestBlock(): Mono<ResponseEntity<EthBlock.Block>> {
        return EtherBroker.broker.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), true).sendAsync().toMono().map {
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

    @GetMapping("/walls")
    fun getAllWallets() : Flux<Wallet> {
        if (walletRepository == null)
            return Flux.empty()

        return walletRepository.findAll()
    }

    @GetMapping("/count")
    fun count() : Mono<ResponseEntity<Long>> {
        return walletRepository?.findAll(Example.of(Wallet("")))
                ?.reduce(0L, { t, wallet ->
                    println("found wallet: ${wallet.account}")
                    t + 1
                })
                ?.map({ t ->
                    if (t != 0L) {
                        ResponseEntity(t, HttpStatus.OK)
                    } else {
                        ResponseEntity(HttpStatus.NOT_FOUND)
                    }
                })
                ?.defaultIfEmpty( ResponseEntity(HttpStatus.NOT_FOUND))
                ?: Mono.just(ResponseEntity(HttpStatus.NOT_FOUND))
    }

    @PutMapping("/wall/{account}")
    fun updateWallet(@PathVariable(value = "account") account: String,
                    @Valid @RequestBody wallet: Wallet): Mono<ResponseEntity<Wallet>> {
        return walletRepository!!.findById(account)
                .flatMap({ existingWallet ->
                    existingWallet.description = wallet.description
                    walletRepository.save(existingWallet)
                })
                .map({ updatedTweet -> ResponseEntity(updatedTweet, HttpStatus.OK) })
                .defaultIfEmpty(ResponseEntity(HttpStatus.NOT_FOUND))
    }

    // Tweets are Sent to the client as Server Sent Events
    @GetMapping( value = ["/stream/wall"],  produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamAllWallets(): Flux<Wallet> {
        return walletRepository!!.findAll().map({wallet ->
            wallet.description += " - stream"
            wallet
        })
    }
}
