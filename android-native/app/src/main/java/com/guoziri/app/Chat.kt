package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        if (messages.isEmpty()) messages.add(
            ChatMsg("bot", "你好，我是「过日子」的 AI 助手 🌟\n今天的记账和日记会自动带给我，直接问就行。")
        )
    }

    fun close() { sheetOpen = false; messages.clear() }

    /** 打开对话并直接提问（AI 页「试试这些」用） */
    suspend fun sendPrompt(q: String, repo: StoreRepo) {
        if (messages.isEmpty()) openBlank() else sheetOpen = true
        send(q, repo)
    }

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
            val r = Nutrition.assess(purchases, fam, repo.settings.customFoods)
            val t = r.total
            lines.add(
                "营养摄入概况：热量%.0fkcal、蛋白质%.1fg、脂肪%.1fg、碳水%.1fg、纤维%.1fg、维C%.0fmg、钙%.0fmg、铁%.1fmg".format(
                    t["calories"] ?: 0.0, t["protein"] ?: 0.0, t["fat"] ?: 0.0, t["carbs"] ?: 0.0,
                    t["fiber"] ?: 0.0, t["vitaminC"] ?: 0.0, t["calcium"] ?: 0.0, t["iron"] ?: 0.0
                )
            )
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
                    .url(s.apiBaseUrl.trimEnd('/') + "/chat/completions")
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

/**
 * AI 对话弹窗，结构对齐 web 版 #sheet-ai（.ai-panel 88dvh / .ai-chat / .ai-input）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSheet(state: ChatState, repo: StoreRepo, snackbar: (String) -> Unit) {
    val c = LocalAppColors.current
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val sending = state.messages.lastOrNull()?.text?.endsWith("▍") == true
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { if (!sending) state.close() },
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = null,
        containerColor = c.bgElevated,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
            SheetGrabber()
            SheetHead(
                title = "AI 助手",
                leftText = "关闭",
                rightText = "新对话",
                onLeft = { if (!sending) state.close() },
                onRight = { state.messages.clear() },
            )

            // .ai-chat：padding 16，gap 12，bg --bg
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().background(c.bg),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.messages) { m ->
                    val isUser = m.role == "user"
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.78f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (isUser) 18.dp else 6.dp,
                                        bottomEnd = if (isUser) 6.dp else 18.dp,
                                    ),
                                )
                                .background(if (isUser) c.tint else c.bgElevated)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(
                                m.text,
                                fontSize = Dimens2.Msg,
                                lineHeight = 24.sp,
                                color = if (isUser) Color.White else c.label,
                            )
                        }
                    }
                }
            }

            // .ai-input：padding 10 14 14，bg elevated，顶部 0.5 线
            Column(Modifier.fillMaxWidth().background(c.bgElevated)) {
                androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = c.separator)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // input：fill 底 r18 p9 16
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.fill)
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        if (input.isEmpty()) {
                            Text("想问点什么…", fontSize = Dimens2.Msg, color = c.tertiary)
                        }
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            textStyle = TextStyle(fontSize = Dimens2.Msg, color = c.label, lineHeight = 22.sp),
                            cursorBrush = SolidColor(c.tint),
                            maxLines = 4,
                            enabled = !sending,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    // send-btn：36x36 圆
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(c.tint)
                            .alpha(if (input.isNotBlank() && !sending) 1f else 0.4f)
                            .clickable(enabled = input.isNotBlank() && !sending) {
                                val q = input.trim()
                                input = ""
                                scope.launch { state.send(q, repo) }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            AppIcons.Send,
                            contentDescription = "发送",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }
}
