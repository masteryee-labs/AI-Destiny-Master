package com.aidestinymaster.app

import android.app.Application
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.io.FileInputStream
import java.security.MessageDigest
import com.aidestinymaster.core.ai.ModelInstaller

class AIDMApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            Log.i(TAG, "onCreate start, filesDir=${filesDir.absolutePath}")
            // Write a marker to verify we can inspect app internal storage via run-as
            try {
                File(filesDir, "init_marker.txt").writeText(System.currentTimeMillis().toString())
            } catch (e: Throwable) {
                Log.w(TAG, "write init_marker failed: ${e.message}")
            }
            // Prefer centralized installer with logging
            val ok = runCatching { ModelInstaller.installIfNeeded(this) }.getOrDefault(false)
            Log.i(TAG, "ModelInstaller.installIfNeeded from App ok=$ok")
            if (!ok) {
                // Fallback to legacy unzip path
                initModelsOnce()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "initModelsOnce failed: ${t.message}", t)
        }
    }

    private fun initModelsOnce() {
        val modelsDir = File(filesDir, "models")
        val onnx = File(modelsDir, "tinyllama-q8.onnx")
        val shaFile = File(modelsDir, "tinyllama-q8.onnx.sha256")

        if (!onnx.exists()) {
            val start = System.currentTimeMillis()
            try {
                // verify asset is readable
                assets.open("models.zip").use { test ->
                    val probe = ByteArray(16)
                    val n = test.read(probe)
                    Log.i(TAG, "assets/models.zip probe bytes=$n")
                }
                unzipFromAssets("models.zip", modelsDir)
            } catch (e: Throwable) {
                Log.e(TAG, "unzip error: ${e.message}", e)
            }
            val dur = System.currentTimeMillis() - start
            Log.i(TAG, "unzip done to ${modelsDir.absolutePath} in ${dur}ms")
        } else {
            Log.i(TAG, "onnx exists, skip unzip: ${onnx.length()} bytes")
        }

        if (onnx.exists() && shaFile.exists()) {
            val expected = shaFile.readText(Charsets.UTF_8).trim()
            val actual = sha256(onnx)
            val ok = expected.equals(actual, ignoreCase = true)
            Log.i(TAG, "checksum expected=$expected actual=$actual validate=$ok")
        } else {
            Log.w(TAG, "onnx or sha not found after unzip: onnx=${onnx.exists()} sha=${shaFile.exists()}")
        }
    }

    private fun unzipFromAssets(assetZipName: String, destDir: File) {
        if (!destDir.exists()) destDir.mkdirs()
        assets.open(assetZipName).use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(DEFAULT_BUF)
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var len = zis.read(buffer)
                            while (len > 0) {
                                fos.write(buffer, 0, len)
                                len = zis.read(buffer)
                            }
                            fos.flush()
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    companion object {
        private const val TAG = "AIDMModelInit"
        private const val DEFAULT_BUF = 8 * 1024
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(16 * 1024)
        FileInputStream(file).use { fis ->
            var read = fis.read(buf)
            while (read > 0) {
                md.update(buf, 0, read)
                read = fis.read(buf)
            }
        }
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }
}
