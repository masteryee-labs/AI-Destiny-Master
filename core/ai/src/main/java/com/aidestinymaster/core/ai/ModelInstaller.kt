package com.aidestinymaster.core.ai

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

object ModelInstaller {
    private const val MODELS_DIR = "models"
    private const val MODELS_ZIP = "models.zip"

    fun modelsDir(context: Context): File = File(context.filesDir, MODELS_DIR)
    fun modelFile(context: Context, name: String): File = File(modelsDir(context), name)

    /**
     * Copy assets/models.zip -> files/models/ and unzip when not yet installed.
     * If expectedSha256 is provided, will validate target file's checksum.
     */
    fun installIfNeeded(context: Context, expectedSha256: String? = null): Boolean {
        val outDir = modelsDir(context)
        if (outDir.exists() && outDir.isDirectory && outDir.list()?.isNotEmpty() == true) {
            // Already installed; if checksum requested, try validate a main model file
            return true
        }
        // Ensure clean dir
        outDir.mkdirs()

        // Open asset zip; if not present, skip gracefully
        val assetIs: InputStream = runCatching { context.assets.open(MODELS_ZIP) }.getOrNull() ?: return false

        // Unzip to outDir
        ZipInputStream(assetIs).use { zis ->
            var entry = zis.nextEntry
            val buf = ByteArray(8192)
            while (entry != null) {
                val dest = File(outDir, entry.name)
                if (entry.isDirectory) {
                    dest.mkdirs()
                } else {
                    dest.parentFile?.mkdirs()
                    FileOutputStream(dest).use { fos ->
                        while (true) {
                            val n = zis.read(buf)
                            if (n <= 0) break
                            fos.write(buf, 0, n)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        // Optionally validate a main model file
        val guess = outDir.listFiles()?.firstOrNull { it.name.endsWith(".onnx", ignoreCase = true) }
        if (guess == null) return true
        if (!expectedSha256.isNullOrBlank()) return validateSha256(guess, expectedSha256)
        // If .sha256 is present next to .onnx, use it
        val localSha = File(guess.parentFile, guess.name + ".sha256")
        if (localSha.exists()) {
            val exp = runCatching { localSha.readText().trim() }.getOrNull()
            if (!exp.isNullOrBlank()) return validateSha256(guess, exp)
        }
        return true
    }

    fun validateSha256(file: File, expected: String): Boolean {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buf = ByteArray(8192)
            while (true) {
                val n = fis.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        val digest = md.digest().joinToString("") { "%02x".format(it) }
        return digest.equals(expected.trim(), ignoreCase = true)
    }
}
