package com.guoziri.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow

/** 主题偏好：auto / light / dark */
object ThemePrefs {
    val theme = MutableStateFlow("auto")
}