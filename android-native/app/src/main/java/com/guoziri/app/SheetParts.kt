package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors

/**
 * 底部弹窗通用部件，规格对齐 web/style.css 的 .sheet-grabber / .sheet-head / .ios-group / .ios-cell
 */

/** 弹窗顶部把手：36x5，r3，margin 8 auto 0 */
@Composable
fun SheetGrabber() {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 36.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.fill),
        )
    }
}

/** 弹窗标题栏：padding 10 16 12，底部 0.5px 分隔线，标题 17/600 */
@Composable
fun SheetHead(
    title: String,
    leftText: String = "取消",
    rightText: String = "存储",
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.widthIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    leftText,
                    fontSize = Dimens2.SheetTitle,
                    color = c.secondary,
                    modifier = Modifier.clickable { onLeft() }.padding(4.dp),
                )
            }
            Text(
                title,
                fontSize = Dimens2.SheetTitle,
                fontWeight = FontWeight.SemiBold,
                color = c.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.widthIn(min = 56.dp), contentAlignment = Alignment.CenterEnd) {
                Text(
                    rightText,
                    fontSize = Dimens2.SheetTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = c.tint,
                    modifier = Modifier.clickable { onRight() }.padding(4.dp),
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = c.separator)
    }
}

/** .ios-group：bg-secondary + r12，间距 16 */
@Composable
fun SheetGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(Dimens.CardR),
        color = LocalAppColors.current.bgSecondary,
        modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        Column(content = content)
    }
}

/** .ios-cell：label + 右侧输入，min-height 52 */
@Composable
fun FormRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "选填",
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    numericFilter: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LocalAppColors.current
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.RowH)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = Dimens2.Body, color = c.label, maxLines = 1)
        if (onClick != null && trailing != null) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) { trailing() }
        } else {
            BasicTextField(
                value = value,
                onValueChange = { v ->
                    onChange(
                        if (numericFilter) v.filter { it.isDigit() || it == '.' } else v
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = Dimens2.Body,
                    color = c.label,
                    textAlign = TextAlign.End,
                    lineHeight = (Dimens2.Body.value * 1.2).sp,
                ),
                cursorBrush = SolidColor(c.tint),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                fontSize = Dimens2.Body,
                                color = c.tertiary,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                lineHeight = (Dimens2.Body.value * 1.2).sp,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

/** .chevron：右侧细箭头 */
@Composable
fun Chevron() {
    AppIcons.Draw(
                    AppIcons.ChevronRight,
                    size = 18.dp,
                    contentDescription = null,
        tint = LocalAppColors.current.tertiary,
        modifier = Modifier.size(18.dp).padding(start = 2.dp),
    )
}

/* ==================================================================
   action-sheet：底部选择面板（对齐 web 版 #action-sheet + .model-list）
   ================================================================== */

data class PickerOption(
    val value: String,
    val label: String,
    val current: Boolean = false,
)

/**
 * 底部选择面板：grabber + 标题/关闭 + 列表。
 * 与 HTML 的 #action-sheet 一致（列表项 15px、r8、当前项 tint/600、项间 0.5px 分隔线）。
 */
@Composable
fun SheetPicker(
    title: String,
    options: List<PickerOption>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                color = c.bgElevated,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
                    SheetGrabber()
                    // .sheet-head（无下边框，padding-bottom 4）
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            title,
                            fontSize = Dimens2.SheetTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = c.label,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "关闭",
                            fontSize = Dimens2.SheetTitle,
                            color = c.secondary,
                            modifier = Modifier.clickable { onDismiss() }.padding(4.dp),
                        )
                    }
                    // .model-list：padding 4 8 8，max-height 46dvh
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 8.dp)
                            .heightIn(max = 320.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    ) {
                        options.forEachIndexed { i, o ->
                            if (i > 0) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = c.separator,
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                )
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPick(o.value) }
                                    .padding(horizontal = 10.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    o.label,
                                    fontSize = 15.sp,
                                    color = if (o.current) c.tint else c.label,
                                    fontWeight = if (o.current) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                )
                                if (o.current) {
                                    Text("✓", fontSize = 15.sp, color = c.tint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ==================================================================
   .alert：系统确认框（居中 270，标题 17/600，底部两个等宽按钮）
   ================================================================== */

@Composable
fun AppAlert(
    title: String,
    message: String = "",
    confirmText: String = "确定",
    dismissText: String = "取消",
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(14.dp),
            color = c.bgElevated.copy(alpha = 0.97f),
            modifier = Modifier.width(270.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // .alert-title：pad 19 16 4
                Text(
                    title,
                    fontSize = Dimens2.SheetTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = c.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 19.dp, end = 16.dp, bottom = 4.dp),
                )
                if (message.isNotBlank()) {
                    // .alert-msg：13px，pad 0 16 18
                    Text(
                        message,
                        fontSize = Dimens2.AlertMsg,
                        color = c.label2,
                        textAlign = TextAlign.Center,
                        lineHeight = (Dimens2.AlertMsg.value * 1.45f).sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = c.separator)
                // .alert-actions：两个等宽按钮，中间 0.5 竖线
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onDismiss() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(dismissText, fontSize = Dimens2.SheetTitle, color = c.tint)
                    }
                    Box(Modifier.width(0.5.dp).fillMaxHeight().background(c.separator))
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onConfirm() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            confirmText,
                            fontSize = Dimens2.SheetTitle,
                            color = if (danger) c.red else c.tint,
                            fontWeight = if (danger) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}