package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMsg(val role: String, val text: String)

class ChatState {
    var sheetOpen by mutableStateOf(false)
    var messages = mutableStateListOf<ChatMsg>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun openBlank() {
        sheetOpen = true
        if (messages.isEmpty()) messages.add(ChatMsg("bot", "你好，我是「过日子」的 AI 助手 \ud83c\udf1f\n今天的记账和日记会自动带给我，直接问就行。"))
    }

    fun close() { sheetOpen = false; messages.clear() }

    suspend fun startConversation(mode: String, repo: StoreRepo) {
        sheetOpen = true
        val q = when (mode) {
            "cost" -> "请总结今天的买菜花销，值不值？哪里能省？"
            "nutrition" -> "评估一下今天全家的营养，缺什么？明天建议买什么？"
            "diary" -> "结合买菜记录和日记，总结一下今天过得怎么样？"
            else -> ""
        }
        if (q.isNotEmpty()) send(q, repo)
    }

    fun buildContext(repo: StoreRepo): String {
        val d = today()
        val fam = repo.settings.family
        val lines = mutableListOf(
            "你是一位贴心的家庭营养师和理财顾问。今天是 $d。",
            "家庭结构：${fam.adults} 位成人，${fam.children} 位儿童。"
        )
        val purchases = repo.purchases.filter { it.date == d }
        if (purchases.isNotEmpty()) {
            lines.add("今日买菜记录：")
            for (p in purchases) {
                val cost = p.totalCost?.let { " 共¥%.2f".format(it) } ?: ""
                val price = p.pricePerJin?.let { " @¥%.2f/斤".format(it) } ?: ""
                lines.add("- ${p.name} (${p.category})$cost$price")
            }
            val r = Nutrition.assess(purchases, fam)
            val t = r.total
            lines.add("营养摄入概况：热量%.0fkcal、蛋白质%.1fg、脂肪%.1fg、碳水%.1fg、纤维%.1fg、维C%.0fmg、钙%.0fmg、铁%.1fmg".format(
                t["calories"] ?: 0.0, t["protein"] ?: 0.0, t["fat"] ?: 0.0, t["carbs"] ?: 0.0,
                t["fiber"] ?: 0.0, t["vitaminC"] ?: 0.0, t["calcium"] ?: 0.0, t["iron"] ?: 0.0))
            val low = r.assessment.filter { it.percent < 70 }
            if (low.isNotEmpty()) lines.add("明显不足：" + low.joinToString("、") { "${it.label}(${it.percent}%)" })
        } else {
            lines.add("今日暂无买菜记录。")
        }
        val diary = repo.diary.filter { it.date == d }
        if (diary.isNotEmpty()) {
            lines.add("今日日记：")
            for (x in diary) {
                val w = if (!x.timeStart.isNullOrBlank() && !x.timeEnd.isNullOrBlank()) "${x.timeStart}-${x.timeEnd}" else (x.time ?: "")
                lines.add("- ($w) ${x.content.take(500)}")
            }
        }
        return lines.joinToString("\n")
    }

    suspend fun send(text: String, repo: StoreRepo) {
        messages.add(ChatMsg("user", text))
        val loadingIdx = messages.size
        messages.add(ChatMsg("bot", "▍"))

        val s = repo.settings
        if (s.apiKey.isBlank()) {
            messages[loadingIdx] = ChatMsg("bot", "未配置 API Key，请到设置页填写")
            return
        }

        val msgs = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", buildContext(repo)))
            for (m in messages.takeLast(21).dropLast(1)) {
                if (m.text != "▍") put(JSONObject().put("role", m.role).put("content", m.text.take(4000)))
            }
        }

        val body = JSONObject()
            .put("model", s.apiModel.ifBlank { "deepseek-chat" })
            .put("messages", msgs)
            .put("stream", true)
            .put("temperature", 0.7)
            .put("max_tokens", 2000)

        try {
            withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url(s.apiBaseUrl.trimEnd('.') + "/chat/completions")
                    .header("Authorization", "Bearer " + s.apiKey)
                    .post(RequestBody.create("application/json".toMediaType(), body.toString()))
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val msg = if (resp.code == 401) "Key 无效（401）" else "AI 服务返回 HTTP ${resp.code}"
                        launch(Dispatchers.Main) { messages[loadingIdx] = ChatMsg("bot", "出错了：$msg") }
                        return@use
                    }
                    val source = resp.body?.source() ?: return@use
                    val acc = StringBuilder()
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val payload = line.removePrefix("data:").trim()
                        if (payload == "[DONE]") break
                        try {
                            val delta = JSONObject(payload)
                                .getJSONArray("choices").getJSONObject(0)
                                .getJSONObject("delta").optString("content")
                            if (delta.isNotEmpty()) {
                                acc.append(delta)
                                val cur = acc.toString()
                                launch(Dispatchers.Main) { messages[loadingIdx] = ChatMsg("bot", cur + "▍") }
                            }
                        } catch (e: Exception) { }
                    }
                    val final = acc.toString()
                    launch(Dispatchers.Main) {
                        messages[loadingIdx] = ChatMsg("bot", final.ifBlank { "（空回复）" })
                    }
                }
            }
        } catch (e: Exception) {
            messages[loadingIdx] = ChatMsg("bot", "出错了：${e.message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSheet(state: ChatState, repo: StoreRepo, snackbar: (String) -> Unit) {
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val sending = state.messages.lastOrNull()?.text?.endsWith("▍") == true

    ModalBottomSheet(
        onDismissRequest = { if (!sending) state.close() },
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxWidth().height(560.dp)) {
            // 标题栏
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.PageH, top = 4.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI 助手",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { state.messages.clear() }) {
                    Text("新对话", fontSize = 13.sp, color = IosBlue)
                }
            }
            CardDivider(modifier = Modifier.padding(start = 0.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(state.messages) { m ->
                    val isUser = m.role == "user"
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    ) {
                        Box(
                            Modifier
                                .widthIn(max = 290.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp,
                                    ),
                                )
                                .background(if (isUser) IosBlue else MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 13.dp, vertical = 9.dp),
                        ) {
                            Text(
                                m.text,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // 输入区
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("想问点什么…", fontSize = 14.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                    enabled = !sending,
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (input.isNotBlank() && !sending) IosBlue
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    onClick = {
                        val q = input.trim()
                        if (q.isNotEmpty() && !sending) {
                            input = ""
                            val scope = CoroutineScope(Dispatchers.Main)
                            scope.launch { state.send(q, repo) }
                        }
                    },
                    enabled = input.isNotBlank() && !sending,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = "发送",
                            tint = if (input.isNotBlank() && !sending) Color.White
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
    // 自动滚动到底
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }
}
