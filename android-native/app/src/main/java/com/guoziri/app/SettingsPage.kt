if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            confirmButton = { TextButton(onClick = { showThemePicker = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showThemePicker = false }) { Text("取消") } },
            title = { Text("外观") },
            text = {
                Column {
                    listOf("auto" to "🌤 自动（跟随系统）", "light" to "☀️ 浅色", "dark" to "🌙 深色").forEach { (v, label) ->
                        Text(
                            label,
                            fontSize = 16.sp,
                            color = if (s.theme == v) IosBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repo.updateSettings { it.copy(theme = v) }
                                    ThemePrefs.theme.value = v
                                    showThemePicker = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
        )
    }