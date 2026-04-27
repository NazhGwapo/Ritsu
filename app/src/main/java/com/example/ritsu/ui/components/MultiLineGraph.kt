package com.example.ritsu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GraphSeries(
    val label: String,
    val data: List<Float>,
    val color: Color
)

@Composable
fun MultiLineGraph(
    series: List<GraphSeries>,
    modifier: Modifier = Modifier,
    label: String = "",
    startLabel: String? = null,
    endLabel: String? = null,
    valueFormatter: (Float) -> String = { it.toInt().toString() }
) {
    if (series.isEmpty() || series.all { it.data.isEmpty() }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available for $label",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        return
    }

    val allData = series.flatMap { it.data }
    val maxVal = allData.maxOrNull() ?: 100f
    val minVal = allData.minOrNull() ?: 0f
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(series) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    // Y-Axis
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = valueFormatter(maxVal),
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = valueFormatter(minVal),
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 32.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        series.forEach { s ->
                            if (s.data.size > 1) {
                                val path = Path()
                                val stepX = width / (s.data.size - 1)

                                s.data.forEachIndexed { index, value ->
                                    val x = index * stepX
                                    val normalizedValue = (value - minVal) / range
                                    val y = height - (normalizedValue * height * animationProgress.value)

                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }

                                drawPath(
                                    path = path,
                                    color = s.color,
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Points
                                s.data.forEachIndexed { index, value ->
                                    val x = index * stepX
                                    val normalizedValue = (value - minVal) / range
                                    val y = height - (normalizedValue * height * animationProgress.value)
                                    drawCircle(color = s.color, radius = 2.dp.toPx(), center = Offset(x, y))
                                }
                            } else if (s.data.size == 1) {
                                // Single data point
                                val value = s.data[0]
                                val x = width / 2
                                val normalizedValue = (value - minVal) / range
                                val y = height - (normalizedValue * height * animationProgress.value)
                                
                                drawCircle(
                                    color = s.color,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }

                // X-Axis Labels
                if (startLabel != null || endLabel != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = startLabel ?: "",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = endLabel ?: "",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            series.forEach { s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(s.color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = s.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
