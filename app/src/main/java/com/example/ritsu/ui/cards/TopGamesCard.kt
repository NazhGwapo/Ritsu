package com.example.ritsu.ui.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritsu.ui.utils.AsyncImage
import com.example.ritsu.R
import com.example.ritsu.ui.theme.RitsuTheme

data class TopGameItem(
    val rank: Int,
    val gameName: String,
    val playCount: Int,
    val configId: Long = 0,
    val achievements: List<Pair<String, Int>> = emptyList(),
    val displayIconUri: String? = null,
    val iconRes: Int? = null
)

@Composable
fun TopGamesCard(
    games: List<TopGameItem>,
    onCardClick: () -> Unit = {},
    onGameClick: (TopGameItem) -> Unit = {},
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
                text = "Top Games",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                games.take(4).forEach { game ->
                    GameListItem(game, onClick = { onGameClick(game) })
                }
            }

            val moreCount = (games.size - 4).coerceAtLeast(0)
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
fun GameListItem(game: TopGameItem, onClick: () -> Unit) {
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
            if (game.displayIconUri != null) {
                AsyncImage(
                    model = game.displayIconUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (game.iconRes != null) {
                Image(
                    painter = painterResource(id = game.iconRes),
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
            Text(
                text = "${game.rank}. ${game.gameName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            if (game.achievements.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    game.achievements.forEach { (label, count) ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$count $label",
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

            Text(
                text = "${game.playCount} plays",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopGamesCardPreview() {
    RitsuTheme {
        TopGamesCard(
            games = listOf(
                TopGameItem(
                    rank = 1, 
                    gameName = "D4DJ", 
                    playCount = 23,
                    achievements = listOf("Full Combo" to 12, "All Perfect" to 5)
                ),
                TopGameItem(
                    rank = 2, 
                    gameName = "Project Sekai", 
                    playCount = 20,
                    achievements = listOf("Full Combo" to 8)
                )
            )
        )
    }
}
