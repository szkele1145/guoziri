package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AiPage(repo: StoreRepo, chatState: ChatState, anchors: GuideAnchors, snackbar: (String) -> Unit) {
    val t = today()
    val hasP = repo.purchases.any { it.date == t }
    val hasD = repo.diary.any { it.date == t }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Text("AI 助手", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp))
        Text(if (repo.settings.apiKey.isNotBlank()) "已连接 · 数据仅在本机处理" else "先到设置页填 API Key",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 20.dp))

        GroupTitle("一键总结")
        GroupCard {
            AiCard("💰", Color(0xFFE8F0FE), "总结今天花销", "值不值、哪儿能省", enabled = hasP, anchorId = "ai-quick", anchors = anchors, anchorScope = "ai") {
                scope.launch { chatState.startConversation("cost", repo) }
            }
            AiCard("🥗", Color(0xFFE8F5E9), "总结今天营养", "补得怎么样、明天建议买什么", enabled = hasP) {
                scope.launch { chatState.startConversation("nutrition", repo) }
            }
            if (repo.settings.hiddenEnabled && hasD) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                AiCard("📖", Color(0xFFF3E8FF), "结合日记总结今天", "检测到今日日记", enabled = true) {
                    scope.launch { chatState.startConversation("diary", repo) }
                }
            }
        }

        GroupTitle("自由提问")
        GroupCard {
            AiCard("💬", Color(0xFFE8F0FE), "随便聊聊", "食谱、省钱、囤货…什么都能问", enabled = true) {
                chatState.openBlank()
            }
        }

        if (chatState.sheetOpen) {
            ChatSheet(chatState, repo) { snackbar(it) }
        }
    }
}

@Composable
fun AiCard(
    icon: String,
    bg: Color,
    title: String,
    sub: String,
    enabled: Boolean,
    anchorId: String? = null,
    anchors: GuideAnchors? = null,
    anchorScope: String = "ai",
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .let { m -> if (anchorId != null && anchors != null) m.guideAnchor(anchors, anchorId, anchorScope) else m },
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 19.sp)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}