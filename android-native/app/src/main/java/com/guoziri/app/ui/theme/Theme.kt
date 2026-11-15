package com.guoziri.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 设计令牌，1:1 对应 web/style.css 的 :root 与 html[data-theme="dark"]。
 * 换算：CSS px -> dp/sp（1:1）。任何色值都不要再在页面里硬编码。
 */
data class AppColors(
    /** 页面底色 --bg */
    val bg: Color,
    /** 卡片/面板底色 --bg-elevated */
    val bgElevated: Color,
    /** 次级底色 --bg-secondary */
    val bgSecondary: Color,
    /** 主文字 --label */
    val label: Color,
    /** 次级文字色 --label-2 */
    val label2: Color,
    /** 次要文字 --secondary rgba(60,60,67,.6) */
    val secondary: Color,
    /** 辅助文字 --tertiary rgba(60,60,67,.3) */
    val tertiary: Color,
    /** 分隔线 --separator */
    val separator: Color,
    /** 按压/填充 --fill */
    val fill: Color,
    /** 主色 --tint */
    val tint: Color,
    /** 主色淡 --tint-soft */
    val tintSoft: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val yellow: Color,
)

/** 亮色（对应 :root） */
val LightAppColors = AppColors(
    bg = Color(0xFFF2F2F7),
    bgElevated = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFF2F2F7),
    label = Color(0xFF1C1C1E),
    label2 = Color(0xFF3C3C43),
    secondary = Color(0x993C3C43),      // rgba(60,60,67,0.6)
    tertiary = Color(0x4D3C3C43),       // rgba(60,60,67,0.3)
    separator = Color(0x4A3C3C43),      // rgba(60,60,67,0.29)
    fill = Color(0x1F787880),           // rgba(120,120,128,0.12)
    tint = Color(0xFF007AFF),
    tintSoft = Color(0x1F007AFF),       // rgba(0,122,255,0.12)
    green = Color(0xFF34C759),
    orange = Color(0xFFFF9500),
    red = Color(0xFFFF3B30),
    yellow = Color(0xFFFFCC00),
)

/** 暗色（对应 html[data-theme="dark"]） */
val DarkAppColors = AppColors(
    bg = Color(0xFF000000),
    bgElevated = Color(0xFF1C1C1E),
    bgSecondary = Color(0xFF1C1C1E),
    label = Color(0xFFFFFFFF),
    label2 = Color(0xFFEBEBF5),
    secondary = Color(0x99EBEBF5),      // rgba(235,235,245,0.6)
    tertiary = Color(0x4DEBEBF5),       // rgba(235,235,245,0.3)
    separator = Color(0x99545458),      // rgba(84,84,88,0.6)
    fill = Color(0x3D787880),           // rgba(120,120,128,0.24)
    tint = Color(0xFF0A84FF),
    tintSoft = Color(0x330A84FF),       // rgba(10,132,255,0.2)
    green = Color(0xFF30D158),
    orange = Color(0xFFFF9F0A),
    red = Color(0xFFFF453A),
    yellow = Color(0xFFFFD60A),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** 供非 Composable 场景读取当前令牌（例如自定义绘制） */
object AppPalette {
    val current: AppColors
        @Composable get() = LocalAppColors.current
}

private val LightScheme = lightColorScheme(
    primary = LightAppColors.tint,
    onPrimary = Color.White,
    primaryContainer = LightAppColors.tintSoft,
    onPrimaryContainer = LightAppColors.tint,
    secondary = LightAppColors.green,
    onSecondary = Color.White,
    tertiary = LightAppColors.orange,
    onTertiary = Color.White,
    error = LightAppColors.red,
    onError = Color.White,
    background = LightAppColors.bg,
    onBackground = LightAppColors.label,
    surface = LightAppColors.bgElevated,
    onSurface = LightAppColors.label,
    surfaceVariant = LightAppColors.fill,
    onSurfaceVariant = LightAppColors.secondary,
    outline = LightAppColors.tertiary,
    outlineVariant = LightAppColors.separator,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,
    surfaceTint = LightAppColors.tint,
)

private val DarkScheme = darkColorScheme(
    primary = DarkAppColors.tint,
    onPrimary = Color.White,
    primaryContainer = DarkAppColors.tintSoft,
    onPrimaryContainer = DarkAppColors.tint,
    secondary = DarkAppColors.green,
    onSecondary = Color.Black,
    tertiary = DarkAppColors.orange,
    onTertiary = Color.Black,
    error = DarkAppColors.red,
    onError = Color.White,
    background = DarkAppColors.bg,
    onBackground = DarkAppColors.label,
    surface = DarkAppColors.bgElevated,
    onSurface = DarkAppColors.label,
    surfaceVariant = DarkAppColors.fill,
    onSurfaceVariant = DarkAppColors.secondary,
    outline = DarkAppColors.tertiary,
    outlineVariant = DarkAppColors.separator,
    inverseSurface = Color.White,
    inverseOnSurface = Color(0xFF1C1C1E),
    surfaceTint = DarkAppColors.tint,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content,
        )
    }
}
