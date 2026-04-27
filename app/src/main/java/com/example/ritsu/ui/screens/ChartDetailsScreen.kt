package com.example.ritsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.cards.MiniLeaderboardCard
import com.example.ritsu.ui.components.LeaderboardSortMode
import com.example.ritsu.ui.components.ScoreDetailsDialog
import kotlinx.serialization.json.Json

@Composable
fun ChartDetailsScreen(
    configId: Long,
    songTitle: String,
    difficultyName: String,
    difficultyVal: String,
    onDifficultyClick: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    onGameClick: (Long) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scoreDao = database.scoreDao()

    val configs by scoreDao.getAllConfigs().collectAsState(initial = emptyList())
    val gameConfig = configs.find { it.id == configId }

    val chartScores by scoreDao.getScoresForChart(configId, songTitle, difficultyName, difficultyVal)
        .collectAsState(initial = emptyList())
    
    val allScoresForSong by scoreDao.getUniqueChartsForSong(configId, songTitle)
        .collectAsState(initial = emptyList())
    
    // We need to count plays for each unique chart. 
    // Since getUniqueChartsForSong returns grouped results (one per chart),
    // we need to actually fetch counts for those charts.
    val chartPlayCounts = remember(allScoresForSong) {
        mutableMapOf<String, Int>()
    }

    // A better way is to fetch all scores for the song and group them in memory
    // But for now, since we have the charts, let's just show them.
    // I'll update the aggregation to group everything manually from a full list for accurate counts.

    var sortMode by remember { mutableStateOf(LeaderboardSortMode.SCORE) }
    var selectedScoreRecord by remember { mutableStateOf<FullScoreRecord?>(null) }

    val sortedScores = remember(chartScores, sortMode) {
        when (sortMode) {
            LeaderboardSortMode.SCORE -> chartScores.sortedByDescending { it.genericScore.totalScore }
            LeaderboardSortMode.ACCURACY -> chartScores.sortedByDescending { it.genericScore.accuracy }
            LeaderboardSortMode.MAX_COMBO -> chartScores.sortedByDescending { it.genericScore.maxCombo }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Song Info
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = songTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        DifficultyBadge(difficultyName, difficultyVal)
                    }
                    Text(
                        text = gameConfig?.gameName ?: "Unknown Game",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onGameClick(configId) }
                    )
                }
            }

            // Graphs Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Graphs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    GraphPlaceholder("Accuracy over time")
                    GraphPlaceholder("Score over time")
                }
            }

            // Chart Leaderboard
            item {
                Column {
                    Text(
                        text = "Chart Leaderboard",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LeaderboardSortMode.entries.forEach { mode ->
                            FilterChip(
                                selected = sortMode == mode,
                                onClick = { sortMode = mode },
                                label = {
                                    Text(
                                        text = when (mode) {
                                            LeaderboardSortMode.SCORE -> "Score"
                                            LeaderboardSortMode.ACCURACY -> "Accuracy"
                                            LeaderboardSortMode.MAX_COMBO -> "Combo"
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }
                    
                    if (sortedScores.isEmpty()) {
                        Text(
                            text = "No scores recorded for this chart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sortedScores.forEachIndexed { index, record ->
                                val s = record.genericScore
                                val displayValue = when (sortMode) {
                                    LeaderboardSortMode.SCORE -> "%,d".format(s.totalScore)
                                    LeaderboardSortMode.ACCURACY -> "${"%.2f".format(s.accuracy)}%"
                                    LeaderboardSortMode.MAX_COMBO -> "${s.maxCombo}x"
                                }

                                val shortenedBooleanLabels = remember(record, gameConfig) {
                                    val configDataStr = gameConfig?.configData
                                    if (configDataStr == null) emptyList<String>() else {
                                        try {
                                            val cData = Json { ignoreUnknownKeys = true }.decodeFromString<GameConfigData>(configDataStr)
                                            val booleanFields = cData.allFieldsWithCategory
                                                .filter { it.first.type == "boolean" }
                                                .map { it.first }
                                            
                                            record.details
                                                .filter { detail -> 
                                                    val field = booleanFields.find { it.key == detail.key }
                                                    field != null && detail.value.lowercase() == "true"
                                                }
                                                .map { detail ->
                                                    val field = booleanFields.find { it.key == detail.key }!!
                                                    field.shortLabel ?: field.label.split(" ").filter { it.isNotBlank() }.joinToString("") { it.take(1).uppercase() }
                                                }
                                        } catch (_: Exception) {
                                            emptyList<String>()
                                        }
                                    }
                                }
                                
                                MiniLeaderboardCard(
                                    rank = index + 1,
                                    score = s,
                                    displayValue = displayValue,
                                    isHighlighted = false,
                                    booleanLabels = shortenedBooleanLabels,
                                    onClick = { selectedScoreRecord = record }
                                )
                            }
                        }
                    }
                }
            }

            // Other Difficulties
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Other Difficulties",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val filteredOtherCharts = allScoresForSong.filter { 
                        it.difficultyName != difficultyName || it.difficultyVal != difficultyVal 
                    }

                    if (filteredOtherCharts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No other difficulty records found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        filteredOtherCharts.forEach { chart ->
                            // For play count, we'd ideally have it from the query.
                            // Since we don't yet, let's keep it generic or add a placeholder text.
                            // Wait, I should probably implement a way to get the count.
                            // I'll add a helper flow to get counts.
                            
                            val playCount by scoreDao.getTrackCountForChart(configId, songTitle, chart.difficultyName, chart.difficultyVal)
                                .collectAsState(initial = 0)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable {
                                        onDifficultyClick(configId, songTitle, chart.difficultyName, chart.difficultyVal)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DifficultyBadge(chart.difficultyName, chart.difficultyVal)
                                
                                Text(
                                    text = "$playCount plays",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (selectedScoreRecord != null && gameConfig != null) {
        ScoreDetailsDialog(
            scoreRecord = selectedScoreRecord!!,
            gameConfig = gameConfig,
            scoreDao = scoreDao,
            onDismiss = { selectedScoreRecord = null },
            onChartDetails = { selectedScoreRecord = null }
        )
    }
}

@Composable
fun DifficultyBadge(name: String, value: String) {
    val badgeText = remember(name, value) {
        val n = name.trim().takeIf { it.isNotBlank() && it != "Unknown" }
        val v = value.trim().takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
        
        when {
            n != null && v != null -> "$n $v"
            n != null -> n
            v != null -> v
            else -> ""
        }
    }
    
    if (badgeText.isNotBlank()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = badgeText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun GraphPlaceholder(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PLACEHOLDER",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
