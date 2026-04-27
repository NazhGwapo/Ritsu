package com.example.ritsu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ritsu.ui.cards.GameListItem
import com.example.ritsu.ui.cards.TopGameItem

@Composable
fun TopGamesScreen(
    onGameClick: (TopGameItem) -> Unit,
    topGames: List<TopGameItem>
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(topGames) { game ->
                GameListItem(game = game, onClick = { onGameClick(game) })
            }
        }
    }
}
