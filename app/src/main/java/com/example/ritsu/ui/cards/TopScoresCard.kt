package com.example.ritsu.ui.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

data class TopScoreItem(
    val scoreId: Long,
    val rank: Int,
    val songTitle: String,
    val difficultyName: String,
    val difficultyVal: String,
    val accuracy: Double,
    val maxCombo: Int,
    val playRank: String,
    val gameName: String,
    val totalScore: Long = 0,
    val playCount: Int = 0,
    val booleanLabels: List<String> = emptyList(),
    val showRank: Boolean = true,
    val showIcon: Boolean = true,
    val displayIconUri: String? = null,
    val iconRes: Int? = null
)

@Composable
fun TopScoresCard(
    scores: List<TopScoreItem>,
    selectedSort: String = "Accuracy",
    onSortChange: (String) -> Unit = {},
    availableGames: List<String> = emptyList(),
    selectedGame: String = "All",
    onGameChange: (String) -> Unit = {},
    onCardClick: () -> Unit = {},
    onScoreClick: (TopScoreItem) -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var gameExpanded by remember { mutableStateOf(false) }
    val sortOptions = listOf("Accuracy", "Max Combo")

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Scores",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort Dropdown
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { expanded = true }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = selectedSort,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            sortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = {
                                        onSortChange(option)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Game Filter Icon Dropdown
                    Box {
                        IconButton(onClick = { gameExpanded = true }) {
                            Icon(
                                imageVector = if (selectedGame == "All") Icons.Default.FilterList else Icons.Default.VideogameAsset,
                                contentDescription = "Filter by Game",
                                tint = if (selectedGame == "All") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = gameExpanded,
                            onDismissRequest = { gameExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All", fontWeight = if (selectedGame == "All") FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onGameChange("All")
                                    gameExpanded = false
                                }
                            )
                            availableGames.forEach { game ->
                                DropdownMenuItem(
                                    text = { Text(game, fontWeight = if (selectedGame == game) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        onGameChange(game)
                                        gameExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                scores.take(4).forEach { score ->
                    ScoreListItem(score, selectedSort = selectedSort, onClick = { onScoreClick(score) })
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

@Composable
fun ScoreListItem(score: TopScoreItem, selectedSort: String = "Accuracy", onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        if (score.showIcon) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (score.displayIconUri != null) {
                    AsyncImage(
                        model = score.displayIconUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (score.iconRes != null) {
                    Image(
                        painter = painterResource(id = score.iconRes),
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
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (score.showRank && score.playRank.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = score.playRank,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = score.songTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val difficultyName = score.difficultyName.takeIf { it.isNotBlank() && it != "Unknown" }
                val highlightText = difficultyName ?: score.difficultyVal

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

            if (score.booleanLabels.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    score.booleanLabels.forEach { label ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            val statsText = when (selectedSort) {
                "Accuracy" -> "${"%.2f".format(score.accuracy)}% Accuracy"
                "Max Combo" -> "${score.maxCombo}x Max Combo"
                "Score" -> "${"%,d".format(score.totalScore)} Score"
                "Plays" -> "${score.playCount} plays"
                else -> "${"%.2f".format(score.accuracy)}% Accuracy"
            }
            Text(
                text = "${score.difficultyVal} - $statsText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = score.gameName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopScoresCardPreview() {
    RitsuTheme {
        TopScoresCard(
            scores = listOf(
                TopScoreItem(1, 1, "Disappearance of Hatsune Miku", "Expert", "6.53", 98.54123, 1000, "S", "D4DJ"),
                TopScoreItem(2, 2, "Neo-Aspect", "Expert", "24", 96.67, 1200, "SS", "BanG Dream!"),
                TopScoreItem(3, 3, "HEAVEN'S RAVE", "Master", "14", 94.12, 800, "S", "D4DJ"),
                TopScoreItem(4, 4, "Yes! BanG Dream!", "Expert", "23", 92.64, 950, "SS", "BanG Dream!")
            )
        )
    }
}
