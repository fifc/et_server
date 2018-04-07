package com.ethwal.server.contract

import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.jcajce.provider.digest.SHA3
import reactor.core.publisher.Mono
import java.nio.charset.Charset
import kotlin.experimental.and

// TODO: transfer ether using smart contract
class SmartContract {
    private val digits = "0123456789abcdef"

    fun sha3(str: String): String {
        var hash = SHA3.Digest256()
        hash.update(str.toByteArray(Charsets.UTF_8))
        var s = hash.digest()
        var sb = StringBuffer(s.size * 2)
        for (c in s) {
            val i = c.toInt() and 0x00ff
            sb.append(digits[i shr 4])
            sb.append(digits[i and 0x0f])
        }

        return sb.toString()
    }
}
