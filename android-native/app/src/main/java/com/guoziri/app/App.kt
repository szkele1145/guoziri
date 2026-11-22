package com.guoziri.app

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("guoziri", "app start (native)")
        scheduleHeartbeat()
    }

    private fun scheduleHeartbeat() {
        val request = PeriodicWorkRequest.Builder(
            HeartbeatWorker::class.java,
            HeartbeatWorker.INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            HeartbeatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
