package com.example.ritsu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.ritsu.R
import com.example.ritsu.data.GenericScore
import com.example.ritsu.ui.cards.ScoreCard

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.ritsu.data.RitsuDatabase

@Composable
fun ScoreScreen() {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    var searchQuery by remember { mutableStateOf("") }

    val scores by database.scoreDao().getAllScores().collectAsState(initial = null)
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = null)
    val configMap = remember(configs) { configs?.associateBy { it.id } ?: emptyMap() }

    val filteredScores = remember(searchQuery, scores, configMap) {
        scores?.filter { fullRecord ->
            val score = fullRecord.genericScore
            val gameName = configMap[score.configId]?.gameName ?: ""
            score.songTitle.contains(searchQuery, ignoreCase = true) ||
                    gameName.contains(searchQuery, ignoreCase = true)
        }
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

        if (filteredScores == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredScores.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ohnoes),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = if (searchQuery.isEmpty()) "No scores yet" else "No matching scores")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredScores) { fullRecord ->
                    val score = fullRecord.genericScore
                    val config = configMap[score.configId]
                    val gameName = config?.gameName ?: "Unknown Game"
                    ScoreCard(
                        score = score,
                        gameName = gameName,
                        displayIconUri = config?.displayIconUri,
                        onClick = { /* TODO: Show details */ },
                        onMoreClick = { /* TODO: Options */ }
                    )
                }
            }
        }
    }
}
