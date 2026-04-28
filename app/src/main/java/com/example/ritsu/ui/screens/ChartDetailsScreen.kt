package com.example.ritsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.cards.MiniLeaderboardCard
import com.example.ritsu.ui.components.LeaderboardSortMode
import com.example.ritsu.ui.components.LineGraph
import com.example.ritsu.ui.components.MultiLineGraph
import com.example.ritsu.ui.components.GraphSeries
import com.example.ritsu.ui.components.DifficultyBadge
import com.example.ritsu.ui.components.ScoreDetailsDialog
import com.example.ritsu.ui.navigation.DetailFilterSheet
import com.example.ritsu.ui.utils.DateUtils
import kotlinx.serialization.json.Json
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailsScreen(
    configId: Long,
    songTitle: String,
    difficultyName: String,
    difficultyVal: String,
    onDifficultyClick: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    onGameClick: (Long) -> Unit = {},
    onGraphClick: (Long, String, String?, String?, String?, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scoreDao = database.scoreDao()
    
    val json = remember { Json { ignoreUnknownKeys = true } }

    val configs by scoreDao.getAllConfigs().collectAsState(initial = emptyList())
    val gameConfig = configs.find { it.id == configId }

    val chartScores by scoreDao.getScoresForChart(configId, songTitle, difficultyName, difficultyVal)
        .collectAsState(initial = emptyList())
    
    val allScoresForSong by scoreDao.getUniqueChartsForSong(configId, songTitle)
        .collectAsState(initial = emptyList())
    
    // --- DATE FILTERING STATE ---
    val dateRanges = listOf("Day", "Week", "Month", "Year", "All Time", "Custom")
    var selectedDateRange by remember { mutableStateOf("All Time") }
    val dateOptions = remember(selectedDateRange) { DateUtils.getDateOptions(selectedDateRange) }
    var selectedDateOption by remember(selectedDateRange) { mutableStateOf(dateOptions.firstOrNull() ?: "") }
    var customStartDate by remember { mutableLongStateOf(0L) }
    var customEndDate by remember { mutableLongStateOf(Long.MAX_VALUE) }
    
    var currentFilterSheet by remember { mutableStateOf(DetailFilterSheet.NONE) }
    var datePickerVisible by remember { mutableStateOf(false) }

    // --- DATA FILTERING ---
    val filteredScores = remember(chartScores, selectedDateRange, selectedDateOption, customStartDate, customEndDate) {
        chartScores.filter { record ->
            DateUtils.filterByDate(
                record.genericScore.playTimestamp,
                selectedDateRange,
                selectedDateOption,
                customStartDate,
                customEndDate
            )
        }
    }

    var sortMode by remember { mutableStateOf(LeaderboardSortMode.SCORE) }
    var selectedScoreRecord by remember { mutableStateOf<FullScoreRecord?>(null) }
    var scoreToEdit by remember { mutableStateOf<FullScoreRecord?>(null) }

    val sortedScores = remember(filteredScores, sortMode) {
        when (sortMode) {
            LeaderboardSortMode.SCORE -> filteredScores.sortedByDescending { it.genericScore.totalScore }
            LeaderboardSortMode.ACCURACY -> filteredScores.sortedByDescending { it.genericScore.accuracy }
            LeaderboardSortMode.MAX_COMBO -> filteredScores.sortedByDescending { it.genericScore.maxCombo }
        }
    }

    val scoresOldestToNewest = remember(filteredScores) {
        filteredScores.sortedBy { it.genericScore.playTimestamp }
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

            // --- TOP FILTERS BAR ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedDateRange != "All Time",
                        onClick = { currentFilterSheet = DetailFilterSheet.DATE_RANGE },
                        label = { Text(selectedDateRange, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp)) }
                    )
                    if (selectedDateRange != "All Time" && selectedDateRange != "Custom") {
                        FilterChip(
                            selected = true,
                            onClick = { currentFilterSheet = DetailFilterSheet.DATE_PERIOD },
                            label = { Text(selectedDateOption, fontSize = 12.sp) }
                        )
                    } else if (selectedDateRange == "Custom") {
                        FilterChip(
                            selected = customStartDate != 0L,
                            onClick = { datePickerVisible = true },
                            label = { Text(if (customStartDate == 0L) "Select Range" else "Custom Range", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Graphs Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(
                        text = "Performance Trends",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val sdf = remember { java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault()) }
                    val startDate = scoresOldestToNewest.firstOrNull()?.genericScore?.playTimestamp?.let { sdf.format(java.util.Date(it)) }
                    val endDate = scoresOldestToNewest.lastOrNull()?.genericScore?.playTimestamp?.let { sdf.format(java.util.Date(it)) }

                    // 1. Score Graph
                    LineGraph(
                        data = scoresOldestToNewest.map { it.genericScore.totalScore.toFloat() },
                        label = "Score over time",
                        startLabel = startDate,
                        endLabel = endDate,
                        valueFormatter = { "%,d".format(it.toLong()) },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onGraphClick(configId, "Score", songTitle, difficultyName, difficultyVal, false) }
                    )

                    // 2. Accuracy Graph
                    LineGraph(
                        data = scoresOldestToNewest.map { it.genericScore.accuracy.toFloat() },
                        label = "Accuracy over time",
                        startLabel = startDate,
                        endLabel = endDate,
                        valueFormatter = { "%.2f%%".format(it) },
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { onGraphClick(configId, "Accuracy", songTitle, difficultyName, difficultyVal, false) }
                    )

                    // 3. Combo Graph
                    LineGraph(
                        data = scoresOldestToNewest.map { it.genericScore.maxCombo.toFloat() },
                        label = "Max Combo over time",
                        startLabel = startDate,
                        endLabel = endDate,
                        valueFormatter = { "${it.toInt()}x" },
                        color = Color(0xFFFFA000), // Orange/Amber
                        modifier = Modifier.clickable { onGraphClick(configId, "Combo", songTitle, difficultyName, difficultyVal, false) }
                    )

                    // 4. Judgement Breakdown Graph
                    val judgementSeries = remember(scoresOldestToNewest, gameConfig) {
                        val configData = gameConfig?.let {
                            try {
                                json.decodeFromString<GameConfigData>(it.configData)
                            } catch (_: Exception) { null }
                        }
                        
                        val judgementKeys = configData?.judgments?.map { it.key } ?: emptyList()
                        val judgementLabels = configData?.judgments?.associate { it.key to it.label } ?: emptyMap()
                        
                        val colors = listOf(
                            Color(0xFF4CAF50), // Green
                            Color(0xFFFFEB3B), // Yellow
                            Color(0xFF03A9F4), // Light Blue
                            Color(0xFFF44336), // Red
                            Color(0xFF9C27B0), // Purple
                            Color(0xFFFF9800)  // Orange
                        )

                        judgementKeys.mapIndexed { index, key ->
                            GraphSeries(
                                label = judgementLabels[key] ?: key,
                                data = scoresOldestToNewest.map { record ->
                                    record.details.find { it.key == key }?.value?.toFloatOrNull() ?: 0f
                                },
                                color = colors.getOrElse(index) { Color.Gray }
                            )
                        }
                    }

                    if (judgementSeries.isNotEmpty()) {
                        MultiLineGraph(
                            series = judgementSeries,
                            label = "Judgement Breakdown",
                            startLabel = startDate,
                            endLabel = endDate,
                            modifier = Modifier.clickable { onGraphClick(configId, "Judgement", songTitle, difficultyName, difficultyVal, false) }
                        )
                    }

                    // 5. Metric Breakdown Graph
                    val metricSeries = remember(scoresOldestToNewest, gameConfig) {
                        val configData = gameConfig?.let {
                            try {
                                json.decodeFromString<GameConfigData>(it.configData)
                            } catch (_: Exception) { null }
                        }
                        
                        val metricFields = configData?.metrics?.filter { it.type != "boolean" } ?: emptyList()
                        
                        val colors = listOf(
                            Color(0xFF00BCD4), // Cyan
                            Color(0xFFFF5722), // Deep Orange
                            Color(0xFF8BC34A), // Light Green
                            Color(0xFFE91E63), // Pink
                            Color(0xFF673AB7), // Deep Purple
                            Color(0xFFCDDC39)  // Lime
                        )

                        metricFields.mapIndexed { index, field ->
                            GraphSeries(
                                label = field.label,
                                data = scoresOldestToNewest.map { record ->
                                    record.details.find { it.key == field.key }?.value?.toFloatOrNull() ?: 0f
                                },
                                color = colors.getOrElse(index) { Color.Gray }
                            )
                        }
                    }

                    if (metricSeries.isNotEmpty()) {
                        MultiLineGraph(
                            series = metricSeries,
                            label = "Metric Breakdown",
                            startLabel = startDate,
                            endLabel = endDate,
                            modifier = Modifier.clickable { onGraphClick(configId, "Metric", songTitle, difficultyName, difficultyVal, false) }
                        )
                    }
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
                            text = "No scores matching date filters.",
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
                                    if (configDataStr == null) emptyList() else {
                                        try {
                                            val cData = json.decodeFromString<GameConfigData>(configDataStr)
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

    // --- BOTTOM SHEETS & DIALOGS ---
    if (currentFilterSheet != DetailFilterSheet.NONE) {
        ModalBottomSheet(onDismissRequest = { currentFilterSheet = DetailFilterSheet.NONE }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text(
                    text = when(currentFilterSheet) {
                        DetailFilterSheet.DATE_RANGE -> "Select Date Range"
                        DetailFilterSheet.DATE_PERIOD -> "Select $selectedDateRange"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                when (currentFilterSheet) {
                    DetailFilterSheet.DATE_RANGE -> {
                        dateRanges.forEach { r ->
                            ListItem(
                                headlineContent = { Text(r) },
                                modifier = Modifier.clickable { 
                                    selectedDateRange = r
                                    if (r == "Custom") datePickerVisible = true else currentFilterSheet = DetailFilterSheet.NONE
                                }
                            )
                        }
                    }
                    DetailFilterSheet.DATE_PERIOD -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(dateOptions) { opt ->
                                ListItem(
                                    headlineContent = { Text(opt) },
                                    modifier = Modifier.clickable { 
                                        selectedDateOption = opt
                                        currentFilterSheet = DetailFilterSheet.NONE 
                                    }
                                )
                            }
                        }
                    }
                    else -> {}
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (datePickerVisible) {
        val state = rememberDateRangePickerState()
        Dialog(onDismissRequest = { datePickerVisible = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Select Date Range", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { datePickerVisible = false }) { Icon(Icons.Default.Close, null) }
                    }
                    DateRangePicker(state = state, modifier = Modifier.weight(1f))
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { 
                        customStartDate = state.selectedStartDateMillis ?: 0L
                        customEndDate = state.selectedEndDateMillis?.let { it + 86400000 - 1 } ?: Long.MAX_VALUE
                        selectedDateRange = "Custom"
                        datePickerVisible = false
                        currentFilterSheet = DetailFilterSheet.NONE 
                    }) { Text("Apply Range") }
                }
            }
        }
    }

    if (selectedScoreRecord != null && gameConfig != null) {
        ScoreDetailsDialog(
            scoreRecord = selectedScoreRecord!!,
            gameConfig = gameConfig,
            scoreDao = scoreDao,
            onDismiss = { selectedScoreRecord = null },
            onChartDetails = { selectedScoreRecord = null },
            onGameDetails = {
                selectedScoreRecord = null
                onGameClick(configId)
            },
            onEdit = {
                scoreToEdit = selectedScoreRecord
                selectedScoreRecord = null
            }
        )
    }

    if (scoreToEdit != null) {
        com.example.ritsu.ui.screens.debug.ManualEntryDialog(
            initialRecord = scoreToEdit,
            onDismiss = { scoreToEdit = null }
        )
    }
}
