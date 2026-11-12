package com.guoziri.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// iOS 风格颜色定义
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    secondary = Color(0xFF34C759),
    tertiary = Color(0xFFFF9F0A),
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

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}

// 公开颜色常量供其他文件使用
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