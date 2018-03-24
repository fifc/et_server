package com.ethwal.server.controller

import  com.ethwal.server.api.*
import com.ethwal.server.Account
import com.ethwal.server.Config
import com.ethwal.server.EtherBroker
import com.ethwal.server.model.Wallet
import com.ethwal.server.repository.WalletRepository
import jdk.incubator.http.HttpClient
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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.function.Predicate

@RestController
class WalletController {
    @Autowired
    private val walletRepository: WalletRepository? = null

    @PostMapping("/create_account")
    fun getAccount(@Valid @RequestBody request: CreateAccount) : Mono<CreateAccountResponse> {
        var response = CreateAccountResponse()
        response.id = request.id
        if (request.password.isNullOrEmpty() || request.password.length < 6) {
            response.result = "INVALID_PASSWORD"
            return Mono.just(response)
        }
        if (request.userId.isNullOrEmpty()) {
            response.result = "INVALID_USERID"
            return Mono.just(response)
        }

        val account = Account.new(request.password)
        if (account == null) {
            response.result = "CREATE_ACCOUNT_ERROR"
        }
        else {
            response.result = "OK"
            response.account = account
            if (walletRepository != null) {
                var wallet = Wallet(account)
                wallet.userId = request.userId
                walletRepository.save(Wallet(response.account))
            }
        }

        return Mono.just(response)
    }

    @PostMapping("/send_trans")
    fun sendTrans(@Valid @RequestBody request: SendTrans) : Mono<SendTransResponse> {
        var response = SendTransResponse()
        response.id = request.id
        if (request.password.isNullOrEmpty()) {
            response.result = "INVALID_PASSWORD"
            return Mono.just(response)
        }

        if (request.account.isNullOrEmpty()) {
            response.result = "INVALID_ACCOUNT"
            return Mono.just(response)
        }

        if (request.toAccount.isNullOrEmpty()) {
            response.result = "INVALID_DEST_ACCOUNT"
            return Mono.just(response)
        }

        val account = Account.new(request.password)
        if (account == null) {
            response.result = "CREATE_ACCOUNT_ERROR"
        }
        else {
            response.result = "OK"
            response.account = account
        }
        return Mono.just(response)
    }

    @GetMapping("/get_balance/{account}")
    fun getLocalBalance(@PathVariable(value = "account") account: String): Mono<ResponseEntity<GetBalanceResponse>> {
        var response = GetBalanceResponse()
        if (account.isNullOrBlank()) {
            response.result = "INVALID_ACCOUNT"
            return Mono.just(ResponseEntity(response, HttpStatus.BAD_REQUEST))
        }
        response.account = account
        response.balance = EtherBroker.getBalance(account)
        response.result = if (response.balance.isBlank()) "FAIL" else "OK"
        return Mono.just(ResponseEntity(response, HttpStatus.OK))
    }

    @GetMapping("/balance/{account}")
    fun getBalance(@PathVariable(value = "account") account: String): Mono<ResponseEntity<GetBalanceResponse>> {
        var response = GetBalanceResponse()
        if (account.isNullOrBlank()) {
            response.result = "INVALID_ACCOUNT"
            return Mono.just(ResponseEntity(response, HttpStatus.BAD_REQUEST))
        }
        var webClient = WebClient.create()
        return webClient.get().uri("https://api.etherscan.io/api?module=account&action=balance&address=${account}&tag=latest&apikey=${Config.etherscanKey}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .flatMap {
                    it.bodyToMono(AccountBalance::class.java)
                            .map {
                                var response = GetBalanceResponse()
                                response.account = account
                                if (it.status == "1") {
                                    response.result = "OK"
                                    response.balance = it.result
                                    ResponseEntity(response, HttpStatus.OK)
                                } else {
                                    response.result = "FAIL"
                                    response.msg = "3rd party returned: ${it.message}"
                                    ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
                                }
                            }
                }
                .defaultIfEmpty(ResponseEntity(GetBalanceResponse(), HttpStatus.NOT_FOUND))

    }

    @GetMapping("/get_market_price")
    fun getMarketPrice(/*@Valid @RequestBody request: GetMarketPrice*/): Mono<ResponseEntity<GetMarketPriceResponse>> {
        val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (timestamp < Config.marketPrice.time + Config.priceUpdatePeriod) { // 10分钟内读取本地缓存
            var response = GetMarketPriceResponse()
            response.usd = Config.marketPrice.usd
            response.btc = Config.marketPrice.btc
            response.time = Config.marketPrice.time
            response.result = "OK"
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
                                    response.result = "FAIL"
                                    ResponseEntity(response, HttpStatus.NOT_FOUND)
                                } else {
                                    var price = it[0]
                                    Config.marketPrice.time = timestamp
                                    Config.marketPrice.btc = price.price_btc?:""
                                    Config.marketPrice.usd = price.price_usd?:""
                                    response.usd = Config.marketPrice.usd
                                    response.btc = Config.marketPrice.btc
                                    response.time = Config.marketPrice.time
                                    response.result = "OK"
                                    ResponseEntity(response, HttpStatus.OK)
                                }
                            }
                }
                .defaultIfEmpty(ResponseEntity(GetMarketPriceResponse(), HttpStatus.NOT_FOUND))
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
