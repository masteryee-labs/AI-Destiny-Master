package com.aidestinymaster.app.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aidestinymaster.app.R
import com.aidestinymaster.app.util.AppLogger
import com.aidestinymaster.core.ai.OnnxAiEngine
import com.aidestinymaster.data.repository.ReportRepository
import kotlinx.coroutines.runBlocking

class AiReportWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): ListenableWorker.Result {
        val chartIds = inputData.getStringArray(KEY_CHART_IDS)?.toList().orEmpty()
        val mode = inputData.getString(KEY_MODE) ?: "default"
        val locale = inputData.getString(KEY_LOCALE) ?: "zh-TW"
        val reportId = inputData.getString(KEY_REPORT_ID) ?: return ListenableWorker.Result.failure()
        val presetSections = inputData.getInt(KEY_SECTIONS_PRESET, PRESET_SECTIONS)

        AppLogger.log(applicationContext, "AiReportWorker start reportId=$reportId charts=${chartIds.joinToString()}")
        val params = PromptBuilder.Params(chartIds = chartIds, mode = mode, locale = locale)
        val prompt = PromptBuilder.buildPrompt(applicationContext, params)

        val engine = OnnxAiEngine(
            context = applicationContext,
            modelPath = applicationContext.filesDir.resolve("models/model.onnx").absolutePath,
            tokenizerPath = applicationContext.filesDir.resolve("models/tokenizer.json").absolutePath
        )

        val sb = StringBuilder()
        var steps = 0
        val maxSteps = 512 // 以 maxTokens 為上限做較合理的百分比
        // 使用與引擎相同 tokenizer 估算 chunk 的 token 數
        val progressTokenizer = com.aidestinymaster.core.ai.Tokenizer(
            applicationContext.filesDir.resolve("models/tokenizer.json").absolutePath
        )
        val res = engine.generateStreaming(
            prompt = prompt,
            maxTokens = 512,
            temperature = 0.8f,
            topP = 0.95f
        ) { chunk ->
            // append chunk to report contentEnc progressively
            sb.append(chunk)
            runBlocking {
                val repo = ReportRepository.from(applicationContext)
                val cur = repo.getOnce(reportId)
                if (cur != null) {
                    val newContent = (cur.contentEnc ?: "") + chunk
                    repo.upsert(cur.copy(contentEnc = newContent, updatedAt = System.currentTimeMillis()))
                    // 進度上報：以 chunk 的 token 數估算步進
                    val inc = runCatching { progressTokenizer.encode(chunk).size }.getOrDefault(1)
                    steps = (steps + inc).coerceAtMost(maxSteps)
                    // 以段落為單位估算（偵測雙換行/標題符號/項目符號）
                    val sectionsDone = countSections(newContent)
                    val explicitCnt = countExplicitSectionMarkers(newContent)
                    val sectionsEst = if (explicitCnt > 0) presetSections else estimateTotalSections(sectionsDone, steps, maxSteps)
                    val progress = androidx.work.Data.Builder()
                        .putInt(KEY_PROGRESS_STEPS, steps)
                        .putInt(KEY_PROGRESS_MAX, maxSteps)
                        .putInt(KEY_PROGRESS_CONTENT_LEN, newContent.length)
                        .putInt(KEY_PROGRESS_SECTIONS_DONE, sectionsDone)
                        .putInt(KEY_PROGRESS_SECTIONS_EST, sectionsEst)
                        .build()
                    try { setProgressAsync(progress) } catch (_: Throwable) { }
                }
            }
        }

        val full = res.getOrElse { t ->
            AppLogger.log(applicationContext, "AiReportWorker failed: ${t.message}")
            return ListenableWorker.Result.retry()
        }

