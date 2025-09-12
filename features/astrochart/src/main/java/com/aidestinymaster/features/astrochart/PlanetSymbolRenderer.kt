package com.aidestinymaster.features.astrochart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

// 自製、簡化的行星向量符號渲染器（MIT 自著）。
// 僅供示意：非嚴格天文字形，但清晰易辨識，避免字型授權與體積問題。
object PlanetSymbolRenderer {
    fun DrawScope.drawPlanetSymbol(name: String, center: Offset, size: Float, color: Color = Color.White) {
        when (name.lowercase()) {
            "sun" -> drawSun(center, size, color)
            "moon" -> drawMoon(center, size, color)
            "mercury" -> drawMercury(center, size, color)
            "venus" -> drawVenus(center, size, color)
            "mars" -> drawMars(center, size, color)
            "jupiter" -> drawJupiter(center, size, color)
            "saturn" -> drawSaturn(center, size, color)
            else -> drawDefault(center, size, color)
        }
    }

    private fun DrawScope.drawSun(c: Offset, s: Float, color: Color) {
        val r = s * 0.35f
        drawCircle(color, radius = r, center = c, style = Stroke(s * 0.10f))
        drawCircle(color, radius = r * 0.35f, center = c)
    }

    private fun DrawScope.drawMoon(c: Offset, s: Float, color: Color) {
        val r = s * 0.45f
        // 大圓
        drawCircle(color, radius = r, center = c, style = Stroke(s * 0.10f))
        // 內側削去形成弦月效果（以 Path 模擬）
        val path = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(c.x - r * 0.7f, c.y - r, c.x + r * 0.7f, c.y + r))
        }
        drawPath(path, color)
    }

    private fun DrawScope.drawMercury(c: Offset, s: Float, color: Color) {
        val r = s * 0.28f
        // 頭部圓
        drawCircle(color, radius = r, center = c, style = Stroke(s * 0.10f))
        // 角（上方小弧）
        val path = Path().apply {
            moveTo(c.x - r * 0.8f, c.y - r * 1.1f)
            cubicTo(c.x - r * 0.2f, c.y - r * 1.5f, c.x + r * 0.2f, c.y - r * 1.5f, c.x + r * 0.8f, c.y - r * 1.1f)
        }
        drawPath(path, color, style = Stroke(s * 0.10f))
        // 柄（下方十字）
        drawLine(color, start = Offset(c.x, c.y + r), end = Offset(c.x, c.y + r * 2.1f), strokeWidth = s * 0.10f)
        drawLine(color, start = Offset(c.x - r * 0.8f, c.y + r * 1.55f), end = Offset(c.x + r * 0.8f, c.y + r * 1.55f), strokeWidth = s * 0.10f)
    }

    private fun DrawScope.drawVenus(c: Offset, s: Float, color: Color) {
        val r = s * 0.35f
        drawCircle(color, radius = r, center = c, style = Stroke(s * 0.10f))
        drawLine(color, start = Offset(c.x, c.y + r), end = Offset(c.x, c.y + r * 2.0f), strokeWidth = s * 0.12f)
        drawLine(color, start = Offset(c.x - r * 0.8f, c.y + r * 1.5f), end = Offset(c.x + r * 0.8f, c.y + r * 1.5f), strokeWidth = s * 0.12f)
    }

    private fun DrawScope.drawMars(c: Offset, s: Float, color: Color) {
        val r = s * 0.3f
        drawCircle(color, radius = r, center = c, style = Stroke(s * 0.10f))
        // 斜向箭頭
        val end = Offset(c.x + r * 1.6f, c.y - r * 1.6f)
        drawLine(color, start = Offset(c.x + r * 0.4f, c.y - r * 0.4f), end = end, strokeWidth = s * 0.12f)
        drawLine(color, start = end, end = Offset(end.x - s * 0.5f, end.y), strokeWidth = s * 0.12f)
        drawLine(color, start = end, end = Offset(end.x, end.y + s * 0.5f), strokeWidth = s * 0.12f)
    }

    private fun DrawScope.drawJupiter(c: Offset, s: Float, color: Color) {
        val r = s * 0.38f
        // 簡化：半圓 + 橫線
        drawCircle(color, radius = r, center = c, style = Stroke(s * 0.10f))
        drawLine(color, start = Offset(c.x - r, c.y + r * 0.1f), end = Offset(c.x + r, c.y + r * 0.1f), strokeWidth = s * 0.12f)
    }

    private fun DrawScope.drawSaturn(c: Offset, s: Float, color: Color) {
        val r = s * 0.32f
        drawCircle(color, radius = r, center = c, style = Stroke(s * 0.10f))
        // 環：斜橢圓（用兩條斜線近似）
        drawLine(color, start = Offset(c.x - r * 1.2f, c.y + r * 0.4f), end = Offset(c.x + r * 1.2f, c.y - r * 0.4f), strokeWidth = s * 0.10f)
        drawLine(color, start = Offset(c.x - r * 1.1f, c.y + r * 0.55f), end = Offset(c.x + r * 1.1f, c.y - r * 0.55f), strokeWidth = s * 0.06f)
    }

    private fun DrawScope.drawDefault(c: Offset, s: Float, color: Color) {
        val r = min(s * 0.25f, 14f)
        drawCircle(color, radius = r, center = c)
    }
}
