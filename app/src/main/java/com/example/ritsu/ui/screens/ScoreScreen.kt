package com.example.ritsu.ui.screens

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import com.example.ritsu.R
import com.example.ritsu.ui.cards.ScoreCard
import com.example.ritsu.data.ScoreDetail
import com.example.ritsu.ui.screens.debug.ManualEntryDialog
import com.example.ritsu.ui.components.ScoreDetailsDialog
import kotlinx.coroutines.launch

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.data.GameConfigData
import kotlinx.serialization.json.Json

@Composable
fun ScoreScreen(
    scrollToTopSignal: Long = 0L,
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    var selectedScore by remember { mutableStateOf<FullScoreRecord?>(null) }
    var scoreToEdit by remember { mutableStateOf<FullScoreRecord?>(null) }

    val scores by database.scoreDao().getAllScores().collectAsState(initial = null)
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = null)
    val configMap = remember(configs) { configs?.associateBy { it.id } ?: emptyMap() }
    val json = remember { Json { ignoreUnknownKeys = true } }

    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            listState.animateScrollToItem(0)
        }
    }

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
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredScores) { fullRecord ->
                    val score = fullRecord.genericScore
                    val config = configMap[score.configId]
                    val gameName = config?.gameName ?: "Unknown Game"
                    
                    val useRank = remember(config) {
                        if (config == null) true else {
                            try {
                                json.decodeFromString<GameConfigData>(config.configData).useRankOcr
                            } catch (_: Exception) {
                                true
                            }
                        }
                    }

                    val booleanLabels = remember(fullRecord, config) {
                        if (config == null) emptyList<String>() else {
                            try {
                                val configData = json.decodeFromString<GameConfigData>(config.configData)
                                val booleanFields = configData.allFieldsWithCategory
                                    .filter { it.first.type == "boolean" }
                                    .map { it.first.key to it.first.label }
                                    .toMap()
                                
                                fullRecord.details
                                    .filter { booleanFields.containsKey(it.key) && it.value.lowercase() == "true" }
                                    .map { booleanFields[it.key] ?: "" }
                            } catch (_: Exception) {
                                emptyList<String>()
                            }
                        }
                    }

                    ScoreCard(
                        score = score,
                        gameName = gameName,
                        displayIconUri = config?.displayIconUri,
                        useRank = useRank,
                        booleanLabels = booleanLabels,
                        onClick = { selectedScore = fullRecord },
                        onEdit = { scoreToEdit = fullRecord },
                        onDelete = {
                            scope.launch {
                                database.scoreDao().deleteScoreById(score.id)
                            }
                        }
                    )
                }
            }
        }
    }

    selectedScore?.let { record ->
        val config = configMap[record.genericScore.configId]
        if (config != null) {
            ScoreDetailsDialog(
                scoreRecord = record,
                gameConfig = config,
                scoreDao = database.scoreDao(),
                onDismiss = { selectedScore = null },
                onEdit = {
                    selectedScore = null
                    scoreToEdit = record
                }
            )
        }
    }

    if (scoreToEdit != null) {
        ManualEntryDialog(
            initialRecord = scoreToEdit,
            onDismiss = { scoreToEdit = null }
        )
    }
}
