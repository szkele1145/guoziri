package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
    Icon(
        AppIcons.ChevronRight,
        contentDescription = null,
        tint = LocalAppColors.current.tertiary,
        modifier = Modifier.size(18.dp).padding(start = 2.dp),
    )
}