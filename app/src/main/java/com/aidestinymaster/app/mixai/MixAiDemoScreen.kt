package com.aidestinymaster.app.mixai

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidestinymaster.features.design.DesignViewModel
import com.aidestinymaster.features.mix_ai.MixAnalysisViewModel
import com.aidestinymaster.features.mix_ai.MixedCollector
import com.aidestinymaster.features.mix_ai.MixedSummary
import com.aidestinymaster.features.mix_ai.PromptBuilder
import com.aidestinymaster.features.ziwei.ZiweiCalculator
import com.aidestinymaster.features.ziwei.ZiweiSummary
import com.aidestinymaster.features.almanac.IchingEngine
import com.aidestinymaster.core.astro.AstroCalculator
import com.aidestinymaster.core.ai.OnnxAiEngine
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.OneTimeWorkRequestBuilder
import com.aidestinymaster.app.work.AiReportWorker
import androidx.compose.material3.OutlinedTextField
import java.util.Locale
import androidx.compose.material3.Card
import androidx.compose.foundation.background
import com.aidestinymaster.app.ui.DesignTokens
import androidx.compose.ui.graphics.Color

@Composable
fun MixAiDemoScreen(activity: ComponentActivity) {
    val designVm: DesignViewModel = viewModel(viewModelStoreOwner = activity)
    val mixVm: MixAnalysisViewModel = viewModel(viewModelStoreOwner = activity)
    var prompt by remember { mutableStateOf("") }
    var promptMixed by remember { mutableStateOf("") }
    val promptState = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var onnxOutput by remember { mutableStateOf("") }
    var onnxRunning by remember { mutableStateOf(false) }
    var checksumStatus by remember { mutableStateOf("") }
    var ortIoInfo by remember { mutableStateOf("") }
    var ortIoDetail by remember { mutableStateOf("") }
    var chartIdsText by remember { mutableStateOf("") }
    var modeText by remember { mutableStateOf("Quick") }
    var localeText by remember { mutableStateOf(java.util.Locale.TAIWAN.toLanguageTag()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.L.dp),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.M.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // 標題 + 以 Design 生成 Prompt（預覽）
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Text("Mix-AI Demo", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = {
                        val instant = java.time.Instant.ofEpochSecond(1_700_000_000)
                        designVm.updateFromStub(instant)
                        val ds = designVm.state.summary
                        if (ds != null) {
                            val mixed = mixVm.buildMixedFromDesign(ds)
                            val p = PromptBuilder.buildPrompt(mixed, Locale.TAIWAN)
                            promptState.value = p
                        }
                    }) { Text("使用目前 Design 產出 Prompt（預覽）") }
                }
            }
        }

        // 產生 Design Prompt、合併 Ziwei + Design
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                        Button(onClick = {
                            val summary = designVm.state.summary
                            if (summary != null) {
                                val mixed = MixedSummary(design = summary)
                                prompt = PromptBuilder.buildPrompt(mixed, Locale.TAIWAN)
                            }
                        }) { Text("產生 Design Prompt") }

                        Button(onClick = {
                            val d = designVm.state.summary
                            if (d != null) {
                                val birth = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Taipei")).minusYears(20)
                                val zChart = ZiweiCalculator.computeZiweiChart(birth)
                                val zSummary: ZiweiSummary = ZiweiCalculator.summarizeZiwei(zChart)
                                val now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Taipei"))
                                val hex = IchingEngine.castHexagramByTime(now)
                                val ich = IchingEngine.interpretHexagram(hex, activity)
                                val ichingBlock = buildString {
                                    appendLine(ich.title)
                                    ich.trend?.let { appendLine("trend: $it") }
                                    ich.notice?.let { appendLine("notice: $it") }
                                    if (ich.tips.isNotEmpty()) { appendLine("advice:"); ich.tips.forEach { t -> appendLine("- $t") } }
                                }.trim()
                                val instant = java.time.Instant.ofEpochSecond(1_700_000_000)
                                val lat = 25.04; val lon = 121.56
                                val planets = AstroCalculator.computePlanets(instant, lat, lon)
                                val aspects = AstroCalculator.computeAspects(planets)
                                val natalStub = buildString {
                                    appendLine("planets:")
                                    planets.longitudes.forEach { (name, deg) -> appendLine("- ${'$'}name: ${"%.1f".format(deg)}°") }
                                    appendLine("aspects:")
                                    aspects.take(8).forEach { a -> appendLine("- ${'$'}{a.p1}-${'$'}{a.p2} ${'$'}{a.type}° orb=${"%.1f".format(a.orb)}°") }
                                }.trim()
                                val mixed = MixedSummary(design = d, ziwei = zSummary, iching = ichingBlock, natal = com.aidestinymaster.features.mix_ai.NatalSummary(stub = natalStub))
                                promptMixed = PromptBuilder.buildPrompt(mixed, Locale.TAIWAN)
                            }
                        }) { Text("合併 Ziwei + Design 產出 Prompt") }
                    }
                }
            }
        }

        // ONNX 生成 / 模型校驗 / IO 列表 / IO 型別（分卡）
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Button(onClick = {
                        val base = when {
                            promptMixed.isNotBlank() -> promptMixed
                            prompt.isNotBlank() -> prompt
                            else -> "Hello from AIDestinyMaster"
                        }
                        val modelPath = java.io.File(activity.filesDir, "models/tinyllama-q8.onnx").absolutePath
                        val tokenizerPath = java.io.File(activity.filesDir, "models/tokenizer").absolutePath
                        val engine = OnnxAiEngine(activity, modelPath, tokenizerPath)
                        onnxOutput = ""; onnxRunning = true
                        scope.launch {
                            engine.generateStreaming(base, maxTokens = 200, temperature = 0.7f, topP = 0.9f) { chunk -> onnxOutput += chunk }
                            onnxRunning = false
                        }
                    }) { Text(if (onnxRunning) "ONNX 生成中…" else "使用本機 ONNX 引擎生成（Stub）") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Button(onClick = {
                        val model = java.io.File(activity.filesDir, "models/tinyllama-q8.onnx")
                        val shaFile = java.io.File(activity.filesDir, "models/tinyllama-q8.onnx.sha256")
                        val engine = OnnxAiEngine(activity, model.absolutePath, java.io.File(activity.filesDir, "models/tokenizer").absolutePath)
                        checksumStatus = if (model.exists() && shaFile.exists()) {
                            val exp = runCatching { shaFile.readText().trim() }.getOrNull().orEmpty()
                            if (exp.isNotBlank()) {
                                val ok = engine.validateModelChecksum(exp)
                                if (ok) "SHA 檢核通過" else "SHA 不一致"
                            } else "SHA 檔為空"
                        } else "模型或 SHA 檔不存在"
                    }) { Text("校驗模型 SHA") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Button(onClick = {
                        val modelPath = java.io.File(activity.filesDir, "models/tinyllama-q8.onnx").absolutePath
                        val engine = OnnxAiEngine(activity, modelPath, java.io.File(activity.filesDir, "models/tokenizer").absolutePath)
                        val info = engine.getSessionInfo()
                        ortIoInfo = if (info == null) "Session 尚未就緒（可能找不到 ONNX 檔)" else {
                            val (ins, outs) = info
                            buildString {
                                appendLine("Inputs:")
                                ins.forEach { name -> appendLine("- ${'$'}name") }
                                appendLine("Outputs:")
                                outs.forEach { name -> appendLine("- ${'$'}name") }
                            }
                        }
                    }) { Text("列出 ORT IO") }
                    if (ortIoInfo.isNotBlank()) Text(ortIoInfo, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Button(onClick = {
                        val modelPath = java.io.File(activity.filesDir, "models/tinyllama-q8.onnx").absolutePath
                        val engine = OnnxAiEngine(activity, modelPath, java.io.File(activity.filesDir, "models/tokenizer").absolutePath)
                        val info = engine.getSessionIOInfo()
                        ortIoDetail = if (info == null) "Session 尚未就緒（可能找不到 ONNX 檔)" else {
                            val (ins, outs) = info
                            buildString {
                                appendLine("Input Details:")
                                ins.forEach { (k, v) -> appendLine("- ${'$'}k = ${'$'}v") }
                                appendLine("Output Details:")
                                outs.forEach { (k, v) -> appendLine("- ${'$'}k = ${'$'}v") }
                            }
                        }
                    }) { Text("列出 IO 型別與維度") }
                    if (ortIoDetail.isNotBlank()) Text(ortIoDetail, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // WorkManager 排程
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Text("排程背景生成（WorkManager）", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = chartIdsText, onValueChange = { chartIdsText = it }, label = { Text("chartIds（以逗號分隔）") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                        OutlinedTextField(value = modeText, onValueChange = { modeText = it }, label = { Text("mode") })
                        OutlinedTextField(value = localeText, onValueChange = { localeText = it }, label = { Text("locale tag") })
                    }
                    Button(onClick = {
                        val ids = chartIdsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                        val data = workDataOf(
                            AiReportWorker.KEY_CHART_IDS to ids.toTypedArray(),
                            AiReportWorker.KEY_MODE to modeText,
                            AiReportWorker.KEY_LOCALE to localeText
                        )
                        val req = OneTimeWorkRequestBuilder<AiReportWorker>().setInputData(data).build()
                        WorkManager.getInstance(activity).enqueue(req)
                    }) { Text("排程背景生成") }
                }
            }
        }

        // Prompt 預覽
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Text("Prompt 預覽：", style = MaterialTheme.typography.titleMedium)
                    Box(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Text(promptState.value.ifBlank { "尚未產生，請點擊上方按鈕" })
                    }
                    if (prompt.isNotBlank()) {
                        Text("Design Prompt 預覽：", style = MaterialTheme.typography.titleSmall)
                        Text(prompt, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                    if (promptMixed.isNotBlank()) {
                        Text("Mixed Prompt 預覽（Ziwei + Design）：", style = MaterialTheme.typography.titleSmall)
                        Text(promptMixed, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                    if (onnxOutput.isNotBlank()) {
                        Text("ONNX Stub 串流輸出：", style = MaterialTheme.typography.titleSmall)
                        Text(onnxOutput, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                    if (checksumStatus.isNotBlank()) {
                        Text("模型校驗：${'$'}checksumStatus", style = MaterialTheme.typography.bodySmall)
                    }
                    if (ortIoInfo.isNotBlank()) {
                        Text("ORT IO：", style = MaterialTheme.typography.titleSmall)
                        Text(ortIoInfo, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                    if (ortIoDetail.isNotBlank()) {
                        Text("ORT IO 型別與維度：", style = MaterialTheme.typography.titleSmall)
                        Text(ortIoDetail, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
