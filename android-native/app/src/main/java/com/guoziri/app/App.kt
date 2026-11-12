package com.guoziri.app

import android.app.Application
import android.util.Log

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("guoziri", "app start (native)")
    }
}
