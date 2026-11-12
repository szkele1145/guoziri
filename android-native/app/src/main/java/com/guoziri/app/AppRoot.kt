package com.guoziri.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/* ---------------- AppRoot：底部导航 + 各页 + 引导 + snackbar ---------------- */
@Composable
fun AppRoot(repo: StoreRepo) {
    var currentTab by remember { mutableStateOf("ledger") }
    var showPurchaseSheet by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<Purchase?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Purchase?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val chatState = remember { ChatState() }
    val showGuide = !repo.settings.guideDone

    fun toast(msg: String) { scope.launch { snackbarHost.showSnackbar(msg) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = { AppTabBar(currentTab, repo.settings.hiddenEnabled) { currentTab = it } },
        floatingActionButton = {
            if (currentTab == "ledger") {
                FloatingActionButton(
                    onClick = { editingPurchase = null; showPurchaseSheet = true },
                    containerColor = IosBlue, shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                ) { Text("＋", fontSize = 26.sp, color = Color.White) }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (currentTab) {
                "ledger" -> LedgerPage(repo, onAdd = { editingPurchase = null; showPurchaseSheet = true },
                    onEdit = { editingPurchase = it; showPurchaseSheet = true },
                    onRequestDelete = { showDeleteConfirm = it })
                "nutrition" -> NutritionPage(repo, date = today())
                "ai" -> AiPage(repo, chatState) { toast(it) }
                "settings" -> SettingsPage(repo, { toast(it) }, onReplayGuide = { /* v6: guideDone=false 重启引导 */
                    repo.updateSettings { it.copy(guideDone = false) }
                })
                "diary" -> DiaryPage(repo) { toast(it) }
            }
        }
    }

    if (showPurchaseSheet) {
        PurchaseSheet(repo, editingPurchase, onDismiss = { showPurchaseSheet = false }) { toast(it) }
    }

    showDeleteConfirm?.let { p ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除这条记录？") },
            text = { Text(p.name) },
            confirmButton = { TextButton(onClick = { repo.deletePurchase(p.id); showDeleteConfirm = null; toast("已删除") }) { Text("删除", color = IosRed) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") } },
        )
    }

    if (showGuide) {
        GuideOverlay(
            repo = repo,
            onDone = {
                repo.updateSettings { it.copy(guideDone = true) }
            },
        )
    }
}

@Composable
fun AppTabBar(current: String, hiddenEnabled: Boolean, onSelect: (String) -> Unit) {
    data class Tab(val id: String, val label: String)

    val tabs = buildList {
        add(Tab("ledger", "记账")); add(Tab("nutrition", "营养")); add(Tab("ai", "AI"))
        if (hiddenEnabled) add(Tab("diary", "日记"))
        add(Tab("settings", "设置"))
    }
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        for (t in tabs) {
            NavigationBarItem(
                selected = current == t.id,
                onClick = { onSelect(t.id) },
                icon = { Text(tabIcon(t.id), fontSize = 20.sp) },
                label = { Text(t.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IosBlue, selectedTextColor = IosBlue,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}

fun tabIcon(id: String) = when (id) {
    "ledger" -> "📋"
    "nutrition" -> "🥗"
    "ai" -> "✨"
    "diary" -> "📖"
    else -> "⚙️"
}