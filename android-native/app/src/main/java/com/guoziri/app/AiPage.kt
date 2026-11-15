package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

/**
 * AI 页，结构对齐 web 版 renderAI：
 * 一键总结(3 张 ai-card) → 自由提问 → 试试这些(4 条 prompt)
 */
@Composable
fun AiPage(repo: StoreRepo, chatState: ChatState, snackbar: (String) -> Unit) {
    val t = today()
    val hasP = repo.purchases.any { it.date == t }
    val hasD = repo.diary.any { it.date == t }
    val hasKey = repo.settings.apiKey.isNotBlank()
    val hidden = repo.settings.hiddenEnabled
    val scope = rememberCoroutineScope()
    val c = LocalAppColors.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Dimens.TabH + 24.dp),
    ) {
        PageTitle(
            "AI 助手",
            subtitle = if (hasKey) "已连接 · 数据仅在本机处理" else "先到设置页填 API Key",
        )

        GroupTitle("一键总结")
        GroupCard {
            AiCard(
                icon = AppIcons.Money,
                bg = Color(0xFFE8F0FE),
                tint = Color(0xFF4285F4),
                title = "总结今天花销",
                sub = "值不值、哪儿能省",
                dim = !hasP,
            ) {
                if (!hasKey) { snackbar("先到设置页填 API Key"); return@AiCard }
                scope.launch { chatState.startConversation("cost", repo) }
            }
            CardDivider()
            AiCard(
                icon = AppIcons.Restaurant,
                bg = Color(0xFFE8F5E9),
                tint = Color(0xFF34A853),
                title = "总结今天营养",
                sub = "补得怎么样、明天买什么",
                dim = !hasP,
            ) {
                if (!hasKey) { snackbar("先到设置页填 API Key"); return@AiCard }
                scope.launch { chatState.startConversation("nutrition", repo) }
            }
            if (hidden && hasD) {
                CardDivider()
                AiCard(
                    icon = AppIcons.Book,
                    bg = Color(0xFFF3E8FF),
                    tint = Color(0xFF9C27B0),
                    title = "结合日记总结今天",
                    sub = "检测到今日日记",
                    dim = false,
                ) {
                    if (!hasKey) { snackbar("先到设置页填 API Key"); return@AiCard }
                    scope.launch { chatState.startConversation("diary", repo) }
                }
            }
        }

        GroupTitle("自由提问")
        GroupCard {
            AiCard(
                icon = AppIcons.Chat,
                bg = Color(0xFFE8F0FE),
                tint = Color(0xFF4285F4),
                title = "随便聊聊",
                sub = "食谱、省钱、囤货…什么都能问",
                dim = false,
            ) {
                if (!hasKey) { snackbar("先到设置页填 API Key"); return@AiCard }
                scope.launch { chatState.openBlank() }
            }
        }

        GroupTitle("试试这些")
        GroupCard {
            listOf(
                "这周买什么菜性价比高？",
                "孩子不爱吃青菜怎么办？",
                "冰箱里该常备什么？",
                "怎么做一顿 20 元的营养晚餐？",
            ).forEachIndexed { i, q ->
                if (i > 0) CardDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Dimens.RowH)
                        .clickable {
                            if (!hasKey) { snackbar("先到设置页填 API Key"); return@clickable }
                            scope.launch { chatState.sendPrompt(q, repo) }
                        }
                        .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
                ) {
                    Text(
                        q,
                        fontSize = 15.sp,
                        color = c.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = c.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    if (chatState.sheetOpen) {
        ChatSheet(chatState, repo) { snackbar(it) }
    }
}

/** .ai-card：42 图标(r10) + 标题/副标题 + ›，min-height 68，padding 14 16，gap 14 */
@Composable
fun AiCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    tint: Color,
    title: String,
    sub: String,
    dim: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .pressable { onClick() }
            .padding(horizontal = Dimens.CardPad, vertical = 14.dp)
            .alpha(if (dim) 0.45f else 1f),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(bg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, fontSize = Dimens2.Body, color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                sub,
                fontSize = Dimens2.Caption,
                color = c.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Icon(
            AppIcons.ChevronRight,
            contentDescription = null,
            tint = c.tertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}
