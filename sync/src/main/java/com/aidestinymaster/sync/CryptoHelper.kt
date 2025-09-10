package com.aidestinymaster.sync

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

object CryptoHelper {
    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    fun encryptToBytes(context: Context, plaintext: String): ByteArray {
        val key = masterKey(context)
        val tmp = File.createTempFile("aidd_enc_", ".bin", context.cacheDir)
        val ef = EncryptedFile.Builder(
            context,
            tmp,
            key,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        ef.openFileOutput().use { it.write(plaintext.toByteArray(Charsets.UTF_8)) }
        val bytes = tmp.readBytes()
        tmp.delete()
        return bytes
    }

    fun decryptFromBytes(context: Context, ciphertext: ByteArray): String {
        val key = masterKey(context)
        val tmp = File.createTempFile("aidd_dec_", ".bin", context.cacheDir)
        tmp.writeBytes(ciphertext)
        val ef = EncryptedFile.Builder(
            context,
            tmp,
            key,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        val data = ef.openFileInput().use { it.readBytes() }
        tmp.delete()
        return data.toString(Charsets.UTF_8)
    }
}

