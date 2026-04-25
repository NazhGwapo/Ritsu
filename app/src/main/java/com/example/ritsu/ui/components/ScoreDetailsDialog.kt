package com.example.ritsu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.ScoreDao
import com.example.ritsu.data.GenericScore
import com.example.ritsu.data.ScoreDetail
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.ui.cards.MiniLeaderboardCard
import com.example.ritsu.ui.theme.RitsuTheme
import kotlinx.coroutines.flow.flowOf
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.json.Json
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
fun ScoreDetailsDialog(
    scoreRecord: FullScoreRecord,
    gameConfig: GameConfig,
    scoreDao: ScoreDao,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
    onChartDetails: () -> Unit = {},
    onGameDetails: () -> Unit = {}
) {
    var currentScoreRecord by remember(scoreRecord) { mutableStateOf(scoreRecord) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ScoreDetailsDialogContent(
            scoreRecord = currentScoreRecord,
            gameConfig = gameConfig,
            scoreDao = scoreDao,
            onDismiss = onDismiss,
            onEdit = onEdit,
            onChartDetails = onChartDetails,
            onGameDetails = onGameDetails,
            onScoreSelected = { currentScoreRecord = it }
        )
    }
}

@Composable
fun ScoreDetailsDialogContent(
    scoreRecord: FullScoreRecord,
    gameConfig: GameConfig,
    scoreDao: ScoreDao,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
    onChartDetails: () -> Unit = {},
    onGameDetails: () -> Unit = {},
    onScoreSelected: (FullScoreRecord) -> Unit = {}
) {
    val score = scoreRecord.genericScore
    val details = scoreRecord.details

    val json = remember { Json { ignoreUnknownKeys = true } }
    val configData = remember(gameConfig) {
        try {
            json.decodeFromString<GameConfigData>(gameConfig.configData)
        } catch (_: Exception) {
            null
        }
    }

    val labelMap = remember(configData) {
        configData?.allFieldsWithCategory?.associate { it.first.key to it.first.label } ?: emptyMap()
    }

    val chartScores by scoreDao.getScoresForChart(score.songTitle, score.difficultyName)
        .collectAsState(initial = emptyList())
    val songTrackCount by scoreDao.getTrackCountForSong(score.songTitle)
        .collectAsState(initial = 0)
    val chartTrackCount by scoreDao.getTrackCountForChart(score.songTitle, score.difficultyName)
        .collectAsState(initial = 0)

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = score.songTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Score Summary
                item {
                    Column {
                        Text(
                            text = "${gameConfig.gameName} - [${score.difficultyName}] *${score.difficultyVal}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        val ppValue = details.find { it.key.contains("PP", ignoreCase = true) }?.value
                        val performanceText = buildString {
                            append("${score.playRank} ${"%.2f".format(score.accuracy)}%")
                            if (ppValue != null) append(" - ${ppValue}PP")
                            append(" - ${"%,d".format(score.totalScore)}")
                        }
                        Text(
                            text = performanceText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                // Categorized Details
                val categories = listOf("Judgment" to "Judgement Breakdown", "Metric" to "Metrics", "Misc" to "Misc")
                categories.forEach { (catKey, catLabel) ->
                    val catDetails = details.filter { it.category == catKey }
                    if (catDetails.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = catLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                catDetails.forEach { detail ->
                                    val displayLabel = labelMap[detail.key] ?: detail.key
                                    val displayValue = if (catKey == "Judgment" && detail.value.toIntOrNull() != null) {
                                        "${detail.value}x"
                                    } else {
                                        detail.value
                                    }
                                    Text(
                                        text = "$displayLabel - $displayValue",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tracking Info
                item {
                    Column {
                        val sdf = remember { SimpleDateFormat("MM/dd/yyyy, h:mm a", Locale.getDefault()) }
                        Text(
                            text = "Tracked on ${sdf.format(Date(score.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This song has been tracked $songTrackCount times, this specific chart tracked $chartTrackCount times.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                // Mini Leaderboard
                if (chartScores.isNotEmpty()) {
                    item {
                        Text(
                            text = "Top Scores",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    itemsIndexed(chartScores) { index, record ->
                        MiniLeaderboardCard(
                            rank = index + 1,
                            score = record.genericScore,
                            isHighlighted = record.genericScore.id == score.id,
                            modifier = Modifier.padding(vertical = 4.dp),
                            onClick = { onScoreSelected(record) }
                        )
                    }
                }
            }

            // Footer Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onChartDetails) {
                    Text("Chart Details", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onGameDetails) {
                    Text("Game Details", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScoreDetailsDialogPreview() {
    val dummyScore = GenericScore(
        id = 1,
        configId = 1,
        songTitle = "Song Name",
        difficultyName = "Difficulty",
        difficultyVal = "4.23",
        difficultySortValue = 4.23,
        totalScore = 758123,
        maxCombo = 1000,
        accuracy = 96.32,
        playRank = "A",
        timestamp = System.currentTimeMillis()
    )
    val dummyDetails = listOf(
        ScoreDetail(scoreId = 1, key = "300", value = "521", category = "Judgment"),
        ScoreDetail(scoreId = 1, key = "100", value = "21", category = "Judgment"),
        ScoreDetail(scoreId = 1, key = "50", value = "1", category = "Judgment"),
        ScoreDetail(scoreId = 1, key = "Miss", value = "2", category = "Judgment"),
        ScoreDetail(scoreId = 1, key = "PP", value = "126.32", category = "Metric")
    )
    val dummyRecord = FullScoreRecord(dummyScore, dummyDetails)

    val dummyConfig = GameConfig(
        id = 1,
        gameName = "Game",
        configData = "{\"judgments\":[{\"key\":\"300\",\"label\":\"Perfect\"},{\"key\":\"100\",\"label\":\"Great\"},{\"key\":\"50\",\"label\":\"Good\"},{\"key\":\"Miss\",\"label\":\"Miss\"}],\"metrics\":[{\"key\":\"PP\",\"label\":\"Performance Points\"}]}"
    )

    RitsuTheme {
        ScoreDetailsDialogContent(
            scoreRecord = dummyRecord,
            gameConfig = dummyConfig,
            scoreDao = object : ScoreDao {
                override suspend fun insertConfig(config: com.example.ritsu.data.GameConfig): Long = 0
                override suspend fun updateConfig(config: com.example.ritsu.data.GameConfig) {}
                override suspend fun getConfigByName(name: String): com.example.ritsu.data.GameConfig? = null
                override suspend fun upsertConfig(newConfig: com.example.ritsu.data.GameConfig) {}
                override suspend fun insertScore(score: GenericScore): Long = 0
                override suspend fun insertDetails(details: List<ScoreDetail>) {}
                override fun getAllScores(): kotlinx.coroutines.flow.Flow<List<FullScoreRecord>> = flowOf(emptyList())
                override fun getAllConfigs(): kotlinx.coroutines.flow.Flow<List<com.example.ritsu.data.GameConfig>> = flowOf(emptyList())
                override suspend fun deleteConfig(config: com.example.ritsu.data.GameConfig) {}
                override suspend fun deleteAllScores() {}
                override suspend fun deleteAllConfigs() {}
                override fun getScoresForChart(songTitle: String, difficultyName: String): kotlinx.coroutines.flow.Flow<List<FullScoreRecord>> = flowOf(listOf(dummyRecord))
                override fun getTrackCountForSong(songTitle: String): kotlinx.coroutines.flow.Flow<Int> = flowOf(8)
                override fun getTrackCountForChart(songTitle: String, difficultyName: String): kotlinx.coroutines.flow.Flow<Int> = flowOf(3)
            },
            onDismiss = {}
        )
    }
}
