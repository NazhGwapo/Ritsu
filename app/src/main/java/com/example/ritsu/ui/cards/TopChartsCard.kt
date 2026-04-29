package com.example.ritsu.ui.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritsu.ui.theme.RitsuTheme

@Composable
fun TopChartsCard(
    scores: List<TopScoreItem>,
    modifier: Modifier = Modifier,
    selectedSort: String = "Plays",
    onCardClick: () -> Unit = {},
    onChartClick: (TopScoreItem) -> Unit = {},
    onMoreClick: () -> Unit = {},
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Charts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                scores.take(4).forEach { score ->
                    ScoreListItem(score, selectedSort = selectedSort) { onChartClick(score) }
                }
            }

            val moreCount = (scores.size - 4).coerceAtLeast(0)
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

@Preview(showBackground = true)
@Composable
fun TopChartsCardPreview() {
    RitsuTheme {
        TopChartsCard(
            scores = listOf(
                TopScoreItem(1, 1, "Disappearance of Hatsune Miku", "Expert", "6.53", 98.54123, 1000, "S", "D4DJ", totalScore = 900000, playCount = 11),
                TopScoreItem(2, 2, "Neo-Aspect", "Expert", "24", 96.67, 1200, "SS", "BanG Dream!", totalScore = 800000, playCount = 4),
                TopScoreItem(3, 3, "HEAVEN'S RAVE", "Master", "14", 94.12, 800, "S", "D4DJ", totalScore = 700000, playCount = 5),
                TopScoreItem(4, 4, "Yes! BanG Dream!", "Expert", "23", 92.64, 950, "SS", "BanG Dream!", totalScore = 600000, playCount = 5)
            )
        )
    }
}
