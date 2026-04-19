package com.example.familyvault.security

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object FileSecurity {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES"

    private fun getKey(): SecretKey {
        val keyBytes = "1234567890123456".toByteArray() // 16 chars
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun encryptFile(input: File, output: File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val inputStream = FileInputStream(input)
        val outputStream = FileOutputStream(output)

        val buffer = ByteArray(1024)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val outputBytes = cipher.update(buffer, 0, bytesRead)
            if (outputBytes != null) outputStream.write(outputBytes)
        }

        val finalBytes = cipher.doFinal()
        if (finalBytes != null) outputStream.write(finalBytes)

        inputStream.close()
        outputStream.close()
    }

    fun decryptFile(input: File, output: File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getKey())

        val inputStream = FileInputStream(input)
        val outputStream = FileOutputStream(output)

        val buffer = ByteArray(1024)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val outputBytes = cipher.update(buffer, 0, bytesRead)
            if (outputBytes != null) outputStream.write(outputBytes)
        }

        val finalBytes = cipher.doFinal()
        if (finalBytes != null) outputStream.write(finalBytes)

        inputStream.close()
        outputStream.close()
    }
}