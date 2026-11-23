package com.guoziri.app

import android.app.Application
import android.content.Context

/**
 * 全局 Application 上下文，用于在非 Activity 环境下访问 assets 等资源
 */
class AppContext : Application() {
    companion object {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        var context: Context? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}