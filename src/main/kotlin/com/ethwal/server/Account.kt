package com.ethwal.server

import java.security.NoSuchProviderException
import java.security.NoSuchAlgorithmException
import java.security.InvalidAlgorithmParameterException
import java.io.IOException
import org.web3j.crypto.CipherException
import org.web3j.crypto.WalletUtils

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

        fun new(password: String): String? {
            val destinationDir = getDestinationDir()
            val destination = createDir(destinationDir)

            try {
                var walletFileName = if (isFullNode())
                    WalletUtils.generateFullNewWalletFile(password, destination)
                else
                    WalletUtils.generateLightNewWalletFile(password, destination)
                println("Wallet file $walletFileName successfully created in: $destinationDir")
                val credentials = WalletUtils.loadCredentials(password, "$destinationDir/$walletFileName")
                return credentials.address
            } catch (e: CipherException) {
                println(e)
            } catch (e: IOException) {
                println(e)
            } catch (e: InvalidAlgorithmParameterException) {
                println(e)
            } catch (e: NoSuchAlgorithmException) {
                println(e)
            } catch (e: NoSuchProviderException) {
                println(e)
            }
            return null
        }
    }
}