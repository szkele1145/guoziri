package com.guoziri.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guoziri.app.ui.theme.LocalAppColors

/**
 * 自绘底部弹窗（替代 Material 的 ModalBottomSheet）
 *
 * 为什么要自己画：
 *  ModalBottomSheet 是独立 Dialog 窗口，键盘弹出时系统会重新布局那个窗口，
 *  导致弹窗「跳一下」。自绘版渲染在主窗口的同一棵 Compose 树里，
 *  配合 Activity 的 adjustResize + imePadding，键盘弹出时整屏等比上移，弹窗不会跳动。
 */
@Composable
fun AppBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    maxHeightFraction: Float = 0.92f,
    /** 是否允许点击外部关闭 */
    dismissOnOutsideClick: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalAppColors.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(Modifier.fillMaxSize()) {
        // 遮罩
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (dismissOnOutsideClick) onDismiss() }
            )
        }

        // 面板
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 吃掉点击，避免穿透到遮罩 */ },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(maxHeightFraction)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(c.bgElevated)
                        // 键盘弹出时整块上移，输入框始终可见
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    content()
                }
            }
        }
    }
}
