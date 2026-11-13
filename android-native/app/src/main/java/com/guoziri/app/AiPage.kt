package com.guoziri.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AiPage(repo: StoreRepo, chatState: ChatState, snackbar: (String) -> Unit) {
    val t = today()
    val hasP = repo.purchases.any { it.date == t }
    val hasD = repo.diary.any { it.date == t }
    val hasKey = repo.settings.apiKey.isNotBlank()

    Column(Modifier.fillMaxSize()) {
        PageTitle(
            "AI 助手",
            subtitle = if (hasKey) "已连接 · 数据仅在本机处理"
            else "先到设置页填 API Key",
        )

        if (!hasKey) {
            Surface(
                shape = RoundedCornerShape(Dimens.CardR),
                color = IosOrange.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PageH),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                    Icon(
                        Icons.Rounded.InfoOutline,
                        contentDescription = null,
                        tint = IosOrange,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "还没有配置 API Key，AI 对话暂时不可用",
                        fontSize = Dimens2.Caption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        GroupTitle("一键总结")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            AiCard(
                Icons.Rounded.Payments, Color(0xFFE8F0FE), IosBlue,
                "总结今天花销", "值不值、哪儿能省",
                enabled = hasP && hasKey,
            ) { chatState.startConversation("cost", repo) }
            CardDivider()
            AiCard(
                Icons.Rounded.Restaurant, Color(0xFFE8F5E9), Color(0xFF34A853),
                "总结今天营养", "补得怎么样、明天建议买什么",
                enabled = hasP && hasKey,
            ) { chatState.startConversation("nutrition", repo) }
            if (repo.settings.hiddenEnabled && hasD) {
                CardDivider()
                AiCard(
                    Icons.Rounded.MenuBook, Color(0xFFF3E8FF), Color(0xFF9C27B0),
                    "结合日记总结今天", "检测到今日日记",
                    enabled = hasKey,
                ) { chatState.startConversation("diary", repo) }
            }
        }

        GroupTitle("自由提问")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            AiCard(
                Icons.Rounded.ChatBubbleOutline, Color(0xFFF2F2F7), IosGray,
                "随便聊聊", "食谱、省钱、囤货…什么都能问",
                enabled = hasKey,
            ) { chatState.openBlank() }
        }

        if (chatState.sheetOpen) {
            ChatSheet(chatState, repo) { snackbar(it) }
        }
    }
}

@Composable
fun AiCard(
    icon: ImageVector,
    bg: Color,
    tint: Color,
    title: String,
    sub: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = Dimens.CardPad, vertical = 13.dp),
    ) {
        IconTile(icon, bg, tint)
        Column(Modifier.weight(1f).padding(start = 13.dp)) {
            Text(
                title,
                fontSize = Dimens2.Body,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            Text(
                sub,
                fontSize = Dimens2.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.42f else 0.28f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.25f else 0.12f),
            modifier = Modifier.size(18.dp),
        )
    }
}
