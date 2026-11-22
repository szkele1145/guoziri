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
 * - 从 GitHub Releases API 检查最新版本
 * - 下载 APK 到外部存储
 * - 调用系统安装程序安装
 */
class UpdateManager(private val context: Context) {

    private val TAG = "UpdateManager"
    
    // GitHub 仓库信息
    private val repoOwner = "szkele1145"
    private val repoName = "guoziri"
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
            currentVersionName = packageInfo.versionName
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
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.addRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.addRequestProperty("User-Agent", "Guoziri-App")
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val json = inputStream.readBytes().decodeToString()
                    inputStream.close()
                    
                    // 简单解析 JSON（实际项目建议用 Gson/Moshi）
                    val releaseInfo = parseReleaseJson(json)
                    
                    if (releaseInfo != null) {
                        val hasUpdate = compareVersion(releaseInfo.tagName)
                        onResult(hasUpdate, releaseInfo, null)
                    } else {
                        onResult(false, null, "解析版本信息失败")
                    }
                } else {
                    onResult(false, null, "网络请求失败: $responseCode")
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, null, "检查更新失败: ${e.message}")
            }
        }
    }
    
    /**
     * 比较版本号
     * GitHub tag 格式通常为 "v1.2.3" 或 "1.2.3"
     */
    private fun compareVersion(latestTag: String): Boolean {
        try {
            // 去除可能的 "v" 前缀
            val latest = latestTag.replace("v", "")
            val current = currentVersionName
            
            val latestParts = latest.split(".").map { it.toInt() }
            val currentParts = current.split(".").map { it.toInt() }
            
            for (i in 0..2) {
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
    
    /**
     * 简单的 JSON 解析（提取关键字段）
     */
    private fun parseReleaseJson(json: String): ReleaseInfo? {
        try {
            // 简单的字符串解析，避免引入 Gson 依赖
            val tagName = extractJsonValue(json, "tag_name") ?: return null
            val name = extractJsonValue(json, "name") ?: tagName
            val body = extractJsonValue(json, "body") ?: ""
            val publishedAt = extractJsonValue(json, "published_at") ?: ""
            
            // 解析 assets 数组，找到 .apk 文件
            val assets = parseAssets(json)
            val apkAsset = assets.find { it.name.endsWith(".apk") } ?: return null
            
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
        // 找到 assets 数组部分
        val assetsStart = json.indexOf("\"assets\"")
        if (assetsStart == -1) return assets
        
        val arrayStart = json.indexOf("[", assetsStart)
        val arrayEnd = json.indexOf("]", arrayStart)
        if (arrayStart == -1 || arrayEnd == -1) return assets
        
        val assetsJson = json.substring(arrayStart + 1, arrayEnd)
        
        // 简单分割每个 asset 对象
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
     * 下载并安装 APK
     * @param releaseInfo 版本信息
     * @param onProgress 下载进度回调 (0-100)
     * @param onComplete 完成回调 (success, message)
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
            try {
                val downloadUrl = URL(asset.browserDownloadUrl)
                val connection = downloadUrl.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.addRequestProperty("User-Agent", "Guoziri-App")
                
                val fileSize = asset.size
                val inputStream = connection.inputStream
                
                // 创建下载目录
                val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update_apks")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                
                val apkFile = File(downloadDir, "guoziri_${releaseInfo.tagName}.apk")
                val outputStream = FileOutputStream(apkFile)
                
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var lastProgress = -1
                
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    outputStream.write(buffer, 0, read)
                    downloaded += read
                    
                    if (fileSize > 0) {
                        val progress = ((downloaded * 100) / fileSize).toInt()
                        if (progress != lastProgress) {
                            lastProgress = progress
                            // 切回主线程更新 UI
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                }
                
                inputStream.close()
                outputStream.close()
                connection.disconnect()
                
                // 下载完成，开始安装
                withContext(Dispatchers.Main) {
                    installApk(apkFile, onComplete)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, "下载失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 安装 APK
     */
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
            
            // Android 8.0+ 需要 REQUEST_INSTALL_PACKAGES 权限
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
            
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, "安装失败: ${e.message}")
        }
    }
    
    /**
     * 显示更新对话框
     */
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

/**
 * 扩展函数：在主线程显示 Toast
 */
fun Context.toast(message: String) {
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
}