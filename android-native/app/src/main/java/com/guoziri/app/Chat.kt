package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSheet(state: ChatState, repo: StoreRepo, snackbar: (String) -> Unit) {
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val sending = state.messages.lastOrNull()?.text?.endsWith("▍") == true
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { if (!sending) state.close() },
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxWidth().height(560.dp)) {
            // 标题栏
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.PageH, top = 4.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = SemanticColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI 助手",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { state.messages.clear() }) {
                    Text("新对话", fontSize = 13.sp, color = SemanticColors.Primary)
                }
            }
            CardDivider(modifier = Modifier.padding(start = 0.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(state.messages) { m ->
                    val isUser = m.role == "user"
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    ) {
                        Box(
                            Modifier
                                .widthIn(max = 290.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp,
                                    ),
                                )
                                .background(if (isUser) SemanticColors.Primary else MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 13.dp, vertical = 9.dp),
                        ) {
                            Text(
                                m.text,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // 输入区
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("想问点什么…", fontSize = 14.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                    enabled = !sending,
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (input.isNotBlank() && !sending) SemanticColors.Primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    onClick = {
                        val q = input.trim()
                        if (q.isNotEmpty() && !sending) {
                            input = ""
                            val scope = rememberCoroutineScope()
                            scope.launch { state.send(q, repo) }
                        }
                    },
                    enabled = input.isNotBlank() && !sending,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = "发送",
                            tint = if (input.isNotBlank() && !sending) Color.White
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
    // 自动滚动到底
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaEffectImpl(listState: androidx.compose.foundation.lazy.LazyListState, size: Int) {
    androidx.compose.runtime.LaunchedEffect(size) {
        if (size > 0) listState.animateScrollToItem(size - 1)
    }
}