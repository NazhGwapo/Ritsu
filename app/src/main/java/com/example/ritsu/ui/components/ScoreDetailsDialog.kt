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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

enum class LeaderboardSortMode {
    SCORE, ACCURACY, MAX_COMBO
}

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

    val fieldMap = remember(configData) {
        configData?.allFieldsWithCategory?.associate { it.first.key to it.first } ?: emptyMap()
    }

    val chartScores by scoreDao.getScoresForChart(score.configId, score.songTitle, score.difficultyName, score.difficultyVal)
        .collectAsState(initial = emptyList())
    val songTrackCount by scoreDao.getTrackCountForSong(score.configId, score.songTitle)
        .collectAsState(initial = 0)
    val chartTrackCount by scoreDao.getTrackCountForChart(score.configId, score.songTitle, score.difficultyName, score.difficultyVal)
        .collectAsState(initial = 0)

    var sortMode by remember { mutableStateOf(LeaderboardSortMode.SCORE) }

    val sortedScores = remember(chartScores, sortMode) {
        when (sortMode) {
            LeaderboardSortMode.SCORE -> chartScores.sortedByDescending { it.genericScore.totalScore }
            LeaderboardSortMode.ACCURACY -> chartScores.sortedByDescending { it.genericScore.accuracy }
            LeaderboardSortMode.MAX_COMBO -> chartScores.sortedByDescending { it.genericScore.maxCombo }
        }
    }

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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = score.songTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = gameConfig.gameName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Performance Card
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (score.playRank.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = score.playRank,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val difficultyName = score.difficultyName.takeIf { it.isNotBlank() && it != "Unknown" }
                                    if (difficultyName != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = difficultyName,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = score.difficultyVal,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${"%.2f".format(score.accuracy)}% Accuracy",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${"%,d".format(score.totalScore)} - ${score.maxCombo}x Combo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Categorized Details (Metrics & Judgments)
                val categories = listOf("Judgment" to "Judgement Breakdown", "Metric" to "Metrics")
                categories.forEach { (catKey, catLabel) ->
                    val catFields = configData?.allFieldsWithCategory
                        ?.filter { it.second == catKey && it.first.type != "boolean" }
                        ?.map { it.first } ?: emptyList()
                    
                    if (catFields.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = catLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val chunks = catFields.chunked(3)
                                    chunks.forEach { rowFields ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowFields.forEach { field ->
                                                val detail = details.find { it.key == field.key }
                                                val value = detail?.value ?: "0"

                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 60.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = field.label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        val displayValue = if (catKey == "Judgment") {
                                                            val v = value.toIntOrNull() ?: 0
                                                            "${v}x"
                                                        } else {
                                                            value.ifEmpty { "0" }
                                                        }
                                                        Text(
                                                            text = displayValue,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                            // Add spacers for incomplete rows to maintain column alignment
                                            repeat(3 - rowFields.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Achievements (Booleans)
                val booleanDetails = details.filter { detail ->
                    val field = fieldMap[detail.key]
                    field?.type == "boolean" && detail.value.lowercase() == "true"
                }
                if (booleanDetails.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "Achievements",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val chunks = booleanDetails.chunked(2)
                                chunks.forEach { rowDetails ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowDetails.forEach { detail ->
                                            val field = fieldMap[detail.key]
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(
                                                    text = field?.label ?: detail.key,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tracking Info
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val sdf = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }
                            InfoRow("Played", sdf.format(Date(score.playTimestamp)))
                            InfoRow("Imported", sdf.format(Date(score.importTimestamp)))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tracked $songTrackCount times ($chartTrackCount for this chart)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }

                // Mini Leaderboard
                if (sortedScores.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "Top Scores",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
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
                            
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                sortedScores.forEachIndexed { index, record ->
                                    val s = record.genericScore
                                    val hasRank = configData?.useRankOcr != false && s.playRank.isNotBlank()
                                    val displayValue = when (sortMode) {
                                        LeaderboardSortMode.SCORE -> {
                                            if (hasRank) "${s.playRank} - ${"%,d".format(s.totalScore)}"
                                            else "%,d".format(s.totalScore)
                                        }
                                        LeaderboardSortMode.ACCURACY -> {
                                            if (hasRank) "${s.playRank} - ${"%.2f".format(s.accuracy)}%"
                                            else "${"%.2f".format(s.accuracy)}%"
                                        }
                                        LeaderboardSortMode.MAX_COMBO -> {
                                            if (hasRank) "${s.playRank} - ${s.maxCombo}x"
                                            else "${s.maxCombo}x"
                                        }
                                    }

                                    val shortenedBooleanLabels = remember(record, configData) {
                                        if (configData == null) emptyList<String>() else {
                                            val booleanFields = configData.allFieldsWithCategory
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
                                        }
                                    }

                                    MiniLeaderboardCard(
                                        rank = index + 1,
                                        score = s,
                                        isHighlighted = s.id == score.id,
                                        displayValue = displayValue,
                                        booleanLabels = shortenedBooleanLabels,
                                        onClick = { onScoreSelected(record) }
                                    )
                                }
                            }
                        }
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

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
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
        playTimestamp = System.currentTimeMillis(),
        importTimestamp = System.currentTimeMillis()
    )
    val dummyDetails = listOf(
        ScoreDetail(scoreId = 1, key = "300", value = "521", category = "Judgment"),
        ScoreDetail(scoreId = 1, key = "100", value = "21", category = "Judgment"),
        ScoreDetail(scoreId = 1, key = "50", value = "1", category = "Judgment"),
        ScoreDetail(scoreId = 1, key = "Miss", value = "2", category = "Judgment"),
        ScoreDetail(scoreId = 1, key = "PP", value = "126.32", category = "Metric"),
        ScoreDetail(scoreId = 1, key = "FC", value = "true", category = "Misc"),
        ScoreDetail(scoreId = 1, key = "AP", value = "false", category = "Misc")
    )
    val dummyRecord = FullScoreRecord(dummyScore, dummyDetails)

    val dummyConfig = GameConfig(
        id = 1,
        gameName = "Game",
        configData = "{\"gameName\":\"Game\",\"judgments\":[{\"key\":\"300\",\"label\":\"Perfect\"},{\"key\":\"100\",\"label\":\"Great\"},{\"key\":\"50\",\"label\":\"Good\"},{\"key\":\"Miss\",\"label\":\"Miss\"}],\"metrics\":[{\"key\":\"PP\",\"label\":\"Performance Points\"}],\"misc\":[{\"key\":\"FC\",\"label\":\"Full Combo\",\"type\":\"boolean\"},{\"key\":\"AP\",\"label\":\"All Perfect\",\"type\":\"boolean\"}]}"
    )

    val dummyRecord2 = FullScoreRecord(
        dummyScore.copy(id = 2, totalScore = 900000, accuracy = 99.0, playRank = "SSS", maxCombo = 1200),
        emptyList()
    )

    RitsuTheme {
        ScoreDetailsDialogContent(
            scoreRecord = dummyRecord,
            gameConfig = dummyConfig,
            scoreDao = object : ScoreDao {
                override suspend fun insertConfig(config: com.example.ritsu.data.GameConfig): Long = 0
                override suspend fun updateConfig(config: com.example.ritsu.data.GameConfig) {}
                override suspend fun updateScore(score: GenericScore) {}
                override suspend fun deleteDetailsForScore(scoreId: Long) {}
                override suspend fun deleteScoreById(id: Long) {}
                override suspend fun getConfigByName(name: String): com.example.ritsu.data.GameConfig? = null
                override suspend fun upsertConfig(newConfig: com.example.ritsu.data.GameConfig) {}
                override suspend fun insertScore(score: GenericScore): Long = 0
                override suspend fun insertDetails(details: List<ScoreDetail>) {}
                override fun getAllScores(): kotlinx.coroutines.flow.Flow<List<FullScoreRecord>> = flowOf(emptyList())
                override fun getAllConfigs(): kotlinx.coroutines.flow.Flow<List<com.example.ritsu.data.GameConfig>> = flowOf(emptyList())
                override suspend fun deleteConfig(config: com.example.ritsu.data.GameConfig) {}
                override suspend fun deleteAllScores() {}
                override suspend fun deleteAllConfigs() {}
                override fun getScoresForChart(configId: Long, songTitle: String, difficultyName: String, difficultyVal: String): kotlinx.coroutines.flow.Flow<List<FullScoreRecord>> = flowOf(listOf(dummyRecord, dummyRecord2))
                override fun getTrackCountForSong(configId: Long, songTitle: String): kotlinx.coroutines.flow.Flow<Int> = flowOf(8)
                override fun getTrackCountForChart(configId: Long, songTitle: String, difficultyName: String, difficultyVal: String): kotlinx.coroutines.flow.Flow<Int> = flowOf(3)
                override fun getUniqueChartsForSong(configId: Long, songTitle: String): kotlinx.coroutines.flow.Flow<List<GenericScore>> = flowOf(emptyList())
            },
            onDismiss = {}
        )
    }
}
