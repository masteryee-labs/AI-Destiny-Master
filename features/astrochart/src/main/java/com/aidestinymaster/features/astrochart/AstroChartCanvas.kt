package com.aidestinymaster.features.astrochart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.aidestinymaster.core.astro.AstroCalculator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.platform.LocalContext
import com.aidestinymaster.features.astrochart.PlanetSymbolRenderer.drawPlanetSymbol

enum class PlanetSymbolMode { Unicode, Font }

@Composable
fun AstroChartCanvas(
    planets: AstroCalculator.PlanetPositions,
    houses: AstroCalculator.Houses? = null,
    modifier: Modifier = Modifier,
    useUnicodeSymbols: Boolean = false,
    symbolMode: PlanetSymbolMode = PlanetSymbolMode.Font,
    fontAssetPath: String = "fonts/planet_symbols.ttf"
) {
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // 預載字型（如資產存在則使用）
    val typeface: Typeface? = remember(fontAssetPath) {
        runCatching { context.assets.open(fontAssetPath).close(); Typeface.createFromAsset(context.assets, fontAssetPath) }.getOrNull()
    }

    Canvas(modifier = modifier
        .fillMaxWidth()
        .semantics { this.contentDescription = "astrochart_canvas" }
        .pointerInput(Unit) {
            detectTransformGestures { _, panChange, zoomChange, _ ->
                pan += panChange
                scale = (scale * zoomChange).coerceIn(0.6f, 2.2f)
            }
        }
        .pointerInput(planets) {
            detectTapGestures { pos ->
                // 命中測試
                val hit = hitPlanet(pos, this.size.width.toFloat(), this.size.height.toFloat(), planets, scale, pan)
                selected = hit
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val baseRadius = min(w, h) * 0.42f

        // 變換
        drawContext.transform.translate(pan.x, pan.y)
        drawContext.transform.scale(scale, scale, pivot = Offset(cx, cy))

        // 外圈
        drawCircle(color = Color(0xFF222222), radius = baseRadius + 8f, center = Offset(cx, cy), style = Stroke(4f))
        drawCircle(color = Color(0xFF666666), radius = baseRadius, center = Offset(cx, cy), style = Stroke(2f))

        // 宮分割：若提供 houses 則使用 cusps，否則 30° 均分
        val cuspAngles = houses?.cusps?.map { it - 90.0 } ?: (0 until 12).map { it * 30.0 - 90.0 }
        cuspAngles.forEach { deg ->
            val ang = Math.toRadians(deg)
            val x = cx + (baseRadius * cos(ang)).toFloat()
            val y = cy + (baseRadius * sin(ang)).toFloat()
            drawLine(color = Color(0x33444444), start = Offset(cx, cy), end = Offset(x, y), strokeWidth = 2f, cap = StrokeCap.Square)
        }

        // 行星點與符號（用名稱首字作為簡易符號；並加 Tooltip)
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
            // 若選擇字型模式且資產存在，套用字型
            if (!useUnicodeSymbols && symbolMode == PlanetSymbolMode.Font && typeface != null) this.typeface = typeface
        }
        val tooltipPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 28f
            isAntiAlias = true
        }
        val tooltipBg = Paint().apply {
            color = android.graphics.Color.argb(210, 246, 246, 246)
            style = Paint.Style.FILL
        }
        val unicode = mapOf(
            "Sun" to "\u2609",      // ☉
            "Moon" to "\u263D",     // ☽
            "Mercury" to "\u263F", // ☿
            "Venus" to "\u2640",   // ♀
            "Mars" to "\u2642",    // ♂
            "Jupiter" to "\u2643", // ♃
            "Saturn" to "\u2644"   // ♄
        )
        planets.longitudes.forEach { (name, deg) ->
            val ang = Math.toRadians(deg - 90.0)
            val rx = baseRadius * 0.9f
            val px = cx + (rx * cos(ang)).toFloat()
            val py = cy + (rx * sin(ang)).toFloat()
            drawCircle(color = Color(0xFFDD6B20), radius = 7f, center = Offset(px, py))
            // 符號：若未指定 Unicode 但字型不可用，自動回退 Unicode
            val useUnicode = useUnicodeSymbols || symbolMode == PlanetSymbolMode.Unicode
            if (useUnicode) {
                val symbol = unicode[name] ?: name.firstOrNull()?.toString() ?: "?"
                drawContext.canvas.nativeCanvas.drawText(symbol, px + 8f, py - 8f, paint)
            } else if (symbolMode == PlanetSymbolMode.Font && typeface != null) {
                val symbol = name.firstOrNull()?.toString() ?: "?"
                drawContext.canvas.nativeCanvas.drawText(symbol, px + 8f, py - 8f, paint)
            } else {
                // 使用自製向量符號
                val sz = 28f
                this.drawPlanetSymbol(name, center = Offset(px + 10f, py - 10f), size = sz, color = Color.White)
            }
            // 簡易短線指向外圈
            val x2 = cx + (baseRadius * cos(ang)).toFloat()
            val y2 = cy + (baseRadius * sin(ang)).toFloat()
            drawLine(color = Color(0x99DD6B20), start = Offset(px, py), end = Offset(x2, y2), strokeWidth = 2f)
            // Tooltip 如果選中
            if (selected == name) {
                val info = "$name  ${"%.1f".format(deg)}°"
                val padding = 10f
                val wTxt = tooltipPaint.measureText(info)
                val hTxt = tooltipPaint.fontMetrics.run { bottom - top }
                var bx = px + 16f
                var by = py - hTxt - 16f
                if (bx + wTxt + padding * 2 > w) bx = px - wTxt - padding * 2 - 16f
                if (bx < 0f) bx = 8f
                if (by < 0f) by = py + 16f
                if (by + hTxt + padding * 2 > h) by = h - hTxt - padding * 2 - 8f
                drawContext.canvas.nativeCanvas.drawRoundRect(bx, by, bx + wTxt + padding * 2, by + hTxt + padding * 2, 10f, 10f, tooltipBg)
                drawContext.canvas.nativeCanvas.drawText(info, bx + padding, by + padding - tooltipPaint.fontMetrics.top, tooltipPaint)
            }
        }
    }
}

private fun hitPlanet(pos: Offset, w: Float, h: Float, planets: AstroCalculator.PlanetPositions, scale: Float, pan: Offset): String? {
    val cx = w / 2f
    val cy = h / 2f
    val radius = min(w, h) * 0.42f
    val rx = radius * 0.9f
    // 反向變換點
    val x = (pos.x - pan.x - cx) / scale + cx
    val y = (pos.y - pan.y - cy) / scale + cy
    var best: Pair<String, Float>? = null
    planets.longitudes.forEach { (name, deg) ->
        val ang = Math.toRadians(deg - 90.0)
        val px = cx + (rx * cos(ang)).toFloat()
        val py = cy + (rx * sin(ang)).toFloat()
        val d2 = (px - x) * (px - x) + (py - y) * (py - y)
        val cur = best
        if (cur == null || d2 < cur.second) best = name to d2
    }
    val hit = best
    return if (hit != null && hit.second <= 22f * 22f) hit.first else null
}
