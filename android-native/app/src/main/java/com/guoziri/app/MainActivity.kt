package com.guoziri.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

// iOS 蓝主色
val IosBlue = Color(0xFF007AFF)
val IosGreen = Color(0xFF34C759)
val IosOrange = Color(0xFFFF9F0A)
val IosRed = Color(0xFFFF3B30)
val IosYellow = Color(0xFFFFCC00)

private val LightScheme = androidx.compose.material3.lightColorScheme(
    primary = IosBlue,
    secondary = IosGreen,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    outlineVariant = Color(0x4A3C3C43),
)

private val DarkScheme = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF0A84FF),
    secondary = Color(0xFF30D158),
    background = Color.Black,
    surface = Color(0xFF1C1C1E),
    onBackground = Color.White,
    onSurface = Color.White,
    outlineVariant = Color(0x60545458),
)

/** 主题偏好：auto / light / dark（与旧版 settings.theme 兼容） */
object ThemePrefs {
    val theme = MutableStateFlow("auto")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = StoreRepo(this)

        setContent {
            val themePref by ThemePrefs.theme.collectAsState()
            val dark = when (themePref) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme) {
                AppRoot(repo)
            }
        }
    }
}