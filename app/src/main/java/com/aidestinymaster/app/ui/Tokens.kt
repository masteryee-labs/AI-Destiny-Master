package com.aidestinymaster.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import android.os.Build
import kotlin.math.abs

object DesignTokens {
    object Radius {
        const val Card = 14
        const val Dock = 24
        const val Chip = 20
    }
    object Duration {
        const val FadeThrough = 220
        const val Press = 90
    }
    object Spacing {
        const val XS = 4
        const val S = 8
        const val M = 12
        const val L = 16
        const val XL = 20
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.pressScale(enabled: Boolean = true, scale: Float = 0.98f): Modifier {
    if (!enabled) return this
    var pressed by remember { mutableStateOf(false) }
    val animated by animateFloatAsState(targetValue = if (pressed) scale else 1f, animationSpec = tween(DesignTokens.Duration.Press), label = "pressScale")
    return this
        .graphicsLayer(scaleX = animated, scaleY = animated)
        .pointerInteropFilter { event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> { pressed = true }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> { pressed = false }
            }
            false
        }
}

@Composable
fun <T> FadeThrough(target: T, reduceMotion: Boolean, content: @Composable (T) -> Unit) {
    if (reduceMotion) {
        content(target)
    } else {
        Crossfade(targetState = target, animationSpec = tween(DesignTokens.Duration.FadeThrough), content = content)
    }
}

// Disable glass blur effect globally to avoid perceived overall blur on UI
fun Modifier.glassModifier(): Modifier = this

@Composable
fun glassCardColors(): CardColors =
    if (Build.VERSION.SDK_INT >= 31)
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f))
    else CardDefaults.cardColors()
