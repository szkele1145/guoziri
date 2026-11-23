package com.guoziri.app

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 自动更新管理器
 * - 从 GitHub Releases API 检查最新版本（支持多加速源）
 * - 下载 APK 到外部存储（支持断点续传、多代理故障转移、通知栏进度）
 * - 调用系统安装程序安装，安装后自动清理临时文件
 */
class UpdateManager(private val context: Context) {

    private val TAG = "UpdateManager"
    
    // GitHub 仓库信息
    private val repoOwner = "szkele1145"
    private val repoName = "guoziri"
    
    // 国内加速源列表（按优先级排序）
    private val accelerationProxies = listOf(
        "https://ghproxy.com/",           
        "https://mirror.ghproxy.com/",    
        "https://gh.api.99988866.xyz/",   
        "https://hub.gitmirror.com/",     
        "https://git.mirrors.sustech.edu.cn/", 
        ""                                
    )
    
    private val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
    
    // 版本信息数据类
    data class ReleaseInfo(
        val tagName: String,
        val name: String,
        val body: String,
        val assets: List<Asset>,
        val publishedAt: String
    )
    
    data class Asset(
        val name: String,
        val browserDownloadUrl: String,
        val size: Long
    )
    
    // 当前版本
    private var currentVersionName: String = ""
    private var currentVersionCode: Int = 0
    
    init {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            currentVersionName = packageInfo.versionName ?: ""
            currentVersionCode = packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }
    
