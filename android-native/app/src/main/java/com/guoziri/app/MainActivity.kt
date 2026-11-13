package com.guoziri.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsStateWithLifecycle
import com.guoziri.app.ui.theme.AppTheme
import com.guoziri.app.ThemePrefs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = StoreRepo(this)

        setContent {
            val themePref by ThemePrefs.theme.collectAsStateWithLifecycle("auto")
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