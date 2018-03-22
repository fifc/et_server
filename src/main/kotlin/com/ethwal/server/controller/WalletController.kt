package com.ethwal.server.controller

import  com.ethwal.server.api.*
import com.ethwal.server.Account
import com.ethwal.server.model.Wallet
import com.ethwal.server.repository.WalletRepository
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

@RestController
class WalletController {
    @Autowired
    private val walletRepository: WalletRepository? = null

    @PostMapping("/create_account}")
    fun createAccount(@Valid @RequestBody request: CreateAccount) : Mono<CreateAccountResponse> {
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

        }
        return Mono.just(response)
    }

    @PostMapping("/send_trans}")
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

    @GetMapping("/walls")
    fun getAllWallets() : Flux<Wallet> {
        if (walletRepository == null)
            return Flux.empty()

        return walletRepository.findAll()
    }

    @GetMapping("/count")
    fun count() : Mono<ResponseEntity<Long>> {
        return walletRepository?.findAll(Example.of(Wallet("")))?.reduce(0L, { t, u -> t + 1 })?.map({ t ->
            if (t != 0L) {
                ResponseEntity(t, HttpStatus.OK)
            } else {
                ResponseEntity(HttpStatus.NOT_FOUND)
            }
        })?.defaultIfEmpty( ResponseEntity(HttpStatus.NOT_FOUND)) ?: Mono.just(ResponseEntity(HttpStatus.NOT_FOUND))
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
