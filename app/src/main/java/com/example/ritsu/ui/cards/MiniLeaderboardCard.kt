package com.example.ritsu.ui.cards

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritsu.data.GenericScore
import com.example.ritsu.ui.theme.RitsuTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MiniLeaderboardCard(
    rank: Int,
    score: GenericScore,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    displayValue: String? = null,
    onClick: () -> Unit = {}
) {
    val backgroundColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    val borderColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = displayValue ?: "${score.playRank} - ${"%,d".format(score.totalScore)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        val timeInfo = remember(score.playTimestamp) {
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                score.playTimestamp,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS
            ).toString()
            val timeFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            "${timeFormat.format(Date(score.playTimestamp))}, $relativeTime"
        }

        Text(
            text = timeInfo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontStyle = FontStyle.Italic,
            fontSize = 10.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MiniLeaderboardCardPreview() {
    RitsuTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MiniLeaderboardCard(
                rank = 1,
                score = GenericScore(
                    configId = 1,
                    songTitle = "Test Song",
                    difficultyName = "Expert",
                    difficultyVal = "10",
                    difficultySortValue = 10.0,
                    totalScore = 976142,
                    maxCombo = 500,
                    accuracy = 98.5,
                    playRank = "S",
                    playTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 30L
                ),
                isHighlighted = false
            )
            Spacer(modifier = Modifier.height(8.dp))
            MiniLeaderboardCard(
                rank = 2,
                score = GenericScore(
                    configId = 1,
                    songTitle = "Test Song",
                    difficultyName = "Expert",
                    difficultyVal = "10",
                    difficultySortValue = 10.0,
                    totalScore = 758123,
                    maxCombo = 450,
                    accuracy = 96.32,
                    playRank = "A",
                    playTimestamp = System.currentTimeMillis() - 1000 * 60 * 10L
                ),
                isHighlighted = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            MiniLeaderboardCard(
                rank = 3,
                score = GenericScore(
                    configId = 1,
                    songTitle = "Test Song",
                    difficultyName = "Expert",
                    difficultyVal = "10",
                    difficultySortValue = 10.0,
                    totalScore = 654321,
                    maxCombo = 300,
                    accuracy = 92.1,
                    playRank = "B",
                    playTimestamp = System.currentTimeMillis() - 1000 * 60 * 60L
                ),
                displayValue = "B - 92.10%"
            )
        }
    }
}
