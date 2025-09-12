package com.aidestinymaster.features.mix_ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aidestinymaster.data.repository.ReportRepository
import com.aidestinymaster.features.design.DesignSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

typealias ReportId = String

enum class MixMode { Quick, Deep }

/**
 * MixAnalysisViewModel
 * - 將各模組摘要整合為 MixedSummary
 * - 使用 PromptBuilder 生成 Prompt
 * - 待 :core:ai OnnxAiEngine 上線後，串流生成並寫入 ReportRepository
 * - 目前先將 Prompt 當作內容寫入，確保 E2E 流程可驗收
 */
class MixAnalysisViewModel(app: Application) : AndroidViewModel(app) {

    fun buildMixedFromDesign(design: DesignSummary): MixedSummary {
        return MixedCollector.collectSummaries(design = design)
    }

    /**
     * 生成綜合報告（暫以 Prompt 作為內容），返回 ReportId
     */
    fun requestAiReport(mixed: MixedSummary, locale: Locale = Locale.TAIWAN): LiveData<ReportId> {
        val live = MutableLiveData<ReportId>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = PromptBuilder.buildPrompt(mixed, locale)
                val repo = ReportRepository.from(getApplication())
                val id = repo.createFromAi(
                    type = "mix-ai",
                    chartId = "mixed",
                    content = prompt
                )
                live.postValue(id)
            } catch (t: Throwable) {
                live.postValue("")
            }
        }
        return live
    }

    /**
     * 任務規格要求的簽名：以 chartIds 與 mode 生成報告。
     * 目前先建立簡要內容，後續串接 :core:ai 推理與各模組資料聚合。
     */
    fun requestAiReport(chartIds: List<String>, mode: MixMode): LiveData<ReportId> {
        val live = MutableLiveData<ReportId>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val header = buildString {
                    appendLine("# Mix-AI Request")
                    appendLine("mode: ${mode.name}")
                    appendLine("chartIds: ${chartIds.joinToString()}")
                    appendLine()
                }
                val repo = ReportRepository.from(getApplication())
                val id = repo.createFromAi(
                    type = "mix-ai",
                    chartId = chartIds.firstOrNull() ?: "mixed",
                    content = header + "(內容將由 ONNX AI 引擎生成，佔位待更新)"
                )
                live.postValue(id)
            } catch (t: Throwable) {
                live.postValue("")
            }
        }
        return live
    }
}