    /**
     * 检查更新
     * @param onResult 回调：(hasUpdate, releaseInfo, errorMessage)
     */
    fun checkUpdate(onResult: (Boolean, ReleaseInfo?, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = fetchWithProxies(apiUrl) { url ->
                    fetchJsonFromUrl(url)
                }
                
                if (json != null) {
                    val releaseInfo = parseReleaseJson(json)
                    
                    if (releaseInfo != null) {
                        val hasUpdate = compareVersion(releaseInfo.tagName)
                        onResult(hasUpdate, releaseInfo, null)
                    } else {
                        onResult(false, null, "解析版本信息失败")
                    }
                } else {
                    onResult(false, null, "所有加速源均不可用，请检查网络")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, null, "检查更新失败: ${e.message}")
            }
        }
    }
    
    /**
     * 检查更新
     * @param onResult 回调：(hasUpdate, releaseInfo, errorMessage)
     */
    fun checkUpdate(onResult: (Boolean, ReleaseInfo?, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                onResult(false, null, "正在检查更新...") // 临时回调用于调试
                val json = fetchWithProxies(apiUrl) { url ->
                    fetchJsonFromUrl(url)
                }
                
                if (json != null) {
                    val releaseInfo = parseReleaseJson(json)
                    
                    if (releaseInfo != null) {
                        val hasUpdate = compareVersion(releaseInfo.tagName)
                        onResult(hasUpdate, releaseInfo, null)
                    } else {
                        onResult(false, null, "解析版本信息失败")
                    }
                } else {
                    onResult(false, null, "所有加速源均不可用，请检查网络")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, null, "检查更新失败: ${e.message}")
            }
        }
    }
    
    private fun fetchWithProxies(originUrl: String, fetcher: (String) -> String?): String? {
        for (proxy in accelerationProxies) {
            val proxiedUrl = if (proxy.isEmpty()) originUrl else proxy + originUrl
            try {
                val json = fetcher(proxiedUrl)
                if (json != null && json.isNotBlank()) {
                    return json
                }
            } catch (e: Exception) {
                android.util.Log.w("UpdateManager", "Proxy $proxy failed: ${e.message}")
                continue
            }
        }
        return null
    }
    
    private fun fetchJsonFromUrl(urlString: String): String? {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000  // 增加超时
        connection.readTimeout = 15000
        connection.addRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.addRequestProperty("User-Agent", "Guoziri-App")
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = connection.inputStream
            val json = inputStream.readBytes().decodeToString()
            inputStream.close()
            connection.disconnect()
            return json
        } else {
            connection.disconnect()
            return null
        }
    }
    
    private fun compareVersion(latestTag: String): Boolean {
        try {
            val latest = latestTag.replace("v", "")
            val current = currentVersionName
            
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = if (i < latestParts.size) latestParts[i] else 0
                val c = if (i < currentParts.size) currentParts[i] else 0
                if (l > c) return true
                if (l < c) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun parseReleaseJson(json: String): ReleaseInfo? {
        try {
            val gson = com.google.gson.Gson()
            val release = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            
            val tagName = release.get("tag_name")?.asString ?: return null
            val name = release.get("name")?.asString ?: tagName
            val body = release.get("body")?.asString ?: ""
            val publishedAt = release.get("published_at")?.asString ?: ""
            
            val assetsArray = release.getAsJsonArray("assets") ?: return null
            val assets = mutableListOf<Asset>()
            for (element in assetsArray) {
                val asset = element.asJsonObject
                val name = asset.get("name")?.asString ?: continue
                val url = asset.get("browser_download_url")?.asString ?: continue
                val size = asset.get("size")?.asLong ?: 0L
                if (name.endsWith(".apk")) {
                    assets.add(Asset(name, url, size))
                }
            }
            val apkAsset = assets.firstOrNull() ?: return null
            
            return ReleaseInfo(tagName, name, body, listOf(apkAsset), publishedAt)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)
    }
    
    private fun parseAssets(json: String): List<Asset> {
        val assets = mutableListOf<Asset>()
        val assetsStart = json.indexOf("\"assets\"")
        if (assetsStart == -1) return assets
        
        val arrayStart = json.indexOf("[", assetsStart)
        val arrayEnd = json.indexOf("]", arrayStart)
        if (arrayStart == -1 || arrayEnd == -1) return assets
        
        val assetsJson = json.substring(arrayStart + 1, arrayEnd)
        
        var pos = 0
        while (pos < assetsJson.length) {
            val objStart = assetsJson.indexOf("{", pos)
            if (objStart == -1) break
            val objEnd = assetsJson.indexOf("}", objStart)
            if (objEnd == -1) break
            
            val objJson = assetsJson.substring(objStart, objEnd + 1)
            val name = extractJsonValue(objJson, "name")
            val url = extractJsonValue(objJson, "browser_download_url")
            val sizeStr = extractJsonValue(objJson, "size")
            val size = sizeStr?.toLongOrNull() ?: 0L
            
            if (name != null && url != null) {
                assets.add(Asset(name, url, size))
            }
            pos = objEnd + 1
        }
        return assets
    }
    
    /**
     * 下载并安装 APK（支持断点续传、多代理故障转移、通知栏进度）
     */
    fun downloadAndInstall(
        releaseInfo: ReleaseInfo,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        val asset = releaseInfo.assets.firstOrNull() ?: run {
            onComplete(false, "未找到 APK 下载链接")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val notificationManager = NotificationManagerCompat.from(context)
            val channelId = "update_download"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "更新下载", NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)
            }
            
            var lastProgress = -1
            val notificationId = 1001
            
            try {
                val originalUrl = asset.browserDownloadUrl
                val proxiedUrls = accelerationProxies.map { proxy ->
                    if (proxy.isEmpty()) originalUrl else proxy + originalUrl
                }
                
                var downloadSuccess = false
                var lastException: Exception? = null
                var apkFile: File? = null
                
                // 预先计算文件路径（所有代理共用同一路径）
                val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update_apks")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val apkFile = File(downloadDir, "guoziri_${releaseInfo.tagName}.apk")
                val tempFile = File(downloadDir, "guoziri_${releaseInfo.tagName}.apk.tmp")
                
                for (proxiedUrl in proxiedUrls) {
                    try {
                        val downloadUrl = URL(proxiedUrl)
                        val connection = downloadUrl.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 15000
                        connection.readTimeout = 30000
                        connection.addRequestProperty("User-Agent", "Guoziri-App")
                        
                        // 支持断点续传
                        val tempFile = File(downloadDir, "guoziri_${releaseInfo.tagName}.apk.tmp")
                        val downloadedSoFar = if (tempFile.exists()) tempFile.length() else 0L
                        
                        // 设置 Range 头支持断点续传
                        var downloaded = if (tempFile.exists()) tempFile.length() else 0L
                        
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 15000
                        connection.readTimeout = 30000
                        connection.addRequestProperty("User-Agent", "Guoziri-App")
                        
                        // 设置 Range 头支持断点续传
                        if (downloaded > 0) {
                            connection.addRequestProperty("Range", "bytes=$downloaded-")
                        }
                        
                        val responseCode = connection.responseCode
                        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                            throw Exception("HTTP $responseCode")
                        }
                        
                        val inputStream = connection.inputStream
                        
                        val fileSize = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                            connection.getHeaderField("Content-Range")?.split("/")?.last()?.toLongOrNull() ?: asset.size
                        } else {
                            asset.size
                        }
                        
                        val outputStream = FileOutputStream(File(downloadDir, "guoziri_${releaseInfo.tagName}.apk.tmp"), true) // append 模式
                        
                        val buffer = ByteArray(8192)
                        var downloaded = if (tempFile.exists()) tempFile.length() else 0L
                        var lastProgress = -1
                        
                        // 显示下载通知
                        val notification = NotificationCompat.Builder(context, channelId)
                            .setContentTitle("正在下载更新 ${releaseInfo.tagName}")
                            .setContentText("0%")
                            .setSmallIcon(android.R.drawable.stat_sys_download)
                            .setProgress(100, 0, false)
                            .setOngoing(true)
                            .build()
                        notificationManager.notify(notificationId, notification)
                        
                        while (true) {
                            val read = inputStream.read(buffer)
                            if (read == -1) break
                            outputStream.write(buffer, 0, read)
                            downloaded += read
                            
                            if (fileSize > 0) {
                                val progress = ((downloaded * 100) / fileSize).toInt()
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    withContext(Dispatchers.Main) {
                                        onProgress(progress)
                                        val notification = NotificationCompat.Builder(context, channelId)
                                            .setContentTitle("正在下载更新 ${releaseInfo.tagName}")
                                            .setContentText("$progress%")
                                            .setSmallIcon(android.R.drawable.stat_sys_download)
                                            .setProgress(100, progress, false)
                                            .setOngoing(true)
                                            .build()
                                        notificationManager.notify(notificationId, notification)
                                    }
                                }
                            }
                        }
                        
                        inputStream.close()
                        outputStream.close()
                        connection.disconnect()
                        
                        // 重命名临时文件
                        val tempFile = File(downloadDir, "guoziri_${releaseInfo.tagName}.apk.tmp")
                        val apkFile = File(downloadDir, "guoziri_${releaseInfo.tagName}.apk")
                        if (tempFile.renameTo(apkFile)) {
                            downloadSuccess = true
                            withContext(Dispatchers.Main) {
                                notificationManager.cancel(notificationId)
                                installApk(apkFile, onComplete)
                            }
                            break
                        } else {
                            throw Exception("重命名临时文件失败")
                        }
                        
                    } catch (e: Exception) {
                        lastException = e
                        continue
                    }
                }
                
                if (!downloadSuccess) {
                    throw lastException ?: Exception("所有加速源下载均失败")
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    notificationManager.cancel(notificationId)
                    onComplete(false, "下载失败: ${e.message}")
                }
            }
        }
    }
    
    private fun installApk(apkFile: File, onComplete: (Boolean, String) -> Unit) {
        if (!apkFile.exists()) {
            onComplete(false, "APK 文件不存在")
            return
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            val uri: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                uri = Uri.fromFile(apkFile)
            }
            
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val packageManager = context.packageManager
                val canInstall = packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    onComplete(false, "请在设置中允许安装未知应用")
                    return
                }
            }
            
            context.startActivity(intent)
            onComplete(true, "已启动安装程序")
            
            // 安装完成后延迟清理临时文件
            CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay(5000)
                try {
                    val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update_apks")
                    downloadDir.listFiles()?.forEach { it.delete() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, "安装失败: ${e.message}")
        }
    }
    
    fun showUpdateDialog(
        activity: Activity,
        releaseInfo: ReleaseInfo,
        onDownload: () -> Unit
    ) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("发现新版本 ${releaseInfo.tagName}")
            .setMessage(releaseInfo.body.takeIf { it.isNotBlank() } ?: "有新版本可用，建议更新")
            .setPositiveButton("立即更新") { _, _ -> onDownload() }
            .setNegativeButton("稍后再说", null)
            .setCancelable(false)
            .create()
        
        dialog.show()
    }
}