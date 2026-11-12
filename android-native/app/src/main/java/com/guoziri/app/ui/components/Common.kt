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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LedgerPage(repo: StoreRepo, onAdd: () -> Unit, onEdit: (Purchase) -> Unit, onRequestDelete: (Purchase) -> Unit) {
    val purchases = repo.purchases
    val settings = repo.settings
    val t = today()

    val sorted = remember(purchases.size, repo.settings.sortOrder) {
        if (repo.settings.sortOrder == "desc")
            purchases.sortedByDescending { it.date + (it.createdAt ?: "") }
        else purchases.sortedBy { it.date + (it.createdAt ?: "") }
    }
    val groups = remember(sorted) { sorted.groupBy { it.date } }

    val todayList = repo.purchases.filter { it.date == today() }
    val todaySum = todayList.sumOf { it.totalCost ?: 0.0 }
    val month = today().substring(0, 7)
    val monthSum = repo.purchases.filter { it.date.startsWith(today().substring(0, 7)) }.sumOf { it.totalCost ?: 0.0 }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("记账", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 14.dp))
            Text(settings.sortOrder == "desc" ? "最新在前" : "最早在前", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("${todayList.size}", "今日笔数", Modifier.weight(1f))
                StatCard(money(todaySum), "今日花销", Modifier.weight(1f))
                StatCard(money(monthSum), "本月累计", Modifier.weight(1f))
                StatCard("${groups.size}", "记账天数", Modifier.weight(1f))
            }

            if (sorted.isEmpty()) {
                EmptyState("🛒", "还没有记录\n点右下角 ＋ 记第一笔")
            } else {
                val groups = sorted.groupBy { it.date }
                val dates = groups.keys.toList().sortedWith(if (settings.sortOrder == "desc") kotlin.Comparator.reverseOrder() else kotlin.Comparator.naturalOrder())

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                ) {
                    for (date in dates) {
                        val items = groups[date]!!
                        val sum = items.sumOf { it.totalCost ?: 0.0 }
                        val isToday = date == today()

                        item(key = "h$date") {
                            Text(
                                text = (if (isToday) "今天 · " else "") + fmtDay(date) + " ${weekDay(date)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp)
                            )
                            GroupCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${items.size} 笔", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(money(sum), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            items(items, key = { it.id }) { p ->
                                PurchaseRow(p, onEdit = { /* edit action */ }, onDelete = { /* delete action */ })
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseRow(p: Purchase, onEdit: () -> Unit, onDelete: () -> Unit) {
    val w = Nutrition.parseWeight(p)
    val wTxt = when {
        p.weightGrams != null -> if (p.weightGrams!! >= 1000) "${(p.weightGrams!! / 1000).toString().toDoubleOrNull()?.let { String.format("%.2fkg", it) } ?: "${p.weightGrams}g"}" else "${p.weightGrams}g"
        p.pricePerJin != null && p.totalCost != null -> "${(p.totalCost!! / p.pricePerJin!! * 500).toInt()}g"
        p.totalJin != null -> "${(p.totalJin!! * 500).toInt()}g"
        else -> ""
    }
    val tm = timeOf(p.createdAt)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier.padding(start = 8.dp).size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(CAT_COLOR[p.category] ?: Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center,
        ) { Text(CAT_ICON[p.category] ?: "🛒", fontSize = 17.sp) }

        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(p.name, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.pricePerJin?.let { Text(money(it) + "/斤", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                p.totalJin?.let { Text("${it}斤", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                wTxt.let { if (it.isNotBlank()) Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
            }
        }
        Text(p.totalCost?.let { money(it) } ?: "—", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
        TextButton(onClick = { /* edit action */ }) { Text("编辑", fontSize = 14.sp) }
        TextButton(onClick = { /* delete action */ }) { Text("删除", fontSize = 14.sp, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 48.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), marginTop = 4.dp)
        }
    }
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}