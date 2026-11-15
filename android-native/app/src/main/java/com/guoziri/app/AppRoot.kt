package com.guoziri.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

/* ---------------- AppRoot：底部导航 + 各页 + snackbar + 启动页 ---------------- */
@Composable
fun AppRoot(repo: StoreRepo) {
    var currentTab by remember { mutableStateOf("ledger") }
    var showPurchaseSheet by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<Purchase?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Purchase?>(null) }
    var splashDone by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val chatState = remember { ChatState() }
    val c = LocalAppColors.current

    fun toast(msg: String) { scope.launch { snackbarHost.showSnackbar(msg) } }

    // fabIn：scale .6→1，弹性
    val fabScale by animateFloatAsState(
        targetValue = if (currentTab == "ledger") 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "fabIn",
    )

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                AppTabBar(currentTab, repo.settings.hiddenEnabled) { currentTab = it }
            },
            floatingActionButton = {
                if (currentTab == "ledger") {
                    // .fab：54x54，right 20，bottom tab+20
                    Box(
                        Modifier
                            .padding(end = Dimens.FabRight, bottom = Dimens.FabBottom + Dimens.TabH)
                            .size(Dimens.Fab)
                            .scale(fabScale)
                            .clip(CircleShape)
                            .background(c.tint)
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
            containerColor = c.bg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            // .page padding: 4 16 calc(tab-h + 24)
            Box(
                Modifier
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(start = Dimens.PageH, end = Dimens.PageH),
            ) {
                // pageIn：opacity 0→1，translateY 6px→0，.22s
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        (
                            fadeIn(animationSpec = tween(220)) +
                                slideInVertically(animationSpec = tween(220)) { it / 60 }
                            ).togetherWith(fadeOut(animationSpec = tween(120)))
                    },
                    label = "pageIn",
                ) { tab ->
                    when (tab) {
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
        }

        if (!splashDone) {
            SplashOverlay { splashDone = true }
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
            containerColor = c.bgElevated,
            shape = RoundedCornerShape(14.dp),
            title = { Text("删除这条记录？", fontSize = Dimens2.SheetTitle, fontWeight = FontWeight.SemiBold) },
            text = { Text(p.name, fontSize = Dimens2.AlertMsg, color = c.label2) },
            confirmButton = {
                TextButton(onClick = {
                    repo.deletePurchase(p.id)
                    showDeleteConfirm = null
                    toast("已删除")
                }) { Text("删除", fontSize = Dimens2.SheetTitle, fontWeight = FontWeight.SemiBold, color = c.red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消", fontSize = Dimens2.SheetTitle, color = c.tint)
                }
            },
        )
    }
}

/**
 * .tabbar：height 58 + safe，背景 rgba(bg,.94)，顶部 0.5px 线；
 * tab-item：图标 20、文字 9/500，选中 tint。
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
