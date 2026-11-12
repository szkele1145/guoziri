package com.guoziri.app

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow

/** 主题偏好：auto / light / dark */
object ThemePrefs {
    var theme: String = "auto"
}