package com.y.et

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.LocalDateTime
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

@SpringBootApplication
class EtApplication {
    var startTime = LocalDateTime.now()
    constructor() {
        startTime = LocalDateTime.now()
    }
}

fun main(args: Array<String>) {
    ArgParser(args).parseInto(::MyArgs).run {
        if (noInfura) {
            //Config.useInfura = false
            println("no_infura is set")
        }
    }

    Health.appContext = runApplication<EtApplication>(*args)
    Health.assertInherentition()
}

class MyArgs(parser: ArgParser) {
    val noInfura by parser.flagging("--no_infura", help = "do not use infura node")

    val name :String by parser.storing("-n", "--name", help = "name of the user").default("")

    val thread by parser.storing("-t", "--thread", help = "thread number") { toInt() } .default(8)

    val source :String by parser.positional("source filename").default ( "" )
}

