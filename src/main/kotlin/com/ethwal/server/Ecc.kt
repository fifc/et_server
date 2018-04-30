package com.ethwal.server

import java.io.IOException
import java.io.StringReader
import java.io.UnsupportedEncodingException
import java.security.*

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyAgreement
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.ShortBufferException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import kotlin.experimental.and

internal object Ecc {
    val publicKeyData = "-----BEGIN PUBLIC KEY-----\n" +
            "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAExHNZUYYyDpxjRistEci+CWFpQ39z/8US\n" +
            "dySJjs14xd0Apc7+yCXeyFyHyLJYe+pBv/CeQWoiR1X9XWZx1h3/tFri8b2HBoSx\n" +
            "mRfmLba76t7LA0+cq8HNcUpvAx6hbrcc\n" +
            "-----END PUBLIC KEY-----\n"

    val privateKeyData = "-----BEGIN EC PRIVATE KEY-----\n" +
            "MIGkAgEBBDCy5gZITZrrndsCKmQA2XlB+oQJpCQFUCASvrw/LZHgNII0nO6llBzo\n" +
            "K9CCJ4eD/nOgBwYFK4EEACKhZANiAATEc1lRhjIOnGNGKy0RyL4JYWlDf3P/xRJ3\n" +
            "JImOzXjF3QClzv7IJd7IXIfIslh76kG/8J5BaiJHVf1dZnHWHf+0WuLxvYcGhLGZ\n" +
            "F+Yttrvq3ssDT5yrwc1xSm8DHqFutxw=\n" +
            "-----END EC PRIVATE KEY-----\n"

    //public static byte[] iv = new SecureRandom().generateSeed(16);
    var iv = "VsfPks;;',iwYUw;UH2132.,02612-sdf".toByteArray()

    fun eccKey() {
        val publicKey: PublicKey
        try {
            val reader = StringReader(publicKeyData)
            val parser = PEMParser(reader)
            val obj = parser.readObject()
            publicKey = JcaPEMKeyConverter().getPublicKey(obj as SubjectPublicKeyInfo)
            println(publicKey.algorithm)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            val reader = StringReader(privateKeyData)
            val parser = PEMParser(reader)
            val obj = parser.readObject()
            val keyPair = JcaPEMKeyConverter().getKeyPair(obj as PEMKeyPair)
            val privateKey = keyPair.private
            println(privateKey.algorithm)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun ecc() {
        val plainText = "Look mah, I'm a message!"
        println("Original plaintext message: $plainText")

        Security.addProvider(BouncyCastleProvider())
        // Initialize two key pairs
        val keyPairA = generateECKeys()
        val keyPairB = generateECKeys()

        // Create two AES secret keys to encrypt/decrypt the message
        val secretKeyA = generateSharedSecret(keyPairA!!.private, keyPairB!!.public)
        val secretKeyB = generateSharedSecret(keyPairB.private, keyPairA.public)

        // Encrypt the message using 'secretKeyA'
        val cipherText = encryptString(secretKeyA, plainText)
        println("Encrypted cipher text: " + cipherText!!)

        // Decrypt the message using 'secretKeyB'
        val decryptedPlainText = decryptString(secretKeyB!!, cipherText)
        println("Decrypted cipher text: " + decryptedPlainText!!)
    }

    fun generateECKeys(): KeyPair? {
        try {
            val parameterSpec = ECNamedCurveTable.getParameterSpec("brainpoolp256r1")
            val keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC")

            keyPairGenerator.initialize(parameterSpec)

            return keyPairGenerator.generateKeyPair()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return null
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
            return null
        } catch (e: NoSuchProviderException) {
            e.printStackTrace()
            return null
        }

    }

    fun generateSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): SecretKey? {
        try {
            val keyAgreement = KeyAgreement.getInstance("ECDH", "BC")
            keyAgreement.init(privateKey)
            keyAgreement.doPhase(publicKey, true)

            return keyAgreement.generateSecret("AES")
        } catch (e: InvalidKeyException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            return null
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return null
        } catch (e: NoSuchProviderException) {
            e.printStackTrace()
            return null
        }

    }

    fun encryptString(key: SecretKey?, plainText: String): String? {
        try {
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")
            val plainTextBytes = plainText.toByteArray(charset("UTF-8"))
            val cipherText: ByteArray

            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            cipherText = ByteArray(cipher.getOutputSize(plainTextBytes.size))
            var encryptLength = cipher.update(plainTextBytes, 0,
                    plainTextBytes.size, cipherText, 0)
            encryptLength += cipher.doFinal(cipherText, encryptLength)

            return bytesToHex(cipherText)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return null
        } catch (e: NoSuchProviderException) {
            e.printStackTrace()
            return null
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
            return null
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
            return null
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
            return null
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            return null
        } catch (e: ShortBufferException) {
            e.printStackTrace()
            return null
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
            return null
        } catch (e: BadPaddingException) {
            e.printStackTrace()
            return null
        }

    }

    fun decryptString(key: SecretKey, cipherText: String?): String? {
        try {
            val decryptionKey = SecretKeySpec(key.encoded, key.algorithm)
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")
            val cipherTextBytes = hexToBytes(cipherText!!)

            cipher.init(Cipher.DECRYPT_MODE, decryptionKey, ivSpec)
            val plainText = ByteArray(cipher.getOutputSize(cipherTextBytes.size))
            var decryptLength = cipher.update(cipherTextBytes, 0, cipherTextBytes.size, plainText, 0)
            decryptLength += cipher.doFinal(plainText, decryptLength)

            return String(plainText, Charsets.UTF_8)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return null
        } catch (e: NoSuchProviderException) {
            e.printStackTrace()
            return null
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
            return null
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
            return null
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
            return null
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
            return null
        } catch (e: BadPaddingException) {
            e.printStackTrace()
            return null
        } catch (e: ShortBufferException) {
            e.printStackTrace()
            return null
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            return null
        }

    }

    @JvmOverloads
    fun bytesToHex(data: ByteArray, length: Int = data.size): String {
        val digits = "0123456789ABCDEF"
        val buffer = StringBuilder()

        for (i in 0 until length) {
            val v = data[i] and 0xff.toByte()

            buffer.append(digits[v.toInt() shr 4])
            buffer.append(digits[(v and 0xf.toByte()).toInt()])
        }

        return buffer.toString()
    }

    fun hexToBytes(string: String): ByteArray {
        val length = string.length
        val data = ByteArray(length / 2)
        var i = 0
        while (i < length) {
            data[i / 2] = ((Character.digit(string[i], 16) shl 4) + Character
                    .digit(string[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
