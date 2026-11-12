package com.guoziri.app

import kotlinx.coroutines.flow.MutableStateFlow

/** 主题偏好：auto / light / dark */
object ThemePrefs {
    val theme = MutableStateFlow("auto")
}