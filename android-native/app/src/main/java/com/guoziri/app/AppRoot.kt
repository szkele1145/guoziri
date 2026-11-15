package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors
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
                // .fab：54x54，right 20，bottom tab+20，蓝色阴影 0 4 16 rgba(0,122,255,.35)
                Box(
                    Modifier
                        .padding(end = Dimens.FabRight, bottom = Dimens.FabBottom + Dimens.TabH)
                        .size(Dimens.Fab)
                        .clip(CircleShape)
                        .background(SemanticColors.Primary)
                        .clickable { editingPurchase = null; showPurchaseSheet = true },
                    contentAlignment = Alignment.Center,
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
        containerColor = LocalAppColors.current.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        // .page padding: 4 16 calc(tab-h + 24)
        Box(
            Modifier
                .padding(padding)
                .statusBarsPadding()
                .padding(start = Dimens.PageH, end = Dimens.PageH),
        ) {
            when (currentTab) {
                "ledger" -> LedgerPage(repo,
                    onEdit = { editingPurchase = it; showPurchaseSheet = true },
                    onRequestDelete = { showDeleteConfirm = it },
                    onAdd = { editingPurchase = null; showPurchaseSheet = true })
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
            containerColor = LocalAppColors.current.bgElevated,
            shape = RoundedCornerShape(14.dp),
            title = { Text("删除这条记录？", fontSize = Dimens2.SheetTitle, fontWeight = FontWeight.SemiBold) },
            text = { Text(p.name, fontSize = Dimens2.AlertMsg, color = SemanticColors.Label2) },
            confirmButton = {
                TextButton(onClick = {
                    repo.deletePurchase(p.id)
                    showDeleteConfirm = null
                    toast("已删除")
                }) { Text("删除", fontSize = Dimens2.SheetTitle, color = SemanticColors.Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消", fontSize = Dimens2.SheetTitle, color = SemanticColors.Primary)
                }
            },
        )
    }
}

/**
 * .tabbar：height 58 + safe-b，背景 rgba(bg,0.94)，顶部 0.5px 分隔线，
 * tab-item：gap 2、padding-bottom 6、图标 20、文字 9/500，选中 tint。
 */
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
    val c = LocalAppColors.current

    Column(
        Modifier
            .fillMaxWidth()
            .background(c.bg.copy(alpha = 0.94f)),
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = c.separator)
        Row(
            Modifier
                .fillMaxWidth()
                .height(Dimens.TabH)
                .navigationBarsPadding(),
        ) {
            tabs.forEach { t ->
                val active = current == t.id
                val tint = if (active) c.tint else c.secondary
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(t.id) }
                        .padding(bottom = 6.dp),
                ) {
                    Icon(
                        tabIconVector(t.id),
                        contentDescription = t.label,
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        t.label,
                        fontSize = Dimens2.TabLabel,
                        fontWeight = FontWeight.Medium,
                        color = tint,
                    )
                }
            }
        }
    }
}
