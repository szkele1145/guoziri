GroupTitle("总评")
        GroupCard {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val stroke = 20f
                        drawArc(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            style = Stroke(stroke))
                        drawArc(color = ringColor,
                            startAngle = -90f, sweepAngle = 360f * minOf(overall, 100) / 100f, useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }