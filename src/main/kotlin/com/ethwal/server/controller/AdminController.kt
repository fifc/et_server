package com.ethwal.server.controller

import com.ethwal.server.Config
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/")
class AdminController {
    @GetMapping("/")
    fun homepage(model: Model): Mono<String> {
        return Mono.just("index")
    }

    @RequestMapping(
            value = ["*"],
            method = [ RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE])
    @ResponseBody
    fun allFallback(): String {
        val sec = Config.authMap["becac26346d9711e39bddc87acc699997ddc7ff8"]?.second?:-1L
        return "this is fallback for all requests - $sec"
    }
}