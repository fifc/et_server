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
            val list = dir.listFiles({ dir, name->
                name.contains(pattern, true)
            })

            return if (list == null || list.isEmpty())
                null
            else {
                var cre = WalletUtils.loadCredentials(password, list[0])
                cre
            }
        }
    }
}