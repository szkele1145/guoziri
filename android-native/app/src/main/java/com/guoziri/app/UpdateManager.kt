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
import android.util.Log
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
 * 支持两条通道：
 *  - 正式版（releases）：稳定版本，推荐更新
 *  - 测试版（test）    ：抢先体验，不建议更新（会在界面上明确标注）
 *
 * 检查时两个版本都会列出来，由用户自己选择更新哪一个。
 */
class UpdateManager(private val context: Context) {

    private val TAG = "UpdateManager"

    private val repoOwner = "szkele1145"
    private val repoName = "guoziri"

    /**
     * 加速源分组。
     *
     * 实测结论（tools/probe_accelerators.py）：
     *  - 查版本（feed / REST API）只有 gh-proxy.com 和直连可用
     *  - 下载 APK 是另一条路径，ghproxy.net / gh-proxy.com / 直连可用
     *  - 两类操作必须用不同的源列表，且每个列表都以直连兜底
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

    private fun feedUrl(track: String) = "https://github.com/$repoOwner/$repoName/releases.atom"
    private fun apiUrl(track: String) =
        "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

    /** 版本渠道 */
    enum class Channel(val id: String, val label: String, val recommend: Boolean) {
        /** 正式版：稳定通道 */
        STABLE("releases", "正式版", true),

        /** 测试版：抢先体验 */
        TEST("test", "测试版", false);

        companion object {
            fun from(id: String?): Channel? = entries.firstOrNull { it.id == id }
        }
    }

    /** 一个可安装的版本 */
    data class ReleaseInfo(
        val tagName: String,
        val body: String,
        val apkUrl: String,
        val apkSize: Long,
        val channel: Channel,
    ) {
        /** 展示用的版本号（去掉 v 前缀） */
        val displayVersion: String get() = tagName.removePrefix("v")
    }

    private val currentVersionName: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    /* ------------------------------------------------------------------ */
    /* 检查更新                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * 检查更新（正式版 + 测试版一起查）。
     *
     * @param onResult (可用版本列表, 错误信息)
     */
    fun checkUpdate(onResult: (List<ReleaseInfo>, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "当前版本: $currentVersionName，开始检查更新")
                val found = mutableListOf<ReleaseInfo>()
                var lastError: String? = null

                for (channel in Channel.entries) {
                    val info = queryChannel(channel)
                    if (info == null) {
                        Log.i(TAG, "[${channel.label}] 未查到可用版本")
                        continue
                    }
                    Log.i(TAG, "[${channel.label}] 查到 ${info.tagName}")
                    found.add(info)
                }

                if (found.isEmpty()) {
                    Log.i(TAG, "检查更新失败：$lastError")
                    onResult(emptyList(), lastError ?: "没有找到可用版本")
                } else {
                    onResult(found, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查更新异常", e)
                onResult(emptyList(), e.message ?: "检查更新失败")
            }
        }
    }

    /** 查单个渠道的最新版本 */
    private fun queryChannel(channel: Channel): ReleaseInfo? {
        // 1) Atom feed 取版本号
        var latestTag: String? = null
        for (acc in metaAccelerators) {
            try {
                val feed = fetchText(acc + feedUrl(channel.id), accept = "application/atom+xml")
                if (feed.isNullOrBlank()) continue
                latestTag = parseFeedTag(feed)
                if (!latestTag.isNullOrBlank()) break
            } catch (e: Exception) {
                Log.i(TAG, "feed 失败 [${channel.label}/${acc.ifEmpty { "直连" }}]: ${e.message}")
            }
        }

        // 2) REST API 取完整信息（含 APK 地址）
        for (acc in metaAccelerators) {
            try {
                val json = fetchText(acc + apiUrl(channel.id))
                if (json.isNullOrBlank()) continue
                val info = parseRelease(json, channel)
                if (info != null) return info
            } catch (_: Exception) {
                continue
            }
        }

        // 3) 兜底：expanded_assets 抓 APK 链接
        if (latestTag != null) {
            val apkUrl = fetchApkUrl(latestTag)
            if (apkUrl != null) {
                return ReleaseInfo(latestTag, "", apkUrl, 0L, channel)
            }
        }
        return null
    }

    /** 从 Atom feed 里解析最新的 tag（只在 <entry> 块里找） */
    private fun parseFeedTag(feed: String): String? {
        return try {
            val i = feed.indexOf("<entry>")
            if (i < 0) return null
            val m = java.util.regex.Pattern.compile("<id>[^<]*/([^/<]+)</id>")
                .matcher(feed.substring(i))
            if (m.find()) m.group(1) else null
        } catch (e: Exception) {
            null
        }
    }

    /** 解析 GitHub Release JSON，找出 APK 资源 */
    private fun parseRelease(json: String, channel: Channel): ReleaseInfo? {
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
            if (url.isEmpty()) null else ReleaseInfo(tag, body, url, size, channel)
        } catch (e: Exception) {
            null
        }
    }

    /** 用 expanded_assets 接口拿 APK 链接（纯服务端 HTML） */
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
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection?.disconnect()
        }
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

            fun notify(text: String, progress: Int) {
                nm.notify(
                    notifyId,
                    NotificationCompat.Builder(context, channelId)
                        .setContentTitle("正在下载 ${releaseInfo.displayVersion}")
                        .setContentText(text)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setProgress(100, progress, false)
                        .setOngoing(true)
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

                            notify("0%", 0)

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
                                            notify("$pct%", pct)
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
                        Log.i(TAG, "下载源失败 [${acc.ifEmpty { "直连" }}]: ${e.message}")
                        lastError = e.message ?: "下载失败"
                        // 换源时清掉半截文件（断点续传只在同源有效）
                        try { temp.delete() } catch (_: Exception) {}
                    }
                }
                nm.cancel(notifyId)
                withContext(Dispatchers.Main) { onComplete(false, "下载失败：$lastError") }
            } catch (e: Exception) {
                nm.cancel(notifyId)
                Log.e(TAG, "下载异常", e)
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
}