        // Finalize summary/title if needed (skipped; assume another component may update)
        notifyDone(reportId)
        AppLogger.log(applicationContext, "AiReportWorker done reportId=$reportId len=${full.length}")
        // 完成時上報 100%
        try {
            val progress = androidx.work.Data.Builder()
                .putInt(KEY_PROGRESS_STEPS, maxSteps)
                .putInt(KEY_PROGRESS_MAX, maxSteps)
                .putInt(KEY_PROGRESS_CONTENT_LEN, full.length)
                .putInt(KEY_PROGRESS_SECTIONS_DONE, countSections(full))
                .putInt(KEY_PROGRESS_SECTIONS_EST, if (countExplicitSectionMarkers(full) > 0) presetSections else countSections(full))
                .build()
            setProgressAsync(progress)
        } catch (_: Throwable) { }
        return ListenableWorker.Result.success()
    }

    private fun notifyDone(reportId: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CH_ID, "AI Tasks", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(ch)
        }
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("aidm://report/" + reportId))
        val pi = PendingIntent.getActivity(
            applicationContext,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val notif = NotificationCompat.Builder(applicationContext, CH_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("AI 生成完成")
            .setContentText("報告已更新：$reportId")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
    }

    companion object {
        private const val CH_ID = "ai_tasks"
        const val KEY_CHART_IDS = "chartIds"
        const val KEY_MODE = "mode"
        const val KEY_LOCALE = "locale"
        const val KEY_REPORT_ID = "reportId"
        const val KEY_PROGRESS_STEPS = "progress_steps"
        const val KEY_PROGRESS_MAX = "progress_max"
        const val KEY_PROGRESS_CONTENT_LEN = "progress_content_len"
        const val KEY_PROGRESS_SECTIONS_DONE = "progress_sections_done"
        const val KEY_PROGRESS_SECTIONS_EST = "progress_sections_est"
        const val KEY_SECTIONS_PRESET = "sections_preset"
        const val PRESET_SECTIONS = 8

        fun enqueue(context: Context, reportId: String, chartIds: List<String>, mode: String, locale: String, sectionsPreset: Int = PRESET_SECTIONS): String {
            val data = Data.Builder()
                .putStringArray(KEY_CHART_IDS, chartIds.toTypedArray())
                .putString(KEY_MODE, mode)
                .putString(KEY_LOCALE, locale)
                .putString(KEY_REPORT_ID, reportId)
                .putInt(KEY_SECTIONS_PRESET, sectionsPreset)
                .build()
            val req = OneTimeWorkRequestBuilder<AiReportWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, java.time.Duration.ofSeconds(10))
                .addTag("ai_report_" + reportId)
                .build()
            WorkManager.getInstance(context).enqueue(req)
            return req.id.toString()
        }
    }
}

private fun countSections(text: String): Int {
    if (text.isBlank()) return 0
    // Recognized markers (if model outputs explicit tokens/headings):
    // - Explicit tokens: <|section|>, <|end_section|>, <section>, [/section]
    // - Markdown headings: ^#{1,6} 
    // - CJK title brackets: 【...】/〈...〉/《...》 lines
    // - Numbered headings: ^第[一二三四五六七八九十]{1,3}[章節篇部分][：:、.\s]
    // - Bullet or divider: ^[-*•]\s, or blank-line-separated paragraphs
    val explicit = Regex("(<\\|section\\|>|<\\|end_section\\|>|<section>|</section>|\\[/?section])", RegexOption.IGNORE_CASE)
    val heading = Regex("^(#{1,6}\\s)|(^【.+】$)|(^〈.+〉$)|(^《.+》$)|(^第[一二三四五六七八九十百千]+[章節篇部分][：:、.\\s])", setOf(RegexOption.MULTILINE))
    val bullet = Regex("(^[-*•]\\s)", setOf(RegexOption.MULTILINE))

    val markers = mutableListOf<Int>()
    explicit.findAll(text).forEach { markers += it.range.first }
    heading.findAll(text).forEach { markers += it.range.first }
    bullet.findAll(text).forEach { markers += it.range.first }

    val paragraphs = text.split(Regex("\n\n+"))
    var count = 0
    if (markers.isNotEmpty()) {
        // Count distinct marker starts, group by proximity (within 40 chars)
        markers.sort()
        var groups = 0
        var last = -999
        for (p in markers) {
            if (p - last > 40) { groups++; last = p }
        }
        count = groups
    } else {
        // Fallback: count paragraphs with reasonable length
        count = paragraphs.count { it.trim().length >= 60 }
    }
    return count
}

private fun countExplicitSectionMarkers(text: String): Int {
    if (text.isBlank()) return 0
    val explicit = Regex("(<\\|section\\|>|<\\|end_section\\|>|<section>|</section>|\\[/?section])", RegexOption.IGNORE_CASE)
    val list = explicit.findAll(text).map { it.range.first }.sorted().toList()
    if (list.isEmpty()) return 0
    // 合併相近標記（40字內視為同一章節開頭）
    var groups = 0
    var last = -999
    for (p in list) {
        if (p - last > 40) { groups++; last = p }
    }
    return groups
}

private fun estimateTotalSections(done: Int, steps: Int, max: Int): Int {
    if (done <= 0) return 6 // 初期預估為 6 段
    val pct = if (max > 0) steps.toFloat() / max else 0f
    val denom = if (pct < 0.25f) 0.25f else pct
    val est = kotlin.math.ceil(done / denom).toInt().coerceAtLeast(done)
    // Clamp to 6..12 段，貼近產品預期章節數
    return est.coerceIn(6, 12)
}
