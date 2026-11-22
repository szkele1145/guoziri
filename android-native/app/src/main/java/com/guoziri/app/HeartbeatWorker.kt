package com.guoziri.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * 心跳 Worker：每 15 分钟 ping GitHub 保持连接活跃
 * 使用 WorkManager 周期性执行，系统会自动管理生命周期、电池优化等
 */
class HeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/szkele1145/guoziri")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.addRequestProperty("User-Agent", "Guoziri-Heartbeat")
            connection.addRequestProperty("Accept", "application/vnd.github+json")

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode == 200) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "HeartbeatWorker"
        const val INTERVAL_MINUTES = 15L
    }
}