package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LedgerPage(
    repo: StoreRepo,
    anchors: GuideAnchors,
    onAdd: () -> Unit,
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
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("记账", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("${todayList.size}", "今日笔数", Modifier.weight(1f))
                StatCard(money(todaySum), "今日花销", Modifier.weight(1f))
                StatCard(money(monthSum), "本月累计", Modifier.weight(1f))
                StatCard("${groups.size}", "记账天数", Modifier.weight(1f))
            }
        }

        if (sorted.isEmpty()) {
            EmptyState("🛒", "还没有记录\n点右下角 ＋ 记第一笔")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp),
            ) {
                for ((date, items) in groups) {
                    val sum = items.sumOf { it.totalCost ?: 0.0 }
                    val label = (if (date == t) "今天 · " else "") + fmtDay(date) + " " + weekDay(date)
                    item(key = "h$date") {
                        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp))
                        GroupCard {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${items.size} 笔", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(money(sum), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    items(items, key = { it.id }) { p ->
                        PurchaseRow(p, onEdit = { onEdit(p) }, onDelete = { onRequestDelete(p) })
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Box(
            Modifier.padding(start = 8.dp).size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(CAT_COLOR[p.category] ?: Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center,
        ) { Text(CAT_ICON[p.category] ?: "🛒", fontSize = 17.sp) }

        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val wTxt = weightText(p)
                if (p.pricePerJin != null) Text(money(p.pricePerJin) + "/斤", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                if (wTxt.isNotBlank()) Text(wTxt, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        Text(p.totalCost?.let { money(it) } ?: "—", fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.padding(end = 4.dp))
        TextButton(onClick = onEdit) { Text("编辑", fontSize = 14.sp) }
        TextButton(onClick = onDelete) { Text("删除", fontSize = 14.sp, color = IosRed) }
    }
}