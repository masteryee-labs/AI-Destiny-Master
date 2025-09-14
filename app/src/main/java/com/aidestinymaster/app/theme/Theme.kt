package com.aidestinymaster.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import android.graphics.Typeface

private val LightColors = lightColorScheme(
    primary = Color(0xFF8B6A2E), // muted gold for light theme
    onPrimary = Color.White,
    secondary = Color(0xFF3D5AFE),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFF),
    onBackground = Color(0xFF10131A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10131A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9A46A), // brand gold
    onPrimary = Color(0xFF0A0A0A),
    secondary = Color(0xFF9BB6FF),
    onSecondary = Color(0xFF0A0F18),
    background = Color(0xFF10131A), // deep space
    onBackground = Color(0xFFEDEFF6),
    surface = Color(0xFF151A23), // elevated surface
    onSurface = Color(0xFFEDEFF6)
)

private fun cjkFontFamily(): FontFamily {
    // Try common CJK fonts; fallback to SansSerif
    val candidates = listOf(
        "Noto Sans CJK TC",
        "Noto Sans TC",
        "Source Han Sans TC",
        "PingFang TC",
        "PingFang HK",
        "Microsoft JhengHei",
        "Heiti TC",
        "sans-serif"
    )
    val tf = candidates.firstNotNullOfOrNull { name -> runCatching { Typeface.create(name, Typeface.NORMAL) }.getOrNull() }
    return if (tf != null) FontFamily(tf) else FontFamily.SansSerif
}

private val CjkSans = cjkFontFamily()

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, fontFamily = CjkSans),
    displayMedium = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = CjkSans),
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold, fontFamily = CjkSans),

    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, fontFamily = CjkSans),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, fontFamily = CjkSans),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, fontFamily = CjkSans),

    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = CjkSans),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = CjkSans),
    titleSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = CjkSans),

    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, fontFamily = CjkSans),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, fontFamily = CjkSans),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, fontFamily = CjkSans),

    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = CjkSans),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = CjkSans),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = CjkSans)
)
private val AppShapes = Shapes()

private fun Typography.scaled(scale: Float): Typography = Typography(
    displayLarge = displayLarge.copy(fontSize = (displayLarge.fontSize.value * scale).sp),
    displayMedium = displayMedium.copy(fontSize = (displayMedium.fontSize.value * scale).sp),
    displaySmall = displaySmall.copy(fontSize = (displaySmall.fontSize.value * scale).sp),
    headlineLarge = headlineLarge.copy(fontSize = (headlineLarge.fontSize.value * scale).sp),
    headlineMedium = headlineMedium.copy(fontSize = (headlineMedium.fontSize.value * scale).sp),
    headlineSmall = headlineSmall.copy(fontSize = (headlineSmall.fontSize.value * scale).sp),
    titleLarge = titleLarge.copy(fontSize = (titleLarge.fontSize.value * scale).sp),
    titleMedium = titleMedium.copy(fontSize = (titleMedium.fontSize.value * scale).sp),
    titleSmall = titleSmall.copy(fontSize = (titleSmall.fontSize.value * scale).sp),
    bodyLarge = bodyLarge.copy(fontSize = (bodyLarge.fontSize.value * scale).sp),
    bodyMedium = bodyMedium.copy(fontSize = (bodyMedium.fontSize.value * scale).sp),
    bodySmall = bodySmall.copy(fontSize = (bodySmall.fontSize.value * scale).sp),
    labelLarge = labelLarge.copy(fontSize = (labelLarge.fontSize.value * scale).sp),
    labelMedium = labelMedium.copy(fontSize = (labelMedium.fontSize.value * scale).sp),
    labelSmall = labelSmall.copy(fontSize = (labelSmall.fontSize.value * scale).sp),
)

@Composable
fun AppTheme(darkTheme: Boolean, fontScale: Float = 1.0f, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography.scaled(fontScale),
        shapes = AppShapes,
        content = content
    )
}
