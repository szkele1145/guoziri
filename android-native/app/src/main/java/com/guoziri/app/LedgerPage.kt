package com.guoziri.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LedgerPage(
    repo: StoreRepo,
    onEdit: (Purchase) -> Unit,
    onRequestDelete: (Purchase) -> Unit,
) {
    val purchases = repo.purchases
    val sortOrder = repo.settings.sortOrder
    val t = today()

    val sorted = remember(purchases.size, sortOrder) {
        if (sortOrder == "desc") purchases.sortedByDescending { it.date + (it.createdAt ?: "") }
        else purchases.sortedBy { it.date + (it.createdAt ?: "") }
    }
    val groups = remember(sorted) { sorted.groupBy { it.date } }
    val todayList = purchases.filter { it.date == t }
    val todaySum = todayList.sumOf { it.totalCost ?: 0.0 }
    val month = t.substring(0, 7)
    val monthSum = purchases.filter { it.date.startsWith(month) }.sumOf { it.totalCost ?: 0.0 }

    Column(Modifier.fillMaxSize()) {
        PageTitle(
            "记账",
            subtitle = if (todayList.isEmpty()) "今天还没有买菜记录"
            else "今天 ${todayList.size} 笔 · 共 ${money(todaySum)}",
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.PageH),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard("${todayList.size}", "今日笔数", Modifier.weight(1f))
            StatCard(moneyShort(todaySum), "今日花销", Modifier.weight(1f), accent = SemanticColors.Primary)
            StatCard(moneyShort(monthSum), "本月累计", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        if (sorted.isEmpty()) {
            EmptyState(
                Icons.Rounded.List,
                "还没有记录",
                hint = "点右下角 ＋ 记第一笔",
                actionText = "去记账",
                onAction = { onAdd() }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp),
            ) {
                for ((date, items) in groups) {
                    val sum = items.sumOf { it.totalCost ?: 0.0 }
                    val label = (if (date == t) "今天 · " else "") + fmtDay(date) + " " + weekDay(date)
                    item(key = "h$date") {
                        GroupTitle(label)
                        items(items, key = { it.id }) { p ->
                            PurchaseRow(p, onEdit = { onEdit(p) }, onDelete = { onRequestDelete(p) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseRow(p: Purchase, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = Dimens.PageH, vertical = 9.dp),
    ) {
        IconTile(
            icon = categoryIcon(p.category),
            bg = categoryTint(p.category),
            tint = categoryIconTint(p.category),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                p.name,
                fontSize = Dimens2.Body,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val wTxt = weightText(p)
            val parts = mutableListOf<String>()
            if (p.pricePerJin != null) parts.add("${money(p.pricePerJin)}/斤")
            if (wTxt.isNotBlank()) parts.add(wTxt)
            if (parts.isNotEmpty()) {
                Text(
                    parts.joinToString(" · "),
                    fontSize = Dimens2.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            p.totalCost?.let { money(it) } ?: "—",
            fontSize = Dimens2.Body,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(end = 4.dp),
        )
        // 更多按钮：编辑 / 删除
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(19.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("编辑", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(17.dp)) },
                    onClick = { menuOpen = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("删除", fontSize = 14.sp, color = SemanticColors.Error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.size(17.dp), tint = SemanticColors.Error) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}