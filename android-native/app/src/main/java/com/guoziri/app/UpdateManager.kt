package com.guoziri.app

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
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

/**
 * 自动更新管理器
 *
 * 流程：
 *  1. 检查更新 —— 依次尝试多个国内加速源读取 GitHub Releases API
 *  2. 下载 APK —— 依次尝试多个加速源，支持断点续传、失败自动换源
 *  3. 安装     —— 通过 FileProvider 调用系统软件包安装程序
 */
class UpdateManager(private val context: Context) {

    private val repoOwner = "szkele1145"
    private val repoName = "guoziri"

    /**
     * 加速源分组。
     *
     * 实测结论（tools/probe_accelerators.py）：
     *  - 查版本（feed / REST API）只有 gh-proxy.com 和直连可用，其余代理不支持 API 路径
     *  - 下载 APK 是另一条路径，ghproxy.net / gh-proxy.com / 直连可用
     *  - 所以「查」和「下载」必须用不同的源列表，不能混用
     *  - 每个列表最后都放空串（直连）兜底
     */
    private val metaAccelerators = listOf(
        "https://gh-proxy.com/",
        ""
    )

    private val downloadAccelerators = listOf(
        "https://ghproxy.net/",
        "https://gh-proxy.com/",
        ""
    )

    private val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
    /** Atom feed：不走 REST API，没有速率限制，优先用 */
    private val feedUrl = "https://github.com/$repoOwner/$repoName/releases.atom"

    /** 版本信息 */
    data class ReleaseInfo(
        val tagName: String,
        val body: String,
        val apkUrl: String,
        val apkSize: Long
    )

