package com.guoziri.app

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// iOS风格颜色定义
val IosBlue = Color(0xFF007AFF)
val IosGreen = Color(0xFF34C759)
val IosOrange = Color(0xFFFF9F0A)
val IosRed = Color(0xFFFF3B30)
val IosYellow = Color(0xFFFFCC00)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    secondary = Color(0xFF34C759),
    tertiary = Color(0xFFFF9500),
    error = Color(0xFFFF3B30),
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    primaryContainer = Color(0xFFD1E8FF),
    onPrimaryContainer = Color(0xFF001F4A),
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF143D1A),
    surfaceVariant = Color(0xFFE5E5EA),
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0xFFD1D1D6),
    inverseSurface = Color(0xFF121212),
    inverseOnSurface = Color.White,
    surfaceTint = Color(0xFF007AFF),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    secondary = Color(0xFF30D158),
    tertiary = Color(0xFFFF9F0A),
    error = Color(0xFFFF453A),
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF001E3A),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF1C3D1C),
    onSecondaryContainer = Color.White,
    surfaceVariant = Color(0xFF3E3E3E),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFF4A4A4A),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    surfaceTint = Color(0xFF0A84FF),
)

// 自定义iOS颜色常量
val IosBlue = Color(0xFF007AFF)
val IosGreen = Color(0xFF34C759)
val IosOrange = Color(0xFFFF9F0A)
val IosRed = Color(0xFFFF3B30)
val IosYellow = Color(0xFFFFCC00)
val IosPurple = Color(0xFFAF52DE)
val IosPink = Color(0xFFFF2D55)
val IosTeal = Color(0xFF5AC8FA)
val IosGray = Color(0xFF8E8E93)
val IosGray2 = Color(0xFFAFAFAF)
val IosGray3 = Color(0xFFD1D1D6)
val IosGray4 = Color(0xFFC7C7CC)
val IosGray4_light = Color(0xFFE5E5EA)
val IosGray5 = Color(0xFF98989E)
val IosGray6 = Color(0xFF636366)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}

// 自定义颜色方案
val LightColorScheme = lightColorScheme(
    primary = IosBlue,
    secondary = IosGreen,
    tertiary = IosOrange,
    error = IosRed,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    primaryContainer = Color(0xFFD1E8FF),
    onPrimaryContainer = Color(0xFF001F4A),
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF143D1A),
    surfaceVariant = Color(0xFFE5E5EA),
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0xFFD1D1D6),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    secondary = Color(0xFF30D158),
    tertiary = IosOrange,
    error = IosRed,
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF001E3A),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF1C3D1C),
    onSecondaryContainer = Color.White,
    surfaceVariant = Color(0xFF3E3E3E),
    outline = Color(0xFF84848A),
    outlineVariant = Color(0xFF4A4A4A),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// Material3 Typography
object Typography {
    val headlineLarge = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    )
    val headlineMedium = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    )
    val titleLarge = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    )
    val bodyLarge = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    )
    val bodyMedium = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    )
    val labelMedium = androidx.compose.material3.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    )
}

// 自定义形状
object Shapes {
    val extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    val small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val full = androidx.compose.foundation.shape.RoundedCornerShape(100.dp)
}