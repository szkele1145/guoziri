package com.guoziri.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.guoziri.app.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 35 起系统强制 edge-to-edge；显式开启后统一由 Compose insets 处理
        enableEdgeToEdge()

        val repo = StoreRepo(this)

        setContent {
            val themePref by ThemePrefs.theme.collectAsState("auto")
            val dark = when (themePref) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            AppTheme(darkTheme = dark) {
                AppRoot(repo)
            }
        }
    }
}
