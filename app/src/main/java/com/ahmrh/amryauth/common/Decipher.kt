package com.ahmrh.amryauth.common

import android.util.Log
import com.ahmrh.amryauth.BuildConfig
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object Decipher {
    fun decryptAES(cipherText: String): String {

        val secretKey = "3r2Si3uc7XSgh53o".toByteArray(Charsets.UTF_8)
        val initVector = "doBiHebupO9fTkKo".toByteArray(Charsets.UTF_8)

        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(secretKey, "AES")
            val ivSpec = IvParameterSpec(initVector)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

            val decodedCiphertext = Base64.getDecoder().decode(cipherText)
            val decryptedData = cipher.doFinal(decodedCiphertext)
            return String(decryptedData, Charsets.UTF_8)
        } catch (ex: Exception) {
            println("Decryption failed with: ${ex.javaClass.simpleName} - ${ex.message}")
        }
        return ""
    }


}