package com.example.ritsu.ui.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritsu.ui.theme.RitsuTheme

data class ActivityDataPoint(
    val label: String,
    val value: Int
)

@Composable
fun ActivityGraphCard(
    dataPoints: List<ActivityDataPoint>,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val chartBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    ElevatedCard(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Activity Graph",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Animated normalized values
            val maxValue = (dataPoints.maxOfOrNull { it.value } ?: 10).coerceAtLeast(1)
            val animatedValues = dataPoints.map { point ->
                animateFloatAsState(
                    targetValue = point.value.toFloat() / maxValue,
                    animationSpec = tween(durationMillis = 600),
                    label = "BarHeightAnimation"
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(chartBgColor)
                    .padding(12.dp)
            ) {
                // Drawing Area
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()) {
                        
                        // Y-axis labels
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(1.0, 0.75, 0.5, 0.25, 0.0).forEach { ratio ->
                                Text(
                                    text = "${(maxValue * ratio).toInt()}",
                                    color = labelColor,
                                    fontSize = 10.sp,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        // Grid and Bars
                        Canvas(modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp)) { // Padding for labels
                            val width = size.width
                            val height = size.height
                            
                            val itemCount = dataPoints.size
                            val spacingRatio = 0.5f 
                            val totalSlots = itemCount + (itemCount + 1) * spacingRatio
                            val slotWidth = width / totalSlots
                            val barWidth = slotWidth
                            val spacing = slotWidth * spacingRatio
                            
                            // Horizontal grid lines
                            val gridLines = 4
                            for (i in 0..gridLines) {
                                val y = height - (i * height / gridLines)
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Bars
                            animatedValues.forEachIndexed { index, animValue ->
                                val x = spacing + index * (barWidth + spacing)
                                val barHeight = animValue.value * height
                                
                                if (barHeight > 0) {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.3f)),
                                            startY = height - barHeight,
                                            endY = height
                                        ),
                                        topLeft = Offset(x, height - barHeight),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    // X-axis labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 24.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val labelStep = when {
                            dataPoints.size > 20 -> 4
                            dataPoints.size > 10 -> 2
                            else -> 1
                        }
                        
                        dataPoints.forEachIndexed { index, point ->
                            if (index % labelStep == 0 || index == dataPoints.size - 1) {
                                Text(
                                    text = point.label,
                                    color = labelColor,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActivityGraphCardPreview() {
    RitsuTheme {
        ActivityGraphCard(
            dataPoints = listOf(
                ActivityDataPoint("1/1", 38),
                ActivityDataPoint("1/8", 45),
                ActivityDataPoint("1/17", 82),
                ActivityDataPoint("1/25", 22)
            )
        )
    }
}
