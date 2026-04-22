package com.example.ritsu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.ritsu.data.GenericScore
import com.example.ritsu.ui.cards.ScoreCard

@Composable
fun ScoreScreen() {
    var searchQuery by remember { mutableStateOf("") }

    // Dummy data for display
    val dummyScores = remember {
        listOf(
            GenericScore(
                songTitle = "Example Song 1",
                difficultyName = "Expert",
                difficultyVal = 26.0,
                accuracy = 99.5,
                playRank = "S",
                configId = 1,
                totalScore = 1000000,
                maxCombo = 500,
                timestamp = System.currentTimeMillis()
            ),
            GenericScore(
                songTitle = "Example Song 2",
                difficultyName = "Master",
                difficultyVal = 31.0,
                accuracy = 97.2,
                playRank = "A",
                configId = 1,
                totalScore = 950000,
                maxCombo = 800,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search scores...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(dummyScores.filter { it.songTitle.contains(searchQuery, ignoreCase = true) }) { score ->
                ScoreCard(
                    score = score,
                    gameName = "Example Game",
                    onMoreClick = { /* Handle menu click */ }
                )
            }
        }
    }
}
