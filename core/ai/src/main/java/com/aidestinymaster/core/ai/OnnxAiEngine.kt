package com.aidestinymaster.core.ai

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import kotlin.random.Random

/**
 * OnnxAiEngine skeleton. Real ONNX Runtime integration can be added later.
 */
class OnnxAiEngine(
    private val context: Context,
    private val modelPath: String,
    private val tokenizerPath: String
) {
    private val env: OrtEnvironment? = runCatching { OrtEnvironment.getEnvironment() }.getOrNull()
    private val session: OrtSession? = runCatching {
        val f = File(modelPath)
        if (!f.exists()) null else env?.createSession(f.absolutePath, OrtSession.SessionOptions())
    }.getOrNull()

    /**
     * Streaming generation stub: immediately echoes small chunks from the prompt.
     * Replace with ONNX Runtime decoder-only loop later.
     */
    fun generateStreaming(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        onChunk: (String) -> Unit
    ): Result<String> {
        // If session is available, attempt an auto-detected decoder-only token loop.
        session?.let { sess ->
            val schema = detectSchema(sess)
            if (schema != null) {
                return runCatching { generateTokensLoop(sess, schema, prompt, maxTokens, temperature, topP, onChunk) }
                    .recoverCatching {
                        // If anything fails, fallback to stub streaming
                        val chunks = prompt.chunked(64).take(maxTokens.coerceAtMost(16))
                        val acc = StringBuilder()
                        for (c in chunks) { onChunk(c); acc.append(c) }
                        acc.toString()
                    }
            }
        }
        // No session: fallback stub
        return runCatching {
            val chunks = prompt.chunked(64).take(maxTokens.coerceAtMost(8))
            val acc = StringBuilder()
            for (c in chunks) {
                onChunk(c)
                acc.append(c)
            }
            acc.toString()
        }
    }

    /**
     * Validate model checksum by computing SHA-256 on the provided modelPath.
     */
    fun validateModelChecksum(expectedSha256: String): Boolean {
        val f = File(modelPath)
        if (!f.exists()) return false
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(f).use { fis ->
            val buf = ByteArray(8192)
            while (true) {
                val n = fis.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        val digest = md.digest().joinToString("") { "%02x".format(it) }
        return digest.equals(expectedSha256.trim(), ignoreCase = true)
    }

    /** Debug: return session input/output names if available. */
    fun getSessionInfo(): Pair<Set<String>, Set<String>>? {
        val s = session ?: return null
        return s.inputNames to s.outputNames
    }

    /** Debug: return input name -> info string, and output name -> info string. */
    fun getSessionIOInfo(): Pair<Map<String, String>, Map<String, String>>? {
        val s = session ?: return null
        val inMap = s.inputNames.associateWith { name -> s.inputInfo[name].toString() }
        val outMap = s.outputNames.associateWith { name -> s.outputInfo[name].toString() }
        return inMap to outMap
    }

    // ---- Internal helpers for auto-detect loop ----

    private data class DetectedSchema(
        val inputIds: String,
        val attentionMask: String?,
        val logits: String,
        val pastKeys: List<String>,
        val pastValues: List<String>,
        val presentKeys: List<String>,
        val presentValues: List<String>,
        val idsTypeInt64: Boolean
    )

    private fun detectSchema(sess: OrtSession): DetectedSchema? {
        val ins = sess.inputNames
        val outs = sess.outputNames
        // Heuristic matching
        val ids = ins.firstOrNull { it.equals("input_ids", true) || it.contains("input_ids", true) }
            ?: ins.firstOrNull { sess.inputInfo[it].toString().contains("INT64") && sess.inputInfo[it].toString().contains("[1, ") }
        val attn = ins.firstOrNull { it.equals("attention_mask", true) || it.contains("attention_mask", true) }
        val logits = outs.firstOrNull { it.equals("logits", true) || it.contains("logits", true) }
            ?: outs.firstOrNull()
        if (ids == null || logits == null) return null
        // KV-cache names (optional)
        val pastK = ins.filter { it.contains("past_key_values", true) && it.contains("key", true) }
        val pastV = ins.filter { it.contains("past_key_values", true) && it.contains("value", true) }
        val presentK = outs.filter { it.contains("present", true) && it.contains("key", true) }
        val presentV = outs.filter { it.contains("present", true) && it.contains("value", true) }
        val infoStr = sess.inputInfo[ids]?.toString() ?: ""
        val ids64 = infoStr.contains("INT64")
        return DetectedSchema(ids, attn, logits, pastK, pastV, presentK, presentV, ids64)
    }

    private fun generateTokensLoop(
        sess: OrtSession,
        schema: DetectedSchema,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        onChunk: (String) -> Unit
    ): String {
        val tokenizer = Tokenizer(tokenizerPath) // NOTE: stub; replace with real
        val inputTokens = tokenizer.encode(prompt).map { it.toLong() }.toMutableList()
        val generated = StringBuilder()

        var pastK: List<OnnxTensor>? = null
        var pastV: List<OnnxTensor>? = null

        fun idsTensor(ids: LongArray): OnnxTensor {
            val data = arrayOf(ids) // shape [1, seq]
            return OnnxTensor.createTensor(env, data)
        }

        fun maskTensor(len: Int): OnnxTensor {
            val arr = LongArray(len) { 1L }
            val data = arrayOf(arr)
            return OnnxTensor.createTensor(env, data)
        }

        var steps = 0
        while (steps < maxTokens) {
            val feeds = HashMap<String, OnnxTensor>()
            val idsArr = if (steps == 0) inputTokens.toLongArray() else longArrayOf(inputTokens.last())
            val idsT = idsTensor(idsArr)
            feeds[schema.inputIds] = idsT
            val closeList = mutableListOf(idsT)

            schema.attentionMask?.let {
                val m = maskTensor(idsArr.size)
                feeds[it] = m
                closeList.add(m)
            }

            // Attach past if available (not implemented without shapes; rely on model optional past)
            // Some models allow first step without past; we skip past feeds for portability.

            sess.run(feeds).use { res ->
                // Get logits
                val logitsAny = res.get(schema.logits)?.value
                val nextId = when (logitsAny) {
                    is Array<*> -> {
                        // Expect shape [1, 1, vocab]; flatten last
                        val last = (logitsAny.lastOrNull() as? Array<*>)?.lastOrNull()
                        val arr = (last as? FloatArray) ?: FloatArray(0)
                        sampleFromLogits(arr, temperature, topP)
                    }
                    is FloatArray -> sampleFromLogits(logitsAny, temperature, topP)
                    else -> 0
                }
                inputTokens.add(nextId.toLong())
                val text = tokenizer.decode(intArrayOf(nextId))
                onChunk(text)
                generated.append(text)
            }

            // Close tensors
            closeList.forEach { it.close() }
            steps++
        }
        return generated.toString()
    }

    private fun sampleFromLogits(logits: FloatArray, temperature: Float, topP: Float): Int {
        if (logits.isEmpty()) return 0
        val temp = temperature.coerceAtLeast(1e-3f)
        val scaled = FloatArray(logits.size) { i -> logits[i] / temp }
        val probs = softmax(scaled)
        return sampleTopP(probs, topP)
    }

    private fun softmax(x: FloatArray): FloatArray {
        var max = x.maxOrNull() ?: 0f
        val exps = FloatArray(x.size) { i -> kotlin.math.exp((x[i] - max).toDouble()).toFloat() }
        val sum = exps.sum().coerceAtLeast(1e-6f)
        for (i in exps.indices) exps[i] /= sum
        return exps
    }

    private fun sampleTopP(probs: FloatArray, topP: Float): Int {
        val p = topP.coerceIn(0.01f, 1f)
        val idx = probs.indices.sortedByDescending { probs[it] }
        var cum = 0f
        val cut = mutableListOf<Int>()
        for (i in idx) {
            cut.add(i)
            cum += probs[i]
            if (cum >= p) break
        }
        val norm = cut.sumOf { probs[it].toDouble() }.toFloat().coerceAtLeast(1e-6f)
        var r = Random.nextFloat()
        for (i in cut) {
            val q = probs[i] / norm
            if (r <= q) return i
            r -= q
        }
        return cut.lastOrNull() ?: 0
    }
}
