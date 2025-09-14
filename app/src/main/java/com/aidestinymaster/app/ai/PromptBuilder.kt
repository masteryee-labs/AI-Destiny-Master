package com.aidestinymaster.app.ai

import android.content.Context

object PromptBuilder {
    data class Params(
        val chartIds: List<String>,
        val mode: String,
        val locale: String,
    )

    // TODO: stitch summaries from each module (BaZi/Ziwei/Astro/Design/etc.)
    fun buildPrompt(ctx: Context, params: Params): String {
        val header = when (params.locale) {
            "zh-TW" -> "你是一位嚴謹而中性的命理分析助手，請以繁體中文說明。"
            else -> "You are an impartial destiny analysis assistant."
        }
        val body = "Charts=" + params.chartIds.joinToString() + "\nMode=" + params.mode
        return header + "\n\n" + body + "\n\n" + "請輸出分段的小節與重點建議。"
    }
}
