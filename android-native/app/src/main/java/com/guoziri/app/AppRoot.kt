package com.guoziri.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/* ---------------- AppRoot：底部导航 + 各页 + snackbar ---------------- */
@Composable
fun AppRoot(repo: StoreRepo) {
    var currentTab by remember { mutableStateOf("ledger") }
    var showPurchaseSheet by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<Purchase?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Purchase?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val chatState = remember { ChatState() }

    fun toast(msg: String) { scope.launch { snackbarHost.showSnackbar(msg) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            AppTabBar(currentTab, repo.settings.hiddenEnabled) { currentTab = it }
        },
        floatingActionButton = {
            if (currentTab == "ledger") {
                FloatingActionButton(
                    onClick = { editingPurchase = null; showPurchaseSheet = true },
                    containerColor = IosBlue,
                    shape = CircleShape,
                    modifier = Modifier.size(54.dp),
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "记一笔",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (currentTab) {
                "ledger" -> LedgerPage(repo,
                    onEdit = { editingPurchase = it; showPurchaseSheet = true },
                    onRequestDelete = { showDeleteConfirm = it })
                "nutrition" -> NutritionPage(repo, date = today())
                "ai" -> AiPage(repo, chatState) { toast(it) }
                "settings" -> SettingsPage(repo) { toast(it) }
                "diary" -> DiaryPage(repo) { toast(it) }
            }
        }
    }

    if (showPurchaseSheet) {
        PurchaseSheet(
            repo = repo,
            editing = editingPurchase,
            onDismiss = { showPurchaseSheet = false },
        ) { toast(it) }
    }

    showDeleteConfirm?.let { p ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除这条记录？") },
            text = { Text(p.name) },
            confirmButton = {
                TextButton(onClick = {
                    repo.deletePurchase(p.id)
                    showDeleteConfirm = null
                    toast("已删除")
                }) { Text("删除", color = IosRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            },
        )
    }
}

@Composable
fun AppTabBar(
    current: String,
    hiddenEnabled: Boolean,
    onSelect: (String) -> Unit,
) {
    data class Tab(val id: String, val label: String)

    val tabs = buildList {
        add(Tab("ledger", "记账")); add(Tab("nutrition", "营养")); add(Tab("ai", "AI"))
        if (hiddenEnabled) add(Tab("diary", "日记"))
        add(Tab("settings", "设置"))
    }
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        for (t in tabs) {
            NavigationBarItem(
                selected = current == t.id,
                onClick = { onSelect(t.id) },
                icon = {
                    Icon(
                        tabIconVector(t.id),
                        contentDescription = t.label,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = { Text(t.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IosBlue,
                    selectedTextColor = IosBlue,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    indicatorColor = IosBlue.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
