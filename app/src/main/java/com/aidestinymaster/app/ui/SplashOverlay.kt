package com.aidestinymaster.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A premium-looking splash overlay: fades/zooms in a golden compass star and orbit arcs
 * over deep-space background, then fades out. Duration ~1600ms.
 */
@Composable
fun SplashOverlay(reduceMotion: Boolean = false, onFinished: () -> Unit) {
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) {
            alpha.snapTo(0f)
        } else {
            scale.animateTo(1.05f, animationSpec = tween(600, easing = EaseOut))
            scale.animateTo(1.0f, animationSpec = tween(200, easing = EaseOut))
            alpha.animateTo(0f, animationSpec = tween(800, easing = LinearEasing))
        }
        onFinished()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF10131A))
            .alpha(alpha.value),
        contentAlignment = Alignment.Center
    ) {
        // Compass star + orbit rings
        Canvas(Modifier.size(220.dp).scale(scale.value)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val gold = Color(0xFFC9A46A)
            // orbit arcs
            drawArc(
                color = gold,
                startAngle = 200f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(8f, 8f),
                size = androidx.compose.ui.geometry.Size(w - 16f, h - 16f),
                style = Stroke(width = 4f)
            )
            drawArc(
                color = gold,
                startAngle = 20f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(8f, 8f),
                size = androidx.compose.ui.geometry.Size(w - 16f, h - 16f),
                style = Stroke(width = 4f)
            )
            // star
            val outer = (w * 0.22f)
            val inner = (w * 0.09f)
            val p = Path()
            for (i in 0 until 8) {
                val aOuter = Math.toRadians((i * 45.0)).toFloat()
                val aInner = Math.toRadians(i * 45.0 + 22.5).toFloat()
                val xO = cx + outer * kotlin.math.cos(aOuter)
                val yO = cy + outer * kotlin.math.sin(aOuter)
                val xI = cx + inner * kotlin.math.cos(aInner)
                val yI = cy + inner * kotlin.math.sin(aInner)
                if (i == 0) p.moveTo(xO, yO) else p.lineTo(xO, yO)
                p.lineTo(xI, yI)
            }
            p.close()
            drawPath(p, color = Color.White)
            // center dot
            drawCircle(color = gold, radius = w * 0.018f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        }
    }
}
