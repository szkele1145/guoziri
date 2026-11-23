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

    /**
     * 下载分块策略（IDM 式）：块多而小（32 块，12MB ≈ 380KB/块），
     * 同时只开 6 个连接。单块失败重试代价低，也不会把网络打超时。
     */
    private val CHUNKS = 32
    private val CONCURRENCY = 6

    private fun feedUrl(track: String) = "https://github.com/$repoOwner/$repoName/releases.atom"

    /**
     * 按 tag 查 Release。
     *
     * 注意：不能用 /releases/latest —— 它永远返回最新的「正式版」，
     * 拿不到 test 通道。必须用 /releases/tags/{tag}。
     */
    private fun apiUrl(track: String) =
        "https://api.github.com/repos/$repoOwner/$repoName/releases/tags/$track"

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
        val tagName: String,       // GitHub tag（releases / test / v1.3 ...）
        val releaseName: String,   // Release 标题（"过日子 v1.3"）
        val body: String,
        val apkUrl: String,
        val apkSize: Long,
        val channel: Channel,
    ) {
    /**
     * 展示用的版本号。
     * 优先级：标题 > APK 文件名 > tag；全抽不到返回 "?"。
     * （"过日子 v1.3" → 1.3；"guoziri-v1.3.apk" → 1.3，不含 .apk 后缀）
     */
    val displayVersion: String
        get() {
            val ver = Regex("""v(\d+(?:\.\d+)*(?:-test\d+)?)""", RegexOption.IGNORE_CASE)
            // 1) 从 Release 标题抽 "v数字"
            ver.find(releaseName)?.groupValues?.get(1)?.let { return it }
            // 2) 从 APK 文件名抽（兜底路径没有标题）
            ver.find(apkUrl.substringAfterLast('/'))?.groupValues?.get(1)?.let { return it }
            // 3) 从 tag 抽
            ver.find(tagName)?.groupValues?.get(1)?.let { return it }
            return "?"
        }
    }

    private val currentVersionName: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    private val currentVersionCode: Long = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
        }
    }.getOrDefault(0L)

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
                Log.i(TAG, "当前版本: $currentVersionName (versionCode $currentVersionCode)，开始检查更新")
                val found = mutableListOf<ReleaseInfo>()
                var lastError: String? = null

                for (channel in Channel.entries) {
                    val info = queryChannel(channel)
                    if (info == null) {
                        Log.i(TAG, "[${channel.label}] 未查到可用版本")
                        continue
                    }
                    // 版本比较：通道版本必须比当前新才提示，否则会一直提示"有更新"
                    if (!isNewer(info.displayVersion, currentVersionName)) {
                        Log.i(TAG, "[${channel.label}] ${info.displayVersion} 不比当前 $currentVersionName 新，跳过")
                        continue
                    }
                    Log.i(TAG, "[${channel.label}] 发现新版本 ${info.displayVersion}")
                    found.add(info)
                }

                if (found.isEmpty()) {
                    onResult(emptyList(), lastError)
                } else {
                    onResult(found, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查更新异常", e)
                onResult(emptyList(), e.message ?: "检查更新失败")
            }
        }
    }

    /**
     * 语义化版本比较：a 是否比 b 新。
     * 支持 "1.3.1"、"1.3.1-test1"（-testN 视为同版本号的预发布，不算更新）。
     */
    private fun isNewer(a: String, b: String): Boolean {
        fun parts(v: String): List<Int> =
            v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val pa = parts(a); val pb = parts(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        // 数字部分相同：带 -test 后缀的不算新版本
        val aTest = a.contains("-test")
        val bTest = b.contains("-test")
        return if (aTest == bTest) false else bTest   // a 是预发布且 b 是正式 → 不更新
    }

    /** 查单个渠道的最新版本 */
    private fun queryChannel(channel: Channel): ReleaseInfo? {
        // 1) 优先按 tag 查 Release（能准确区分 releases / test 两个通道）
        for (acc in metaAccelerators) {
            try {
                val json = fetchText(acc + apiUrl(channel.id))
                if (json.isNullOrBlank()) continue
                val info = parseRelease(json, channel)
                if (info != null) {
                    Log.i(TAG, "[${channel.label}] API 命中: ${info.tagName}")
                    return info
                }
            } catch (e: Exception) {
                Log.i(TAG, "[${channel.label}] API 失败 [${acc.ifEmpty { "直连" }}]: ${e.message}")
            }
        }

        // 2) 直接按 tag 抓 expanded_assets（不依赖 API，最稳）
        val directApk = fetchApkUrl(channel.id)
        if (directApk != null) {
            Log.i(TAG, "[${channel.label}] assets 命中: ${channel.id}")
            return ReleaseInfo(channel.id, "", "", directApk, 0L, channel)
        }

        // 3) 降级：Atom feed 只返回最新那条，无法区分通道，仅作参考
        var latestTag: String? = null
        for (acc in metaAccelerators) {
            try {
                val feed = fetchText(acc + feedUrl(channel.id), accept = "application/atom+xml")
                if (feed.isNullOrBlank()) continue
                latestTag = parseFeedTag(feed)
                if (!latestTag.isNullOrBlank()) break
            } catch (e: Exception) {
                Log.i(TAG, "feed 失败 [${acc.ifEmpty { "直连" }}]: ${e.message}")
            }
        }

        if (latestTag != null) {
            val apkUrl = fetchApkUrl(latestTag)
            if (apkUrl != null) {
                Log.i(TAG, "[${channel.label}] feed 兜底命中: $latestTag")
                return ReleaseInfo(latestTag, "", "", apkUrl, 0L, channel)
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
            val name = obj.get("name")?.asString ?: ""
            val body = obj.get("body")?.asString ?: ""
            val assets = obj.getAsJsonArray("assets") ?: return null
            var url = ""
            var size = 0L
            for (el in assets) {
                val a = el.asJsonObject
                val n = a.get("name")?.asString ?: continue
                if (n.endsWith(".apk")) {
                    url = a.get("browser_download_url")?.asString ?: continue
                    size = a.get("size")?.asLong ?: 0L
                    break
                }
            }
            if (url.isEmpty()) null
            else ReleaseInfo(tag, name, stripMarkdown(body), url, size, channel)
        } catch (e: Exception) {
            null
        }
    }

    /** 去掉 markdown 符号，只留可读文字（用于弹窗摘要） */
    private fun stripMarkdown(body: String): String {
        if (body.isBlank()) return ""
        return body
            .replace(Regex("""^#{1,6}\s*""", RegexOption.MULTILINE), "")   // 标题井号
            .replace("**", "")                                             // 加粗
            .replace("*", "")
            .replace("`", "")
            .replace(Regex("""\n{2,}"""), "\n")                            // 多空行合并
            .trim()
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
                val final = File(dir, "guoziri_${releaseInfo.tagName}.apk")

                var lastError = "所有加速源均不可用"

                for (acc in downloadAccelerators) {
                    try {
                        Log.i(TAG, "下载尝试 [${acc.ifEmpty { "直连" }}]: ${releaseInfo.apkUrl}")
                        multiThreadDownload(
                            url = acc + releaseInfo.apkUrl,
                            outFile = final,
                            onProgress = { pct ->
                                // 通知栏 + 应用内弹窗
                                mainScope.launch {
                                    onProgress(pct)
                                    notify("$pct%", pct)
                                }
                            },
                        )
                        Log.i(TAG, "下载完成 [${acc.ifEmpty { "直连" }}]")
                        nm.cancel(notifyId)
                        withContext(Dispatchers.Main) { installApk(final, onComplete) }
                        return@launch
                    } catch (e: Exception) {
                        Log.i(TAG, "下载源失败 [${acc.ifEmpty { "直连" }}]: ${e.message}")
                        lastError = e.message ?: "下载失败"
                        // 换源时清掉残留分块
                        cleanChunks(final)
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

    private val mainScope = CoroutineScope(Dispatchers.Main)

    /** 分块文件的命名前缀 */
    private fun chunkFile(outFile: File, index: Int) =
        File(outFile.parent, outFile.name + ".part$index")

    /** 清理残留分块 */
    private fun cleanChunks(outFile: File) {
        outFile.parentFile?.listFiles()?.forEach {
            if (it.name.startsWith(outFile.name + ".part")) it.delete()
        }
        if (outFile.name.endsWith(".tmp")) outFile.delete()
    }

    /**
     * 多线程分块下载。
     *
     * 策略（IDM 式）：切成 CHUNKS 个小块，但同时只有 CONCURRENCY 个连接在跑。
     * 块小 → 单块失败重试代价低；并发少 → 不会把网络/服务器打超时。
     * 每块独立 part 文件，天然支持断点续传。
     */
    private fun multiThreadDownload(
        url: String,
        outFile: File,
        onProgress: (Int) -> Unit,
    ) {
        // ---- 1. 探测大小 & Range 支持 ----
        var totalSize = -1L
        var rangeSupported = false
        var head: HttpURLConnection? = null
        try {
            head = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Guoziri-App")
                setRequestProperty("Range", "bytes=0-0")
            }
            val code = head.responseCode
            Log.i(TAG, "探测响应: HTTP $code")
            if (code == HttpURLConnection.HTTP_PARTIAL) {
                rangeSupported = true
                totalSize = head.getHeaderField("Content-Range")?.substringAfter("/")?.toLongOrNull() ?: -1L
            } else if (code == HttpURLConnection.HTTP_OK) {
                totalSize = head.contentLengthLong
            }
        } finally {
            head?.disconnect()
        }
        if (totalSize <= 0) throw IllegalStateException("无法获取文件大小")
        Log.i(TAG, "文件大小: %.2f MB, Range: %s, 分块: %d, 并发: %d"
            .format(totalSize / 1048576f, rangeSupported, CHUNKS, CONCURRENCY))

        // 不支持 Range 就退回单线程
        if (!rangeSupported) {
            Log.i(TAG, "服务器不支持 Range，退回单线程")
            singleThreadDownload(url, outFile, totalSize, onProgress)
            return
        }

        // ---- 2. 分块（块数多、块小）----
        val chunkSize = totalSize / CHUNKS
        val ranges = mutableListOf<Triple<Int, Long, Long>>()
        var start = 0L
        for (i in 0 until CHUNKS) {
            val end = if (i == CHUNKS - 1) totalSize - 1 else start + chunkSize - 1
            ranges.add(Triple(i, start, end))
            start = end + 1
        }

        val downloaded = java.util.concurrent.atomic.AtomicLong(0)
        val pending = java.util.concurrent.ConcurrentLinkedQueue<Triple<Int, Long, Long>>(ranges)
        val failedChunks = java.util.concurrent.ConcurrentLinkedQueue<Int>()

        var lastPct = -1

        // ---- 3. 有限并发执行 ----
        val pool = java.util.concurrent.Executors.newFixedThreadPool(CONCURRENCY)
        try {
            val futures = (0 until CONCURRENCY).map {
                pool.submit {
                    while (true) {
                        val task = pending.poll() ?: return@submit
                        val (index, s, e) = task
                        val expected = e - s + 1
                        val part = chunkFile(outFile, index)
                        if (part.exists() && part.length() == expected) {
                            downloaded.addAndGet(expected)
                            continue
                        }
                        var attempt = 0
                        var ok = false
                        while (attempt < 4 && !ok) {
                            attempt++
                            var conn: HttpURLConnection? = null
                            try {
                                val have = if (part.exists()) part.length() else 0L
                                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                                    requestMethod = "GET"
                                    connectTimeout = 12000
                                    readTimeout = 60000   // 读超时放宽，弱网下 30s 不够
                                    instanceFollowRedirects = true
                                    setRequestProperty("User-Agent", "Guoziri-App")
                                    setRequestProperty("Range", "bytes=${s + have}-$e")
                                }
                                val code = conn.responseCode
                                if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                                    throw IllegalStateException("HTTP $code")
                                }
                                conn.inputStream.use { input ->
                                    FileOutputStream(part, true).use { out ->
                                        val buf = ByteArray(131072)   // 128KB
                                        while (true) {
                                            val n = input.read(buf)
                                            if (n == -1) break
                                            out.write(buf, 0, n)
                                            val g = downloaded.addAndGet(n.toLong())
                                            val pct = ((g * 100) / totalSize).toInt().coerceIn(0, 99)
                                            if (pct != lastPct) {
                                                lastPct = pct
                                                onProgress(pct)
                                            }
                                        }
                                    }
                                }
                                ok = true
                            } catch (e: Exception) {
                                Log.i(TAG, "分块 $index 第 $attempt 次失败: ${e.message}")
                            } finally {
                                conn?.disconnect()
                            }
                        }
                        if (!ok) {
                            failedChunks.add(index)
                            throw IllegalStateException("分块 $index 下载失败")
                        }
                    }
                }
            }
            futures.forEach { it.get() }
        } finally {
            pool.shutdown()
        }

        // ---- 4. 校验 + 合并 ----
        onProgress(100)
        FileOutputStream(outFile).use { merged ->
            ranges.forEach { (index, s, e) ->
                chunkFile(outFile, index).inputStream().use { input ->
                    input.copyTo(merged, 65536)
                }
            }
        }
        if (outFile.length() != totalSize) {
            throw IllegalStateException("合并后大小不符: ${outFile.length()} != $totalSize")
        }
        cleanChunks(outFile)
        Log.i(TAG, "多线程下载完成: %.2f MB".format(outFile.length() / 1048576f))
    }

    /** 单线程下载（Range 不可用时的降级路径） */
    private fun singleThreadDownload(
        url: String,
        outFile: File,
        totalSize: Long,
        onProgress: (Int) -> Unit,
    ) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Guoziri-App")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${conn.responseCode}")
            }
            val tmp = File(outFile.parent, outFile.name + ".single.tmp")
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { out ->
                    val buf = ByteArray(65536)
                    var got = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n == -1) break
                        out.write(buf, 0, n)
                        got += n
                        if (totalSize > 0) {
                            onProgress(((got * 100) / totalSize).toInt().coerceIn(0, 99))
                        }
                    }
                }
            }
            if (!tmp.renameTo(outFile)) throw IllegalStateException("保存文件失败")
        } finally {
            conn?.disconnect()
        }
    }

    /** 调用系统安装程序 */
    private fun installApk(apkFile: File, onComplete: (Boolean, String) -> Unit) {
        if (!apkFile.exists()) {
            onComplete(false, "安装包不存在")
            return
        }
        try {
            // Android 8.0+：没有「安装未知应用」权限时，直接跳到本 App 的授权页，
            // 用户打开开关后回来重试即可（不用自己去设置里翻）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                onComplete(false, "请打开「允许安装未知应用」开关，然后重新点一次更新")
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
