if (showCatPicker) {
        AlertDialog(
            onDismissRequest = { showCatPicker = false },
            confirmButton = { TextButton(onClick = { showCatPicker = false }) { Text("取消") } },
            dismissButton = { },
            title = { Text("选择分类") },
            text = {
                Column {
                    CATEGORIES.forEach { c ->
                        Text("${CAT_ICON[c]} $c", fontSize = 16.sp,
                            color = if (c == category) IosBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable {
                                category = c; showCatPicker = false
                            }.padding(vertical = 12.dp))
                    }
                }
            },
        )
    }
}