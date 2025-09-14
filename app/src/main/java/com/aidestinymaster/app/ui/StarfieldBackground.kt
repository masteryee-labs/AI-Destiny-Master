package com.aidestinymaster.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlin.random.Random

/**
 * 星空粒子背景（低雜訊），支援滾動視差偏移；尊重 reduceMotion 參數關閉動畫。
 */
@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier,
    starColor: Color = Color(0x33FFFFFF),
    nebulaColor: Color = Color(0x22C9A46A),
    starCount: Int = 140,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    var parallax by remember { mutableStateOf(0f) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                parallax = (parallax + available.y * 0.02f).coerceIn(-40f, 40f)
                return Offset.Zero
            }
        }
    }
    val animatedParallax by animateFloatAsState(targetValue = if (reduceMotion) 0f else parallax, label = "parallax")

    val stars = remember {
        val rnd = Random(42)
        List(starCount) { Pair(rnd.nextFloat(), rnd.nextFloat()) }
    }

    Box(modifier.fillMaxSize().nestedScroll(connection)) {
        Canvas(Modifier.fillMaxSize()) {
            // Nebula glow
            drawIntoCanvas {
                drawCircle(color = nebulaColor, radius = size.minDimension * 0.8f, center = Offset(size.width * 0.2f, size.height * 0.25f))
            }
            // Stars
            stars.forEachIndexed { idx, (nx, ny) ->
                val x = nx * size.width
                val y = ny * size.height + animatedParallax
                val r = if (idx % 11 == 0) 1.8f else 1.0f
                drawCircle(color = starColor, radius = r, center = Offset(x, (y % size.height)))
            }
        }
        content()
    }
}
