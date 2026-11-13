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
    val anchors = remember { GuideAnchors() }

    // 引导：首次进入自动跑；设置页「重播新手引导」也能再跑一次
    var showGuide by remember { mutableStateOf(!repo.settings.guideDone) }
    var guideStep by remember { mutableIntStateOf(0) }

    fun toast(msg: String) { scope.launch { snackbarHost.showSnackbar(msg) } }

    fun startGuide() {
        repo.openSandbox()
        repo.loadSandboxDemo()
        guideStep = 0
        showGuide = true
        currentTab = "ledger"
    }

    fun endGuide() {
        repo.closeSandbox()
        showGuide = false
        guideStep = 0
        repo.updateSettings { it.copy(guideDone = true) }
        toast("引导完成，开始使用吧！")
    }

    /** 引导推进：处理切页 / 开弹窗 / 收尾 */
    fun guideNext() {
        if (guideStep >= GuideSteps.size - 1) { endGuide(); return }
        val next = guideStep + 1
        val ns = GuideSteps.STEPS[next]
        guideStep = next
        if (ns.inSheet) {
            // 在弹窗里展示，先把弹窗关掉再由下一步打开
            showPurchaseSheet = false
            editingPurchase = null
        } else {
            showPurchaseSheet = false
            ns.tab?.let { currentTab = it }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            AppTabBar(currentTab, repo.settings.hiddenEnabled, anchors) { currentTab = it }
        },
        floatingActionButton = {
            if (currentTab == "ledger") {
                FloatingActionButton(
                    onClick = { editingPurchase = null; showPurchaseSheet = true },
                    containerColor = IosBlue, shape = CircleShape,
                    modifier = Modifier.size(56.dp).guideAnchor(anchors, "fab", currentTab),
                ) { Text("＋", fontSize = 26.sp, color = Color.White) }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (currentTab) {
                "ledger" -> LedgerPage(repo,
                    anchors = anchors,
                    onAdd = { editingPurchase = null; showPurchaseSheet = true },
                    onEdit = { editingPurchase = it; showPurchaseSheet = true },
                    onRequestDelete = { showDeleteConfirm = it })
                "nutrition" -> NutritionPage(repo, date = today(), anchors = anchors)
                "ai" -> AiPage(repo, chatState, anchors) { toast(it) }
                "settings" -> SettingsPage(repo, { toast(it) },
                    anchors = anchors,
                    onReplayGuide = { startGuide() })
                "diary" -> DiaryPage(repo) { toast(it) }
            }
        }
    }

    // 引导走到「添加记录」这一步时自动打开表单
    val guideInSheet = showGuide && GuideSteps.STEPS.getOrNull(guideStep)?.inSheet == true
    LaunchedEffect(guideStep) {
        if (showGuide && GuideSteps.STEPS.getOrNull(guideStep)?.inSheet == true) {
            editingPurchase = null
            showPurchaseSheet = true
        }
    }

    if (showPurchaseSheet) {
        PurchaseSheet(
            repo = repo,
            editing = editingPurchase,
            anchors = anchors,
            guideStep = if (guideInSheet) guideStep else null,
            onNext = { guideNext() },
            onSkip = { endGuide() },
            onDismiss = { if (guideInSheet) guideNext() else showPurchaseSheet = false },
        ) { toast(it) }
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

    // 弹窗内的那一步由 PurchaseSheet 自己画引导卡，这里不叠全局遮罩
    val guideOverlayVisible = showGuide && GuideSteps.STEPS.getOrNull(guideStep)?.inSheet != true
    if (guideOverlayVisible) {
        GuideOverlay(
            step = guideStep,
            anchors = anchors,
            scope = currentTab,
            onNext = { guideNext() },
            onSkip = { endGuide() },
        )
    }
}

@Composable
fun AppTabBar(
    current: String,
    hiddenEnabled: Boolean,
    anchors: GuideAnchors,
    onSelect: (String) -> Unit,
) {
    data class Tab(val id: String, val label: String)

    val tabs = buildList {
        add(Tab("ledger", "记账")); add(Tab("nutrition", "营养")); add(Tab("ai", "AI"))
        if (hiddenEnabled) add(Tab("diary", "日记"))
        add(Tab("settings", "设置"))
    }
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = Modifier.guideAnchor(anchors, "tabbar", "tabbar"),
    ) {
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