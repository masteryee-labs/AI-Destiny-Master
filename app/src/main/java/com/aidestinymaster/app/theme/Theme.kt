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
    primary = Color(0xFF3D5AFE),
    onPrimary = Color.White,
    secondary = Color(0xFF26C6DA),
    onSecondary = Color.Black,
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color.Black,
    secondary = Color(0xFF80DEEA),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color(0xFFEFEFEF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEFEFEF)
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

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
