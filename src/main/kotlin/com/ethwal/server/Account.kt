package com.ethwal.server

import org.apache.commons.logging.LogFactory
import java.security.NoSuchProviderException
import java.security.NoSuchAlgorithmException
import java.security.InvalidAlgorithmParameterException
import java.io.IOException
import org.web3j.crypto.CipherException
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import reactor.core.publisher.Mono

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import javax.security.auth.callback.LanguageCallback
import kotlin.concurrent.withLock

/*
{
    accounts: [{
        address: "0x4a3a7520628ee51a9c225fc296b59b94af6a1469",
        url: "keystore://C:\\Users\\1\\AppData\\Roaming\\Ethereum\\keystore\\UTC--2018-03-21T09-48-46.363198200Z--4a3a7520628ee51a9c225fc296b59b94af6a1469"
    }],
    status: "Locked",
    url: "keystore://C:\\Users\\1\\AppData\\Roaming\\Ethereum\\keystore\\UTC--2018-03-21T09-48-46.363198200Z--4a3a7520628ee51a9c225fc296b59b94af6a1469"
}
*/
class Account  {
    companion object {
        private val LOG = LogFactory.getLog(Account::class.java)
        private var credentialMap: MutableMap<String, Pair<Credentials, LocalDateTime>> = mutableMapOf()
        private val random = Random()

        private fun getDestinationDir(): String {
            return Config.keystoreDir
        }

        private fun isFullNode(): Boolean {
            return Config.isFullNode
        }

        private fun createDir(destinationDir: String): File {
            val destination = File(destinationDir)

            if (!destination.exists()) {
                if (!destination.mkdirs()) {
                    println("Unable to create destination directory [$destinationDir] ...")
                    throw IOException()
                }
            }

            return destination
        }

        // 离线模式
        fun new(password: String): String? {
            val destinationDir = getDestinationDir()
            val destination = createDir(destinationDir)

            var address: String? = null

            try {
                val seperator = System.getProperty("file.separator")
                var walletFileName = if (isFullNode())
                    WalletUtils.generateFullNewWalletFile(password, destination)
                else
                    WalletUtils.generateLightNewWalletFile(password, destination)

                println("Wallet file $walletFileName successfully created in: $destinationDir")

                val credentials = WalletUtils.loadCredentials(password, "$destinationDir$seperator$walletFileName")
                address = credentials.address
            } catch (e: CipherException) {
                LOG.error(e)
            } catch (e: IOException) {
                LOG.error(e)
            } catch (e: InvalidAlgorithmParameterException) {
                LOG.error(e)
            } catch (e: NoSuchAlgorithmException) {
                LOG.error(e)
            } catch (e: NoSuchProviderException) {
                LOG.error(e)
            }

            return address
        }
        // 账户导入 - 私钥形式
        fun importKey(key: String, password: String): Mono<String> {
            return Mono.defer {
                val keyFile = "accimport-${LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)}-${random.nextLong()}"
                val pwFile = "$keyFile-p"
                var address: String? = null
                var err: Throwable? = null
                try {
                    File(keyFile).printWriter().use {
                        it.print(key)
                    }
                    File(pwFile).printWriter().write(password)
                    //INFO [04-10|14:14:34] Maximum peer count                       ETH=25 LES=0 total=25
                    //Address: {becac26346d9711e39bddc87acc699997ddc7ff8}
                    val output = "./geth account import $keyFile --keystore ${Config.keystoreDir} --password $pwFile".runCommand(File("."))
                    if (output.isNullOrBlank()) {
                        throw Exception("system error")
                    }
                    val regex = """.*Address:.*\{([a-zA-Z0-9]+)\}""".toRegex()
                    val result = regex.find(output!!)
                    if (result != null) {
                        address = result.value
                    }
                } catch (e: CipherException) {
                    err = e
                } catch (e: IOException) {
                    err = e
                } catch (e: InvalidAlgorithmParameterException) {
                    err = e
                } catch (e: NoSuchAlgorithmException) {
                    err = e
                } catch (e: NoSuchProviderException) {
                    err = e
                } finally {
                    Files.delete(Paths.get(keyFile))
                    Files.delete(Paths.get(pwFile))
                }

                if (err != null) {
                    LOG.error(err)
                    Mono.error(err)
                } else {

                    Mono.just(address?:"")
                }
            }
        }

        // 账户导入 - 文件形式
        fun importFile(data: String, password: String): Mono<String> {
            return Mono.defer {
                var address = ""
                var err: Throwable? = null
                var keyFile = "accimport-${LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)}-${random.nextLong()}"
                try {
                    File(keyFile).writer().write(data)
                    val credentials = WalletUtils.loadCredentials(password, keyFile)
                    address = credentials.address
                    val destinationDir = getDestinationDir()
                    createDir(destinationDir)
                    val separator = System.getProperty("file.separator")
                    val destKeyFile = "$destinationDir$separator$keyFile-$address"
                    Files.move(Paths.get(keyFile), Paths.get(destKeyFile))
                } catch (e: CipherException) {
                    err = e
                } catch (e: IOException) {
                    err = e
                } catch (e: InvalidAlgorithmParameterException) {
                    err = e
                } catch (e: NoSuchAlgorithmException) {
                    err = e
                } catch (e: NoSuchProviderException) {
                    err = e
                } finally {
                }

                if (err != null) {
                    try {
                        Files.delete(Paths.get(keyFile))
                    } catch(e: Exception) { }
                    LOG.error(err)
                    Mono.error(err)
                } else {

                    Mono.just(address)
                }
            }
        }

        // 离线模式, 异步模式
        fun newAsync(password: String): Mono<String> {
            return Mono.defer {
                val destinationDir = getDestinationDir()
                val destination = createDir(destinationDir)

                var address = ""
                var err: Throwable? = null

                try {
                    val seperator = System.getProperty("file.separator")
                    var walletFileName = if (isFullNode())
                        WalletUtils.generateFullNewWalletFile(password, destination)
                    else
                        WalletUtils.generateLightNewWalletFile(password, destination)

                    println("Wallet file $walletFileName successfully created in: $destinationDir")

                    val credentials = WalletUtils.loadCredentials(password, "$destinationDir$seperator$walletFileName")
                    address = credentials.address
                } catch (e: CipherException) {
                    err = e
                    LOG.error(e)
                } catch (e: IOException) {
                    err = e
                    LOG.error(e)
                } catch (e: InvalidAlgorithmParameterException) {
                    err = e
                } catch (e: NoSuchAlgorithmException) {
                    err = e
                } catch (e: NoSuchProviderException) {
                    err = e
                } catch (e: Throwable) {
                    err = e
                }

                if (err != null) {
                    LOG.error(err)
                    Mono.error(err)
                } else
                    Mono.just(address)
            }
        }

        // RPC模式
        fun create(password: String): String? {
            val admin = EtherBroker.admin ?: return null
            return try {
                val account = admin.personalNewAccount(password).send()
                account.accountId
            } catch (e: Exception) {
                LOG.error(e)
                null
            }
        }

        fun loadCredentials(account: String, password: String): Credentials? {
            val dir = File(Config.keystoreDir)
            val pattern = account.substring(2)
            val list = dir.listFiles({ _, name->
                name.contains(pattern, true)
            })

            return if (list == null || list.isEmpty())
                null
            else {
                try {
                    var credentilals = WalletUtils.loadCredentials(password, list[0])
                    credentilals
                } catch (e: Exception) {
                    LOG.info("invalid address or password: $account")
                    LOG.info(e)
                    null
                }
            }
        }

        // 根据用户输入的密码解锁账户
        fun checkoutCredentials(account: String, password: String): Mono<Credentials> {
            synchronized(credentialMap, {
                var cr = credentialMap.get(account)
                if (cr != null) {
                    credentialMap.remove(account)
                    if (cr.second.plusMinutes(30).isAfter(LocalDateTime.now())) {
                        credentialMap.put(account, Pair(cr.first, LocalDateTime.now()))
                        return Mono.just(cr.first)
                    }
                }
            })

            return Mono.defer {
                val dir = File(Config.keystoreDir)
                val pattern = account.substring(2)
                val list = dir.listFiles({ _, name ->
                    name.contains(pattern, true)
                })

                if (list == null || list.isEmpty())
                    Mono.empty<Credentials>()
                else {
                    try {
                        var credentials = WalletUtils.loadCredentials(password, list[0])
                        addCredentialCache(account, credentials)
                        Mono.just(credentials)
                    } catch (e: Exception) {
                        LOG.info("invalid address or password: $account")
                        LOG.info(e)
                        Mono.error<Credentials>(e)
                    }
                }
            }
        }

        // 缓存加载的credentials
        private fun addCredentialCache(account: String, credentials: Credentials) {
            val cacheNum = 1000
            val timestamp = LocalDateTime.now()
            synchronized(credentialMap, {
                if (credentialMap.size >= cacheNum) {
                    var key :String? = null
                    var oldest = timestamp
                    var removeList : MutableList<String> = mutableListOf()
                    for (item in credentialMap) {
                        if (item.value.second.plusMinutes(30).isBefore(timestamp)) {
                            removeList.add(item.key)
                        } else {
                           if (removeList.isEmpty() && item.value.second.isBefore(oldest)) {
                               key = item.key
                               oldest = item.value.second

                           }
                        }
                    }
                    if (removeList.isEmpty()) {
                        if (key != null)
                            credentialMap.remove(key)
                        else
                            credentialMap.clear()
                    } else {
                        for (item in removeList)
                            credentialMap.remove(item)
                    }

                }
                credentialMap.put(account, Pair(credentials, timestamp))
            })
        }

        // 运行shell命令，返回程序的输出
        private fun String.runCommand(workingDir: File, input: String? = null): String? {
            return try {
                val parts = this.split("\\s".toRegex())
                val proc = ProcessBuilder(*parts.toTypedArray())
                        .directory(workingDir)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .start()

                if (input != null) {
                    proc.outputStream.write(input.toByteArray())
                }

                proc.waitFor(60, TimeUnit.MINUTES)
                proc.inputStream.bufferedReader().readText()
            } catch(e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}