package com.ethwal.server.contract

import io.netty.handler.codec.http.HttpMethod.GET
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.result.view.Rendering
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import java.nio.charset.Charset
import java.time.Duration
import kotlin.experimental.and

private const val hexDigitTable = "0123456789abcdef"

// TODO: transfer ether using smart contract
class SmartContract {

    fun sha3(str: String): String {
        val hash = SHA3.Digest256()
        hash.update(str.toByteArray(Charsets.UTF_8))
        val s = hash.digest()
        val sb = StringBuffer(s.size * 2)
        for (c in s) {
            val i = c.toInt() and 0x00ff
            sb.append(hexDigitTable[i shr 4])
            sb.append(hexDigitTable[i and 0x0f])
        }

        return sb.toString()
    }
}

data class SmartContractMessage(var payload: String? = null)

@Controller
@RequestMapping("/tx")
class ContractTestController {
    @RequestMapping("", method = [RequestMethod.GET, RequestMethod.POST] )
    //@ResponseBody
    fun contractIndex(): Rendering {
        var content = "Contracts"
        return Rendering.view("tx")
                .modelAttribute("content", content)
                .modelAttribute("messageList", ReactiveDataDriverContextVariable(
                        Flux.zip(
                               Flux.interval(Duration.ofMillis(1)),
                               Flux.just(
                                       SmartContractMessage("one"),
                                       SmartContractMessage("two"),
                                       SmartContractMessage("three")
                               )
                        ).map {
                            it.t2
                        }
                ))
                .build()
    }
}

