package com.aidestinymaster.features.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * 簡化版 BodyGraph：
 * - 中軸與九個節點（以圓點代表），非官方圖形，僅作為中性可讀 MVP。
 * - 依 GateAssignments 高亮部分節點（以隨機門→節點映射的穩定函數），避免侵權圖樣。
 */
@Composable
fun BodyGraph(
    viewModel: DesignViewModel,
    modifier: Modifier = Modifier,
    theme: NodeColorTheme = NodeColorTheme.Vitality,
    enableLiveReload: Boolean = false,
    channelsAssetName: String = "abstract_channels.json",
    reduceMotion: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onBackground = MaterialTheme.colorScheme.onBackground

    val assignments = viewModel.state.assignments

    // 互動狀態：縮放、平移、選取節點
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selectedNode by remember { mutableStateOf<Int?>(null) }

    // 共用對映：gate -> node index（0..8）
    val nodeIndexByGate = { gate: Int -> (gate - 1) % 9 }

    // 外部抽象通道（assets/abstract_channels.json）
    val context = LocalContext.current
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    LaunchedEffect(enableLiveReload, channelsAssetName) {
        while (true) {
            channels = loadAbstractChannels(context, channelsAssetName) ?: defaultChannels()
            if (!enableLiveReload) break
            delay(2000)
        }
    }

    // 簡單滑入動畫（初始 alpha 漸顯）；若減少動效，則以 0ms 套用
    val appearAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 400),
        label = "appear"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics { this.contentDescription = "bodygraph_canvas" }
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    pan += panChange
                    scale = (scale * zoomChange).coerceIn(0.6f, 2.5f)
                }
            }
            .pointerInput(assignments) {
                detectTapGestures { pos ->
                    // 將螢幕座標轉回內容座標（反向套用 pan/scale）
                    val contentX = (pos.x - pan.x) / scale
                    val contentY = (pos.y - pan.y) / scale
                    val h = size.height
                    val w = size.width
                    val cx = w / 2f
                    val nodes = listOf(
                        Offset(cx, h * 0.12f),
                        Offset(cx, h * 0.20f),
                        Offset(cx, h * 0.28f),
                        Offset(cx, h * 0.36f),
                        Offset(cx, h * 0.48f),
                        Offset(cx, h * 0.58f),
                        Offset(cx, h * 0.68f),
                        Offset(cx, h * 0.78f),
                        Offset(cx, h * 0.88f)
                    )
                    val pt = Offset(contentX, contentY)
                    val hit = nodes.withIndex().minByOrNull { (_, p) -> (p - pt).getDistance() }
                    if (hit != null && (nodes[hit.index] - pt).getDistance() <= 28f) {
                        selectedNode = hit.index
                    } else {
                        selectedNode = null
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val top = h * 0.1f
        val bottom = h * 0.9f

        // 套用平移與縮放
        drawContext.transform.translate(pan.x, pan.y)
        drawContext.transform.scale(scale, scale, pivot = Offset(cx, h / 2f))

        // 中軸
        drawLine(
            color = onBackground.copy(alpha = 0.3f * appearAlpha),
            start = Offset(cx, top),
            end = Offset(cx, bottom),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // 九個節點位置（頭頂到根部，僅抽象點位，非官方形狀）
        val nodes = listOf(
            Offset(cx, h * 0.12f), // N1
            Offset(cx, h * 0.20f), // N2
            Offset(cx, h * 0.28f), // N3
            Offset(cx, h * 0.36f), // N4
            Offset(cx, h * 0.48f), // N5
            Offset(cx, h * 0.58f), // N6
            Offset(cx, h * 0.68f), // N7
            Offset(cx, h * 0.78f), // N8
            Offset(cx, h * 0.88f)  // N9
        )

        // 節點連線（主幹）
        for (i in 0 until nodes.lastIndex) {
            drawLine(
                color = onBackground.copy(alpha = 0.2f * appearAlpha),
                start = nodes[i],
                end = nodes[i + 1],
                strokeWidth = 2f
            )
        }

        // 將活躍閘門映射到節點索引（穩定函數：gate % 9）
        val highlighted = assignments?.activeGates?.map(nodeIndexByGate)?.toSet() ?: emptySet()

        // 活躍強度：統計每個節點上的行星數量
        val activityCounts = IntArray(9)
        assignments?.planetToGate?.forEach { (_, g) ->
            val idx = nodeIndexByGate(g)
            if (idx in 0..8) activityCounts[idx]++
        }

        // 額外：將相同 gate%9 的行星以細線相互連結（抽象連線，避免具體官方形狀）
        assignments?.planetToGate
            ?.entries
            ?.groupBy { nodeIndexByGate(it.value) }
            ?.forEach { (nodeIndex, entries) ->
                val center = nodes[nodeIndex]
                // 為每個行星畫小射線表示有連結
                entries.forEachIndexed { idx, _ ->
                    val angle = (idx / (entries.size.coerceAtLeast(1)).toFloat()) * (Math.PI * 2).toFloat()
                    val end = Offset(
                        x = center.x + 18f * kotlin.math.cos(angle),
                        y = center.y + 18f * kotlin.math.sin(angle)
                    )
                    drawLine(
                        color = primary.copy(alpha = 0.35f * appearAlpha),
                        start = center,
                        end = end,
                        strokeWidth = 2f
                    )
                }
            }

        // 抽象通道（自訂、非官方）：以部分垂直跨節點連線呈現。若兩端皆有活躍，以較粗且更亮顯示。
        channels.forEachIndexed { idx, ch ->
            val a = ch.a; val b = ch.b
            if (a in nodes.indices && b in nodes.indices) {
                val bothActive = activityCounts[a] > 0 && activityCounts[b] > 0
                val baseColor = ch.color ?: (if (bothActive) primary else onBackground)
                val baseWidth = ch.width ?: if (bothActive) 4f else 2f
                // 簡單序列化淡入：依通道索引分段提升
                val seqAlpha = ((idx + 1).toFloat() / (channels.size.coerceAtLeast(1))).coerceIn(0.15f, 1f)
                val alpha = (if (bothActive) 0.55f else 0.18f) * appearAlpha * seqAlpha
                drawLine(
                    color = baseColor.copy(alpha = alpha),
                    start = nodes[a],
                    end = nodes[b],
                    strokeWidth = baseWidth
                )
            }
        }

        // 畫節點（顏色強度與半徑依活躍數量）
        nodes.forEachIndexed { index, p ->
            val active = highlighted.contains(index)
            val count = activityCounts[index]
            val intensity = (0.25f + (count * 0.12f)).coerceIn(0.25f, 0.85f)
            val nodeColor = when (theme) {
                NodeColorTheme.Vitality -> if (active) primary.copy(alpha = intensity) else secondary.copy(alpha = 0.25f + count * 0.08f)
                NodeColorTheme.Stability -> if (active) Color(0xFF4CAF50).copy(alpha = intensity) else Color(0xFF8BC34A).copy(alpha = 0.25f + count * 0.08f)
                NodeColorTheme.Insight -> if (active) Color(0xFF3F51B5).copy(alpha = intensity) else Color(0xFF9FA8DA).copy(alpha = 0.25f + count * 0.08f)
            }
            val radius = (12f + count * 2f).coerceAtMost(22f)
            // 節點序列化淡入：自上而下逐步提高
            val seqAlpha = ((index + 1).toFloat() / nodes.size).coerceIn(0.15f, 1f)
            drawCircle(
                color = nodeColor.copy(alpha = nodeColor.alpha * appearAlpha * seqAlpha),
                radius = radius,
                center = p
            )
            // 外框
            drawCircle(
                color = onBackground.copy(alpha = 0.6f * appearAlpha * seqAlpha),
                radius = if (active) (radius + 2f) else (radius + 1f),
                center = p,
                style = Stroke(width = if (active) 3f else 2f)
            )

            // 選取標記與提示
            if (selectedNode == index) {
                // 高亮圈
                drawCircle(
                    color = primary.copy(alpha = 0.25f),
                    radius = 26f,
                    center = p,
                    style = Stroke(width = 3f)
                )
                // 簡易 tooltip：列出此節點對應的行星與其 gate
                val entries = assignments?.planetToGate
                    ?.filter { (_, g) -> nodeIndexByGate(g) == index }
                    ?.toList()
                    ?: emptyList()

                if (entries.isNotEmpty()) {
                    val text = buildString {
                        append("節點 ").append(index + 1).append("\n")
                        entries.take(4).forEach { (planet, gate) ->
                            append(planet).append(": #").append(gate).append('\n')
                        }
                        if (entries.size > 4) append("…(${entries.size})")
                    }
                    val paint = Paint().apply {
                        color = android.graphics.Color.argb(230, 20, 20, 20)
                        textSize = 30f
                        isAntiAlias = true
                    }
                    val bgPaint = Paint().apply {
                        color = android.graphics.Color.argb(180, 240, 240, 240)
                        style = Paint.Style.FILL
                    }
                    val padding = 12f
                    val lines = text.split('\n')
                    val maxWidth = lines.maxOf { paint.measureText(it) }
                    val lineHeight = paint.fontMetrics.run { bottom - top }
                    val boxW = maxWidth + padding * 2
                    val boxH = lineHeight * lines.size + padding * 2
                    // 邊界避讓：預設右上，若超出則改左或下
                    var bx = p.x + 20f
                    var by = p.y - boxH - 10f
                    if (bx + boxW > w) bx = p.x - boxW - 20f
                    if (bx < 0f) bx = 8f
                    if (by < 0f) by = p.y + 20f
                    if (by + boxH > h) by = h - boxH - 8f
                    // 背景框
                    drawContext.canvas.nativeCanvas.drawRoundRect(bx, by, bx + boxW, by + boxH, 12f, 12f, bgPaint)
                    // 逐行文字
                    var ty = by + padding - paint.fontMetrics.top
                    lines.forEach { line ->
                        drawContext.canvas.nativeCanvas.drawText(line, bx + padding, ty, paint)
                        ty += lineHeight
                    }
                }
            }
        }

        // 圖例（右下角）：
        run {
            val padding = 12f
            val legendLines = listOf(
                "圖例",
                "節點顏色強度 = 活躍數量（主題：${theme.display}）",
                "粗線 = 兩端活躍之抽象通道"
            )
            val paint = Paint().apply {
                color = android.graphics.Color.argb((255 * appearAlpha).toInt(), 30, 30, 30)
                textSize = 28f
                isAntiAlias = true
            }
            val bgPaint = Paint().apply {
                color = android.graphics.Color.argb((200 * appearAlpha).toInt(), 250, 250, 250)
                style = Paint.Style.FILL
            }
            val maxWidth = legendLines.maxOf { paint.measureText(it) }
            val lineHeight = paint.fontMetrics.run { bottom - top }
            val boxW = maxWidth + padding * 2
            val boxH = lineHeight * legendLines.size + padding * 2
            val bx = (w - boxW - 16f)
            val by = (h - boxH - 16f)
            drawContext.canvas.nativeCanvas.drawRoundRect(bx, by, bx + boxW, by + boxH, 12f, 12f, bgPaint)
            var ty = by + padding - paint.fontMetrics.top
            legendLines.forEach { line ->
                drawContext.canvas.nativeCanvas.drawText(line, bx + padding, ty, paint)
                ty += lineHeight
            }
        }
    }
}

// 三種顏色主題（自製、中性）：Vitality/活力、Stability/穩定、Insight/靈感
enum class NodeColorTheme(val display: String) { Vitality("活力"), Stability("穩定"), Insight("靈感") }

private data class Channel(val a: Int, val b: Int, val color: Color? = null, val width: Float? = null)

private fun defaultChannels(): List<Channel> = listOf(
    Channel(0,3), Channel(1,4), Channel(2,5), Channel(3,6), Channel(4,7), Channel(5,8),
    Channel(0,4), Channel(2,4), Channel(4,8)
)

private suspend fun loadAbstractChannels(context: android.content.Context, assetName: String): List<Channel>? = withContext(Dispatchers.IO) {
    runCatching {
        context.assets.open(assetName).use { input ->
            val text = input.readBytes().toString(Charsets.UTF_8)
            val obj = JSONObject(text)
            val arr = obj.getJSONArray("channels")
            val out = mutableListOf<Channel>()
            for (i in 0 until arr.length()) {
                val item = arr.get(i)
                when (item) {
                    is org.json.JSONArray -> {
                        val a = item.getInt(0)
                        val b = item.getInt(1)
                        out += Channel(a, b)
                    }
                    is org.json.JSONObject -> {
                        val a = item.getInt("a")
                        val b = item.getInt("b")
                        val width = item.optDouble("width", Double.NaN)
                        val colorStr = item.optString("color", "")
                        val color = if (colorStr.isNotBlank()) parseColor(colorStr) else null
                        out += Channel(a, b, color = color, width = if (width.isNaN()) null else width.toFloat())
                    }
                    else -> {}
                }
            }
            out
        }
    }.getOrNull()
}

private fun parseColor(hex: String): Color? = runCatching {
    // Accept formats like #RRGGBB or #AARRGGBB
    val clean = hex.trim()
    val colorInt = android.graphics.Color.parseColor(clean)
    val a = (colorInt ushr 24) and 0xFF
    val r = (colorInt ushr 16) and 0xFF
    val g = (colorInt ushr 8) and 0xFF
    val b = (colorInt) and 0xFF
    Color(r / 255f, g / 255f, b / 255f, if (clean.length == 9) a / 255f else 1f)
}.getOrNull()

@Preview(showBackground = true, name = "Vitality Default")
@Composable
private fun BodyGraphPreviewVitality() {
    val vm = DesignViewModel()
    val instant = java.time.Instant.ofEpochSecond(1_700_000_000)
    vm.updateFromStub(instant)
    BodyGraph(vm, modifier = Modifier, theme = NodeColorTheme.Vitality)
}

@Preview(showBackground = true, name = "Stability Thin")
@Composable
private fun BodyGraphPreviewStabilityThin() {
    val vm = DesignViewModel()
    val instant = java.time.Instant.ofEpochSecond(1_700_000_000)
    vm.updateFromStub(instant)
    BodyGraph(vm, modifier = Modifier, theme = NodeColorTheme.Stability, channelsAssetName = "abstract_channels_thin.json")
}

@Preview(showBackground = true, name = "Insight Color")
@Composable
private fun BodyGraphPreviewInsightColor() {
    val vm = DesignViewModel()
    val instant = java.time.Instant.ofEpochSecond(1_700_000_000)
    vm.updateFromStub(instant)
    BodyGraph(vm, modifier = Modifier, theme = NodeColorTheme.Insight, channelsAssetName = "abstract_channels_color.json")
}
