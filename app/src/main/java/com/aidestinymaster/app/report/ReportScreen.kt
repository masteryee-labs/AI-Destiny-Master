package com.aidestinymaster.app.report

import androidx.compose.material3.Card
import androidx.compose.foundation.background

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.AttachMoney
import kotlinx.coroutines.launch
import com.aidestinymaster.sync.ReportSyncBridge
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import com.aidestinymaster.billing.Entitlement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.aidestinymaster.app.R
import com.aidestinymaster.data.repository.ReportRepository
import com.aidestinymaster.app.paywall.PaywallSheet
import com.aidestinymaster.app.ads.RewardedAdDialog
import com.aidestinymaster.billing.EntitlementEvents
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aidestinymaster.app.ai.AiReportWorker
import com.aidestinymaster.data.prefs.UserPrefsRepository
import com.google.android.play.core.review.ReviewManagerFactory
import android.util.Log
import com.aidestinymaster.app.ui.DesignTokens
import com.aidestinymaster.app.ui.glassCardColors
import com.aidestinymaster.app.ui.glassModifier
import androidx.compose.material3.TextButton

@Composable
fun ReportScreen(activity: ComponentActivity, reportId: String) {
    val repo = ReportRepository.from(activity)
    val report by repo.getReportFlow(reportId).collectAsState(initial = null)
    val bridge = ReportSyncBridge(activity)
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val prefs = remember { UserPrefsRepository.from(ctx) }
    val reduceMotion by prefs.reduceMotionFlow.collectAsState(initial = false)
    val reviewCount by prefs.reviewSuccessCountFlow.collectAsState(initial = 0)
    val reviewPrompted by prefs.reviewPromptedFlow.collectAsState(initial = false)
    val favs by ReportPrefs.favsFlow(ctx).collectAsState(initial = emptySet())
    val notes by ReportPrefs.notesFlow(ctx).collectAsState(initial = emptyMap())
    var noteText by remember { mutableStateOf("") }
    LaunchedEffect(reportId, notes) { noteText = notes[reportId] ?: "" }
    val ent = remember { Entitlement.from(activity) }
    var vip by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { vip = ent.hasVip() }
    var showPaywall by remember { mutableStateOf(false) }
    var showRewarded by remember { mutableStateOf(false) }
    // 全域權益變更事件：恢復購買或其他頁購買成功時刷新 VIP 狀態
    LaunchedEffect(Unit) {
        EntitlementEvents.events.collect {
            val before = vip
            vip = Entitlement.from(activity).hasVip()
            if (!before && vip) {
                Toast.makeText(activity, "VIP 已解鎖，推送/拉取/分享已啟用", Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(Unit) { Log.i("AIDM", "ReportScreen start id=" + reportId) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(DesignTokens.Spacing.L.dp)
            .semantics { contentDescription = "report_screen_root" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(id = R.string.report), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(id = R.string.id_label, reportId))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(id = R.string.label_title, (report?.title ?: "-")))
                    Text(stringResource(id = R.string.summary) + " " + (report?.summary ?: "-"))
                }
            }
        }

        // 屬性列（Meta）
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Text(text = stringResource(id = R.string.meta_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(id = R.string.id_label, reportId))
                    Text(stringResource(id = R.string.label_updated_at, (report?.updatedAt ?: 0)))
                    val cref = report?.chartRef ?: "-"
                    Text(stringResource(id = R.string.label_chart_ref, cref))
                }
            }
        }
        val wm = remember { WorkManager.getInstance(activity) }
        val workInfos by wm.getWorkInfosByTagLiveData("ai_report_" + reportId).observeAsState(initial = emptyList())
        val firstInfo = workInfos.firstOrNull()
        val running = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        val sectionOptions = listOf(6, 8, 10, 12)
        var sectionsPreset by remember { mutableStateOf(8) }
        var menuExpanded by remember { mutableStateOf(false) }
        var showInfo by remember { mutableStateOf(false) }
        // 章節說明文案（供 Row 與展開區共用）
        val chaptersTipShort = stringResource(id = R.string.chapters_tip_short)
        val chaptersTipLong = stringResource(id = R.string.chapters_tip_long)
        // AI 進度與章節說明（預設收合，降低技術感）
        var showAiProgress by remember { mutableStateOf(false) }
        Row(
            Modifier.semantics {
                contentDescription = "report_actions"
            },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val aiGenDesc = stringResource(id = R.string.ai_generate)
            val cancelDesc = stringResource(id = R.string.cancel_generation)
            val chaptersLabel = stringResource(id = R.string.chapters_label, sectionsPreset)
            val chaptersTipShort = stringResource(id = R.string.chapters_tip_short)
            val chaptersTipLong = stringResource(id = R.string.chapters_tip_long)
            if (!running) {
                Button(
                    modifier = Modifier.semantics { contentDescription = aiGenDesc },
                    onClick = {
                    val chartRef = report?.chartRef?.let { listOf(it) } ?: emptyList()
                    AiReportWorker.enqueue(activity, reportId, chartRef, mode = "default", locale = "zh-TW", sectionsPreset = sectionsPreset)
                }) { Text(aiGenDesc) }
            } else {
                Button(
                    modifier = Modifier.semantics { contentDescription = cancelDesc },
                    onClick = { wm.cancelAllWorkByTag("ai_report_" + reportId) }
                ) { Text(cancelDesc) }
            }
            // 章節數選擇器
            Button(
                modifier = Modifier.semantics { contentDescription = chaptersLabel },
                onClick = { menuExpanded = true }
            ) { Text(chaptersLabel) }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                sectionOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(stringResource(id = R.string.chapters_option, opt)) }, onClick = {
                        sectionsPreset = opt
                        menuExpanded = false
                    })
                }
            }
            IconButton(onClick = { showInfo = !showInfo }) {
                Icon(Icons.Filled.Info, contentDescription = stringResource(id = R.string.desc_chapters_info))
            }
            // 小提示：預設章節數（收合於 AI 區塊內）
            TextButton(onClick = { showAiProgress = !showAiProgress }) {
                Text(if (showAiProgress) stringResource(id = R.string.hide_ai_details) else stringResource(id = R.string.show_ai_details))
            }
        }
        if (showAiProgress) {
            val st = firstInfo?.state?.name ?: "IDLE"
            val steps = firstInfo?.progress?.getInt(com.aidestinymaster.app.ai.AiReportWorker.KEY_PROGRESS_STEPS, 0) ?: 0
            val max = firstInfo?.progress?.getInt(com.aidestinymaster.app.ai.AiReportWorker.KEY_PROGRESS_MAX, 0) ?: 0
            val len = firstInfo?.progress?.getInt(com.aidestinymaster.app.ai.AiReportWorker.KEY_PROGRESS_CONTENT_LEN, 0) ?: (report?.contentEnc?.length ?: 0)
            val pct = if (max > 0) (steps * 100 / max) else 0
            Text(stringResource(id = R.string.ai_state_progress_len, st, pct, len))
            if (max > 0) {
                val raw = steps.toFloat() / max.toFloat()
                val anim by animateFloatAsState(targetValue = raw, label = "ai_progress")
                if (reduceMotion) {
                    val color = if (anim >= 0.8f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    LinearProgressIndicator(
                        progress = anim,
                        color = color,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp))
                    )
                } else {
                    // Stardust canvas: a thin band with twinkling particles moving by phase
                    val baseColor = if (anim >= 0.8f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .semantics(mergeDescendants = true) { contentDescription = "stardust_progress" }
                    ) {
                        val w = size.width
                        val h = size.height
                        // Background gradient
                        drawRect(
                            brush = Brush.horizontalGradient(listOf(baseColor.copy(alpha = 0.25f), baseColor.copy(alpha = 0.05f))),
                            size = size
                        )
                        // Progress fill
                        drawRect(
                            color = baseColor.copy(alpha = 0.35f),
                            size = androidx.compose.ui.geometry.Size(width = w * anim, height = h)
                        )
                        // Dust particles within progress area
                        val stars = 40
                        val phase = anim
                        for (i in 0 until stars) {
                            val fx = (i / stars.toFloat()) * (w * anim)
                            val jitter = ((i * 37) % 100) / 100f
                            val x = fx + (jitter - 0.5f) * 6f
                            val y = (h * ((i * 19 % 100) / 100f))
                            val r = 1.0f + 1.5f * ((i * 13 % 10) / 10f)
                            val twinkle = 0.5f + 0.5f * kotlin.math.abs(kotlin.math.sin((phase + i * 0.07f) * 6.283f))
                            drawCircle(color = Color.White.copy(alpha = 0.6f * twinkle), radius = r, center = Offset(x, y))
                        }
                    }
                }
            }
            run {
                val secDone = firstInfo?.progress?.getInt(com.aidestinymaster.app.ai.AiReportWorker.KEY_PROGRESS_SECTIONS_DONE, 0) ?: 0
                val secEst = firstInfo?.progress?.getInt(com.aidestinymaster.app.ai.AiReportWorker.KEY_PROGRESS_SECTIONS_EST, 0) ?: 0
                if (secEst > 0 || secDone > 0) {
                    val remain = (secEst - secDone).coerceAtLeast(0)
                    Text(stringResource(id = R.string.sections_progress, secDone, secEst, remain))
                }
            }
            // 章節說明（展開時顯示）
            Text(chaptersTipShort, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            if (showInfo) {
                Text(chaptersTipLong, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
        // Streaming content (contentEnc)
        val content = report?.contentEnc ?: ""
        // 內容摘要卡片
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp)) {
                    Text(text = stringResource(id = R.string.summary), style = MaterialTheme.typography.titleMedium)
                    val preview = (report?.summary?.takeIf { it.isNotBlank() } ?: content.take(240))
                    Text(preview.ifBlank { "-" })
                }
            }
        }
        // Key cards (career/love/health/finance)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.keycards_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { contentDescription = "key_cards_section" }
                    )
                    KeyCardsSection(summary = report?.summary ?: content.take(400))
                }
            }
        }
        if (content.isNotEmpty()) {
            Text(stringResource(id = R.string.content_live), style = MaterialTheme.typography.titleMedium)
            // 將內容切成段落卡片（以雙換行或長度切分）
            val parts = content.split("\n\n").filter { it.isNotBlank() }.take(12)
            parts.forEachIndexed { idx, para ->
                Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
                    Column(Modifier.padding(0.dp)) {
                        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.primary))
                        Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = stringResource(id = R.string.section_n, idx + 1), style = MaterialTheme.typography.titleSmall)
                            Text(text = para)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Row(
            Modifier.semantics {
                contentDescription = "report_share_and_fav"
            },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pushDesc = stringResource(id = R.string.push)
            val pullDesc = stringResource(id = R.string.pull)
            val shareDesc = stringResource(id = R.string.share)
            val unfavDesc = stringResource(id = R.string.unfavorite)
            val favDesc = stringResource(id = R.string.favorite)
            Button(
                modifier = Modifier.semantics { contentDescription = pushDesc },
                onClick = { scope.launch { bridge.push(reportId); status = "Pushed" } }, enabled = vip
            ) { Text(pushDesc) }
            Button(
                modifier = Modifier.semantics { contentDescription = pullDesc },
                onClick = { scope.launch { bridge.pull(reportId); status = "Pulled" } }, enabled = vip
            ) { Text(pullDesc) }
            val chooserTitle = ctx.getString(R.string.share)
            val reportFallback = ctx.getString(R.string.report)
            Button(modifier = Modifier.semantics { contentDescription = shareDesc }, onClick = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, report?.title ?: reportFallback)
                    putExtra(Intent.EXTRA_TEXT, report?.summary ?: "")
                }
                ctx.startActivity(Intent.createChooser(sendIntent, chooserTitle))
            }, enabled = vip) { Text(shareDesc) }
            Button(
                modifier = Modifier.semantics {
                    contentDescription = if (favs.contains(reportId)) unfavDesc else favDesc
                },
                onClick = { scope.launch { ReportPrefs.toggleFav(ctx, reportId) } }
            ) { Text(if (favs.contains(reportId)) unfavDesc else favDesc) }
        }
        if (!vip) {
            Text(stringResource(id = R.string.need_vip), color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPaywall = true }) { Text(stringResource(id = R.string.go_paywall)) }
                Button(onClick = { showRewarded = true }) { Text(stringResource(id = R.string.watch_ad)) }
            }
        }
        if (status.isNotEmpty()) Text(stringResource(id = R.string.status_label, status))
        OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text(stringResource(id = R.string.note)) })
        Button(onClick = { scope.launch { ReportPrefs.setNote(ctx, reportId, noteText) } }) { Text(stringResource(id = R.string.save_note)) }
    }
    // Inline sheets/dialogs
    PaywallSheet(
        activity = activity,
        visible = showPaywall,
        onDismiss = { showPaywall = false },
        onPurchased = {
            // 重新檢查 VIP 權益並關閉付費牆（suspend）
            scope.launch {
                val before = vip
                val nowVip = Entitlement.from(activity).hasVip()
                vip = nowVip
                if (!before && nowVip) {
                    Toast.makeText(activity, "VIP 已解鎖，推送/拉取/分享已啟用", Toast.LENGTH_SHORT).show()
                }
                showPaywall = false
            }
        }
    )
    RewardedAdDialog(activity = activity, visible = showRewarded, onDismiss = { showRewarded = false })

    // In-App Review: trigger after 3 successes, once
    LaunchedEffect(Unit) {
        // Observe work success and update review counter
    }
    val wm2 = remember { WorkManager.getInstance(activity) }
    val works by wm2.getWorkInfosByTagLiveData("ai_report_" + reportId).observeAsState(initial = emptyList())
    LaunchedEffect(works) {
        val success = works.any { it.state == WorkInfo.State.SUCCEEDED }
        if (success) {
            // increment and maybe prompt
            prefs.incrementReviewSuccessCount()
            val count = (reviewCount + 1)
            if (count >= 3 && !reviewPrompted) {
                runCatching {
                    val manager = ReviewManagerFactory.create(activity)
                    val request = manager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val info = task.result
                            manager.launchReviewFlow(activity, info).addOnCompleteListener {
                                // mark prompted regardless of success
                                scope.launch { prefs.setReviewPrompted(true) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyCardsSection(summary: String) {
    // TODO: real NLP parsing; for now, simple placeholders with summary context if available
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KeyCard(title = stringResource(id = R.string.key_card_career), tag = "key_card_career", hint = pickLine(summary, listOf("事業", "工作", "職涯")))
        KeyCard(title = stringResource(id = R.string.key_card_love), tag = "key_card_love", hint = pickLine(summary, listOf("情感", "感情", "關係")))
        KeyCard(title = stringResource(id = R.string.key_card_health), tag = "key_card_health", hint = pickLine(summary, listOf("健康", "身心")))
        KeyCard(title = stringResource(id = R.string.key_card_finance), tag = "key_card_finance", hint = pickLine(summary, listOf("財務", "金錢", "投資")))
    }
}

@Composable
private fun KeyCard(title: String, tag: String, hint: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(com.aidestinymaster.app.ui.DesignTokens.Radius.Card.dp))
            .padding(com.aidestinymaster.app.ui.DesignTokens.Spacing.M.dp)
            .semantics { contentDescription = tag },
        verticalArrangement = Arrangement.spacedBy(com.aidestinymaster.app.ui.DesignTokens.Spacing.S.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(com.aidestinymaster.app.ui.DesignTokens.Spacing.S.dp)) {
            val icon = when (tag) {
                "key_card_career" -> Icons.Filled.Work
                "key_card_love" -> Icons.Filled.Favorite
                "key_card_health" -> Icons.Filled.MedicalServices
                "key_card_finance" -> Icons.Filled.AttachMoney
                else -> null
            }
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
        Text(text = hint ?: stringResource(id = R.string.key_card_hint_fallback), style = MaterialTheme.typography.bodySmall)
    }
}

private fun pickLine(text: String, keywords: List<String>): String? {
    val lines = text.split('\n')
    return lines.firstOrNull { line -> keywords.any { kw -> line.contains(kw) } }
}
