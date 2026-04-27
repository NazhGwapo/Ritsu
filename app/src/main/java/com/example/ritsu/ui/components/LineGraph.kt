package com.example.ritsu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LineGraph(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    label: String = "",
    startLabel: String? = null,
    endLabel: String? = null,
    valueFormatter: (Float) -> String = { it.toString() }
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
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

    val maxVal = data.maxOf { it }
    val minVal = data.minOf { it }
    val range = (maxVal - minVal).coerceAtLeast(0.01f)
    
    // Animation progress
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
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
                .height(150.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                // Y-Axis Info (Top/Bottom values)
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = valueFormatter(maxVal),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = valueFormatter(minVal),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                val trendColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp) // Space for Y-axis labels
                ) {
                    val width = size.width
                    val height = size.height
                    
                    if (data.size > 1) {
                        val path = Path()
                        val fillPath = Path()
                        
                        val stepX = width / (data.size - 1)
                        
                        data.forEachIndexed { index, value ->
                            val x = index * stepX
                            val normalizedValue = (value - minVal) / range
                            val y = height - (normalizedValue * height * animationProgress.value)
                            
                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            
                            if (index == data.size - 1) {
                                fillPath.lineTo(x, height)
                                fillPath.close()
                            }
                        }
                        
                        // Draw Area Fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.2f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )
                        
                        // Draw Line
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Draw Trend Line (Trajectory)
                        if (data.size > 1) {
                            val n = data.size.toFloat()
                            var sumX = 0f
                            var sumY = 0f
                            var sumXY = 0f
                            var sumX2 = 0f
                            
                            data.forEachIndexed { i, y ->
                                val x = i.toFloat()
                                sumX += x
                                sumY += y
                                sumXY += x * y
                                sumX2 += x * x
                            }
                            
                            val denom = n * sumX2 - sumX * sumX
                            if (denom != 0f) {
                                val m = (n * sumXY - sumX * sumY) / denom
                                val b = (sumY - m * sumX) / n
                                
                                val startX = 0f
                                val startY = height - ((b - minVal) / range * height * animationProgress.value)
                                
                                val endX = (n - 1) * stepX
                                val endYVal = m * (n - 1) + b
                                val endY = height - ((endYVal - minVal) / range * height * animationProgress.value)
                                
                                drawLine(
                                    color = trendColor,
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }
                        }

                        // Draw points
                        data.forEachIndexed { index, value ->
                            val x = index * stepX
                            val normalizedValue = (value - minVal) / range
                            val y = height - (normalizedValue * height * animationProgress.value)
                            
                            drawCircle(
                                color = color,
                                radius = 3.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    } else {
                        // Single data point
                        val normalizedValue = 0.5f // Middle
                        val y = height - (normalizedValue * height * animationProgress.value)
                        drawCircle(
                            color = color,
                            radius = 4.dp.toPx(),
                            center = Offset(width / 2, y)
                        )
                        drawLine(
                            color = color.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
        }

        if (startLabel != null || endLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 44.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = startLabel ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = endLabel ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
