package com.aidestinymaster.features.mix_ai

import com.aidestinymaster.features.design.DesignSummary
import com.aidestinymaster.features.ziwei.ZiweiSummary
import com.aidestinymaster.features.almanac.IchingInterpretation
import com.aidestinymaster.core.lunar.AlmanacEngine.AlmanacDay

/**
 * 綜合匯總資料模型：可逐步擴充（八字、紫微、西洋盤、易經、黃曆）。
 * 目前先整合 DesignSummary，避免阻塞其他模組未完成時的編譯。
 */

data class BaziSummary(val stub: String = "")

data class NatalSummary(val stub: String = "")

data class MixedSummary(
    val design: DesignSummary? = null,
    val bazi: BaziSummary? = null,
    val ziwei: ZiweiSummary? = null,
    val natal: NatalSummary? = null,
    val iching: String? = null,
    val almanac: String? = null
)

object MixedCollector {
    /**
     * 任務規格要求的簽名版本：將各模組摘要整合為 MixedSummary。
     */
    fun collectSummaries(
        bazi: BaziSummary,
        ziwei: ZiweiSummary,
        natal: NatalSummary,
        design: DesignSummary,
        iching: IchingInterpretation?,
        almanac: AlmanacDay?
    ): MixedSummary {
        val ichingBlock = iching?.let { i ->
            buildString {
                appendLine(i.title)
                i.trend?.let { appendLine("trend: $it") }
                i.notice?.let { appendLine("notice: $it") }
                if (i.tips.isNotEmpty()) {
                    appendLine("advice:")
                    i.tips.forEach { t -> appendLine("- $t") }
                }
            }.trim()
        }
        val almanacBlock = almanac?.let { a ->
            buildString {
                appendLine("date: ${a.date}")
                a.solarTerm?.let { appendLine("solarTerm: $it") }
                a.ganzhiYear?.let { appendLine("ganzhiYear: $it") }
                if (a.yi.isNotEmpty()) appendLine("yi: ${a.yi.joinToString()}")
                if (a.ji.isNotEmpty()) appendLine("ji: ${a.ji.joinToString()}")
                a.zodiacAnimal?.let { appendLine("zodiac: $it") }
            }.trim()
        }
        return MixedSummary(
            design = design,
            bazi = bazi,
            ziwei = ziwei,
            natal = natal,
            iching = ichingBlock,
            almanac = almanacBlock
        )
    }

    fun collectSummaries(
        design: DesignSummary? = null,
        bazi: BaziSummary? = null,
        ziwei: ZiweiSummary? = null,
        natal: NatalSummary? = null,
        iching: String? = null,
        almanac: String? = null
    ): MixedSummary {
        return MixedSummary(
            design = design,
            bazi = bazi,
            ziwei = ziwei,
            natal = natal,
            iching = iching,
            almanac = almanac
        )
    }
}