    private val currentVersionName: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    /* ------------------------------------------------------------------ */
    /* 检查更新                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * 检查更新。
     *
     * 策略：先读 Atom feed（不走 REST API，没有 60次/小时的匿名速率限制），
     * 拿到最新版本号后再去 releases 页面找 APK 下载地址；失败则回退到 REST API。
     *
     * @param onResult (有更新, 版本信息, 错误信息)
     */
    fun checkUpdate(onResult: (Boolean, ReleaseInfo?, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "当前版本: $currentVersionName，开始检查更新")
                // ---- 1. Atom feed 拿最新版本号 ----
                var latestTag: String? = null
                var lastError = "网络不可用"
                for (acc in metaAccelerators) {
                    try {
                        val feed = fetchText(acc + feedUrl, accept = "application/atom+xml")
                        if (feed.isNullOrBlank()) {
                            Log.i(TAG, "feed 空: ${acc.ifEmpty { "直连" }}")
                            continue
                        }
                        latestTag = parseFeedTag(feed)
                        Log.i(TAG, "feed 结果 [${acc.ifEmpty { "直连" }}]: tag=$latestTag")
                        if (!latestTag.isNullOrBlank()) break
                    } catch (e: Exception) {
                        Log.i(TAG, "feed 失败 [${acc.ifEmpty { "直连" }}]: ${e.message}")
                        lastError = e.message ?: "请求失败"
                    }
                }

                // ---- 2. 用 REST API 拿完整信息（含 APK 地址）----
                if (latestTag != null) {
                    for (acc in metaAccelerators) {
                        try {
                            val json = fetchText(acc + apiUrl)
                            if (json.isNullOrBlank()) {
                                Log.i(TAG, "api 空: ${acc.ifEmpty { "直连" }}")
                                continue
                            }
                            val info = parseRelease(json)
                            if (info != null) {
                                Log.i(TAG, "api 成功 [${acc.ifEmpty { "直连" }}]: tag=${info.tagName}")
                                val hasUpdate = isNewer(info.tagName, currentVersionName)
                                Log.i(TAG, "对比版本: ${info.tagName} vs $currentVersionName -> $hasUpdate")
                                onResult(hasUpdate, if (hasUpdate) info else null, null)
                                return@launch
                            }
                            Log.i(TAG, "api 解析失败: ${acc.ifEmpty { "直连" }}")
                        } catch (e: Exception) {
                            Log.i(TAG, "api 失败 [${acc.ifEmpty { "直连" }}]: ${e.message}")
                            continue
                        }
                    }
                }

                // ---- 3. API 不可用时，用 expanded_assets 接口直接抓 APK 链接 ----
                if (latestTag != null) {
                    val apkUrl = fetchApkUrl(latestTag)
                    Log.i(TAG, "expanded_assets 结果: $apkUrl")
                    if (apkUrl != null) {
                        val info = ReleaseInfo(latestTag, "", apkUrl, 0L)
                        val hasUpdate = isNewer(info.tagName, currentVersionName)
                        onResult(hasUpdate, if (hasUpdate) info else null, null)
                        return@launch
                    }
                }

                // ---- 4. 全部失败 ----
                Log.i(TAG, "检查更新失败: $lastError")
                if (latestTag == null) {
                    onResult(false, null, "$lastError（所有加速源都连不上）")
                } else {
                    onResult(false, null, "找不到 v$latestTag 的安装包")
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查更新异常", e)
                onResult(false, null, e.message ?: "检查更新失败")
            }
        }
    }

    /** 从 Atom feed 里解析最新的 tag
     *
     * feed 结构里第一个 <id> 是 feed 自身的（.../releases），
     * 真正的 release 在 <entry> 里，其 <id> 形如 .../123/v1.3
     */
    private fun parseFeedTag(feed: String): String? {
        return try {
            // 只在 <entry> 块里找
            val entryStart = feed.indexOf("<entry>")
            if (entryStart < 0) return null
            val entry = feed.substring(entryStart)
            val m = java.util.regex.Pattern.compile("<id>[^<]*/([^/<]+)</id>").matcher(entry)
            if (m.find()) m.group(1) else null
        } catch (e: Exception) {
            null
        }
    }

    /** 从 releases 网页里抓 APK 下载地址
     *
     * GitHub 的 releases 页面部分内容是 JS 渲染的，
     * 所以这里优先用 GitHub 的 expanded_assets 接口（纯 HTML，稳）
     */
    private fun parseReleasePage(html: String, tag: String): ReleaseInfo? {
        return try {
            val m = java.util.regex.Pattern.compile(
                "href=\"([^\"]*/releases/download/[^\"]*\\.apk)\""
            ).matcher(html)
            var url = ""
            while (m.find()) {
                var candidate = m.group(1).replace("&amp;", "&")
                if (candidate.startsWith("/")) candidate = "https://github.com$candidate"
                url = candidate
                break
            }
            if (url.isEmpty()) null else ReleaseInfo(tag, "", url, 0L)
        } catch (e: Exception) {
            null
        }
    }

    /** 通过 expanded_assets 接口拿 APK 链接（纯服务端 HTML） */
    private fun fetchApkUrl(tag: String): String? {
        return try {
            val html = fetchText(
                "https://github.com/$repoOwner/$repoName/releases/expanded_assets/$tag",
                accept = "text/html"
            ) ?: return null
            val m = java.util.regex.Pattern.compile(
                "href=\"([^\"]*/releases/download/[^\"]*\\.apk)\""
            ).matcher(html)
            while (m.find()) {
                var candidate = m.group(1).replace("&amp;", "&")
                if (candidate.startsWith("/")) candidate = "https://github.com$candidate"
                return candidate
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** 读取文本（自动跟随重定向） */
    private fun fetchText(urlString: String, accept: String = "application/vnd.github+json"): String? {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                instanceFollowRedirects = true
                setRequestProperty("Accept", accept)
                setRequestProperty("User-Agent", "Guoziri-App")
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) return null
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection?.disconnect()
        }
    }

    /** 解析 GitHub Release JSON，找出 APK 资源 */
    private fun parseRelease(json: String): ReleaseInfo? {
        return try {
            val gson = com.google.gson.Gson()
            val obj = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            val tag = obj.get("tag_name")?.asString ?: return null
            val body = obj.get("body")?.asString ?: ""
            val assets = obj.getAsJsonArray("assets") ?: return null
            var url = ""
            var size = 0L
            for (el in assets) {
                val a = el.asJsonObject
                val name = a.get("name")?.asString ?: continue
                if (name.endsWith(".apk")) {
                    url = a.get("browser_download_url")?.asString ?: continue
                    size = a.get("size")?.asLong ?: 0L
                    break
                }
            }
            if (url.isEmpty()) null else ReleaseInfo(tag, body, url, size)
        } catch (e: Exception) {
            null
        }
    }

    /** 比较版本号：latest > current 则为 true */
    private fun isNewer(latestTag: String, current: String): Boolean {
        fun parts(v: String) = v.replace("v", "").split(".").map { it.toIntOrNull() ?: 0 }
        val l = parts(latestTag)
        val c = parts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = if (i < l.size) l[i] else 0
            val cv = if (i < c.size) c[i] else 0
            if (lv != cv) return lv > cv
        }
        return false
    }

    /* ------------------------------------------------------------------ */
    /* 下载 + 安装                                                        */
    /* ------------------------------------------------------------------ */

    /**
     * 下载并安装 APK。
     * @param onProgress 下载进度 0-100
     * @param onComplete (成功, 提示文案)
     */
    fun downloadAndInstall(
        releaseInfo: ReleaseInfo,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val nm = NotificationManagerCompat.from(context)
            val channelId = "update_download"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "更新下载", NotificationManager.IMPORTANCE_LOW)
                )
            }
            val notifyId = 1001

            fun notify(title: String, text: String, progress: Int, ongoing: Boolean) {
                nm.notify(
                    notifyId,
                    NotificationCompat.Builder(context, channelId)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setProgress(100, progress, false)
                        .setOngoing(ongoing)
                        .build()
                )
            }

            try {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update_apks")
                if (!dir.exists()) dir.mkdirs()
                val temp = File(dir, "guoziri_${releaseInfo.tagName}.apk.tmp")
                val final = File(dir, "guoziri_${releaseInfo.tagName}.apk")

                var lastError = "所有加速源均不可用"

                for (acc in downloadAccelerators) {
                    try {
                        val startAt = if (temp.exists()) temp.length() else 0L
                        var conn: HttpURLConnection? = null
                        try {
                            // 加速源拼在完整 URL 前面；直连（空串）直接用原地址
                            val fullUrl = acc + releaseInfo.apkUrl
                            Log.i(TAG, "下载尝试: $fullUrl")
                            conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                                requestMethod = "GET"
                                connectTimeout = 15000
                                readTimeout = 30000
                                instanceFollowRedirects = true
                                setRequestProperty("User-Agent", "Guoziri-App")
                                if (startAt > 0) setRequestProperty("Range", "bytes=$startAt-")
                            }
                            val code = conn.responseCode
                            Log.i(TAG, "下载响应: HTTP $code")
                            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                                throw IllegalStateException("HTTP $code")
                            }

                            val total = if (code == HttpURLConnection.HTTP_PARTIAL) {
                                conn.getHeaderField("Content-Range")?.substringAfter("/")?.toLongOrNull()
                                    ?: releaseInfo.apkSize
                            } else {
                                conn.contentLengthLong.takeIf { it > 0 } ?: releaseInfo.apkSize
                            }

                            val input = conn.inputStream
                            val out = FileOutputStream(temp, true)
                            val buf = ByteArray(8192)
                            var got = startAt
                            var lastPct = -1

                            notify("正在下载 ${releaseInfo.tagName}", "0%", 0, true)

                            while (true) {
                                val n = input.read(buf)
                                if (n == -1) break
                                out.write(buf, 0, n)
                                got += n
                                if (total > 0) {
                                    val pct = ((got * 100) / total).toInt().coerceIn(0, 100)
                                    if (pct != lastPct) {
                                        lastPct = pct
                                        withContext(Dispatchers.Main) {
                                            onProgress(pct)
                                            notify("正在下载 ${releaseInfo.tagName}", "$pct%", pct, true)
                                        }
                                    }
                                }
                            }
                            input.close()
                            out.close()
                        } finally {
                            conn?.disconnect()
                        }

                        // 下载完成，原子替换
                        if (temp.renameTo(final)) {
                            nm.cancel(notifyId)
                            withContext(Dispatchers.Main) { installApk(final, onComplete) }
                            return@launch
                        }
                        lastError = "保存下载文件失败"
                    } catch (e: Exception) {
                        lastError = e.message ?: "下载失败"
                        // 该源失败，删掉半截文件再试下一个（断点续传只在同源有效）
                        try { temp.delete() } catch (_: Exception) {}
                    }
                }
                nm.cancel(notifyId)
                withContext(Dispatchers.Main) { onComplete(false, "下载失败：$lastError") }
            } catch (e: Exception) {
                nm.cancel(notifyId)
                withContext(Dispatchers.Main) { onComplete(false, "下载失败：${e.message}") }
            }
        }
    }

    /** 调用系统安装程序 */
    private fun installApk(apkFile: File, onComplete: (Boolean, String) -> Unit) {
        if (!apkFile.exists()) {
            onComplete(false, "安装包不存在")
            return
        }
        try {
            // Android 8.0+ 需要用户先授权「安装未知应用」
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                onComplete(false, "请先在系统设置中允许「安装未知应用」")
                return
            }

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onComplete(true, "已打开安装程序")

            // 安装完成后清理下载缓存
            CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay(10_000)
                try {
                    apkFile.parentFile?.listFiles()?.forEach { it.delete() }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            onComplete(false, "无法启动安装程序：${e.message}")
        }
    }

    /** 系统弹窗版提示（备用，Compose 版已用 AppAlert） */
    fun showUpdateDialog(
        activity: Activity,
        releaseInfo: ReleaseInfo,
        onDownload: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setTitle("发现新版本 ${releaseInfo.tagName}")
            .setMessage(releaseInfo.body.ifBlank { "有新版本可用，建议更新" })
            .setPositiveButton("立即更新") { _, _ -> onDownload() }
            .setNegativeButton("稍后再说", null)
            .setCancelable(false)
            .show()
    }
}
