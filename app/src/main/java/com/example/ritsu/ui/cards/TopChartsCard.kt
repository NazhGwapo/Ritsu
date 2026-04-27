package com.example.ritsu.ui.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.ritsu.R
import com.example.ritsu.ui.theme.RitsuTheme

data class TopChartItem(
    val rank: Int,
    val songTitle: String,
    val playCount: Int,
    val difficultyName: String = "",
    val difficultyVal: String = "",
    val displayIconUri: String? = null,
    val iconRes: Int? = null
)

@Composable
fun TopChartsCard(
    charts: List<TopChartItem>,
    onCardClick: () -> Unit = {},
    onChartClick: (TopChartItem) -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onCardClick,
        modifier = modifier
            .padding(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = "Top Charts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                charts.take(4).forEach { chart ->
                    ChartListItem(chart, onClick = { onChartClick(chart) })
                }
            }

            val moreCount = (charts.size - 4).coerceAtLeast(0)
            val isEnabled = moreCount > 0

            Button(
                onClick = onMoreClick,
                enabled = isEnabled,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isEnabled) "$moreCount More" else "No More",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ChartListItem(chart: TopChartItem, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (chart.displayIconUri != null) {
                AsyncImage(
                    model = chart.displayIconUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (chart.iconRes != null) {
                Image(
                    painter = painterResource(id = chart.iconRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ohnoes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center),
                    alpha = 0.5f
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${chart.rank}. ${chart.songTitle}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                val highlightText = chart.difficultyName.takeIf { it.isNotBlank() && it != "Unknown" } ?: chart.difficultyVal
                
                if (highlightText.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = highlightText,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Text(
                text = "${chart.playCount} plays",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopChartsCardPreview() {
    RitsuTheme {
        TopChartsCard(
            charts = listOf(
                TopChartItem(1, "Disappearance of Hatsune Miku", 11, "Expert", "6.53"),
                TopChartItem(2, "HEAVEN'S RAVE [Master 14]", 5, "Master", "14"),
                TopChartItem(3, "pop enemy [Hard 10+]", 5, "", "10+"),
                TopChartItem(4, "Neo-Aspect [Expert 24]", 4, "Expert", "24")
            )
        )
    }
}
