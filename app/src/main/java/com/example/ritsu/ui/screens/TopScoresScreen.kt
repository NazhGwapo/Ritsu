package com.example.ritsu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ritsu.ui.cards.ScoreListItem
import com.example.ritsu.ui.cards.TopScoreItem

@Composable
fun TopScoresScreen(
    sortMode: String,
    onScoreClick: (TopScoreItem) -> Unit,
    topScores: List<TopScoreItem>
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(topScores) { score ->
                ScoreListItem(score = score, selectedSort = sortMode, onClick = { onScoreClick(score) })
            }
        }
    }
}
