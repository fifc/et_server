package com.ethwal.server

import com.ethwal.server.model.Wallet
import com.ethwal.server.repository.WalletRepository

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.contentType




@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerApplicationTests {
    private val webTestClient = WebTestClient.bindToServer().build()

    @Test
    fun testGetAllWallet() {
        webTestClient.get().uri("/walls")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBodyList(Wallet::class.java)
    }

    @Test
    fun testUpdateWallet() {
    }

    @Test
	fun contextLoads() {
	}
}
