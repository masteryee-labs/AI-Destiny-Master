package com.aidestinymaster.core.ai

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

object ModelInstaller {
    private const val MODELS_DIR = "models"
    private const val MODELS_ZIP = "models.zip"
    private const val TAG = "ModelInstaller"

    fun modelsDir(context: Context): File = File(context.filesDir, MODELS_DIR)
    fun modelFile(context: Context, name: String): File = File(modelsDir(context), name)

    /**
     * Copy assets/models.zip -> files/models/ and unzip when not yet installed.
     * If expectedSha256 is provided, will validate target file's checksum.
     */
    fun installIfNeeded(context: Context, expectedSha256: String? = null): Boolean {
        val outDir = modelsDir(context)
        if (outDir.exists() && outDir.isDirectory && outDir.list()?.isNotEmpty() == true) {
            // Already installed; if checksum available (explicit or .sha256), validate on every launch
            val guess = outDir.listFiles()?.firstOrNull { it.name.endsWith(".onnx", ignoreCase = true) }
            if (guess != null) {
                val explicit = expectedSha256?.takeIf { it.isNotBlank() }
                val localSha = File(guess.parentFile, guess.name + ".sha256")
                val expected = explicit ?: runCatching { if (localSha.exists()) localSha.readText().trim() else "" }.getOrDefault("")
                if (!expected.isNullOrBlank()) {
                    val ok = validateSha256(guess, expected)
                    Log.i(TAG, "validateModelChecksum=$ok file=${guess.name}")
                    if (!ok) {
                        Log.e(
                            TAG,
                            "ERR_MODEL_CHECKSUM_MISMATCH: 校驗失敗。建議：請重新下載模型資產或於系統設定→應用→清除資料後重試。file=${guess.absolutePath} expectedSha256=${expected.take(16)}..."
                        )
                    }
                    return ok
                }
            }
            return true
        }
        // Ensure clean dir
        outDir.mkdirs()

        // Open asset zip; if not present, skip gracefully
        val assetIs: InputStream = runCatching { context.assets.open(MODELS_ZIP) }.getOrNull() ?: run {
            Log.w(TAG, "assets/$MODELS_ZIP not found; skip install")
            Log.e(TAG, "ERR_MODELS_ASSET_NOT_FOUND: 找不到資產檔案 assets/$MODELS_ZIP，請確認已將 models.zip 放入 app/src/main/assets/ 後重試。")
            return false
        }

        // Unzip to outDir
        Log.i(TAG, "unzipping models: assets/$MODELS_ZIP -> ${outDir.absolutePath}")
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
        Log.i(TAG, "unzip completed: ${outDir.absolutePath}")
        // Optionally validate a main model file
        val guess = outDir.listFiles()?.firstOrNull { it.name.endsWith(".onnx", ignoreCase = true) }
        if (guess == null) {
            Log.e(TAG, "ERR_MODEL_FILE_NOT_FOUND: 解壓後未找到 .onnx 檔案，請確認 models.zip 內容是否包含模型檔並重試。dir=${outDir.absolutePath}")
            return true
        }
        if (!expectedSha256.isNullOrBlank()) {
            val ok = validateSha256(guess, expectedSha256)
            Log.i(TAG, "validateModelChecksum=$ok file=${guess.name}")
            if (!ok) {
                Log.e(
                    TAG,
                    "ERR_MODEL_CHECKSUM_MISMATCH: 校驗失敗。建議：請重新下載模型資產或於系統設定→應用→清除資料後重試。file=${guess.absolutePath} expectedSha256=${expectedSha256.take(16)}..."
                )
            }
            return ok
        }
        // If .sha256 is present next to .onnx, use it
        val localSha = File(guess.parentFile, guess.name + ".sha256")
        if (localSha.exists()) {
            val exp = runCatching { localSha.readText().trim() }.getOrNull()
            if (!exp.isNullOrBlank()) {
                val ok = validateSha256(guess, exp)
                Log.i(TAG, "validateModelChecksum=$ok file=${guess.name}")
                if (!ok) {
                    Log.e(
                        TAG,
                        "ERR_MODEL_CHECKSUM_MISMATCH: 校驗失敗。建議：請重新下載模型資產或於系統設定→應用→清除資料後重試。file=${guess.absolutePath} expectedSha256=${exp.take(16)}..."
                    )
                }
                return ok
            }
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
