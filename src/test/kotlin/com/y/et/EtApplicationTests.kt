package com.y.et

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

//import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.contentType

//import com.y.et.model.Wallet
//import com.y.et.repository.WalletRepository

//@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EtApplicationTests {

	private val webTestClient = WebTestClient.bindToServer().build()

	@Test
	fun contextLoads() {
	}

	@Test
	fun testGetAllWallet() {
        /*
        webTestClient.get().uri("/walls")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBodyList(Wallet::class.java)
        */
	}

	@Test
	fun testUpdateWallet() {
	}
}
