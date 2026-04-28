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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RankingUtils
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.cards.ScoreListItem
import com.example.ritsu.ui.components.LineGraph
import com.example.ritsu.ui.components.MultiLineGraph
import com.example.ritsu.ui.components.GraphSeries
import com.example.ritsu.ui.navigation.DetailFilterSheet
import com.example.ritsu.ui.utils.DateUtils
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.Json
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(
    configId: Long,
    onChartClick: (Long, String, String, String) -> Unit,
    onGraphClick: (Long, String, String?, String?, String?, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scoreDao = database.scoreDao()
    val json = remember { Json { ignoreUnknownKeys = true } }

    val configs by scoreDao.getAllConfigs().collectAsState(initial = emptyList())
    val gameConfig = configs.find { it.id == configId }
    val scores by scoreDao.getAllScores().collectAsState(initial = null)

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
    val gameScores = remember(scores, configId, selectedDateRange, selectedDateOption, customStartDate, customEndDate) {
        scores?.filter { record ->
            record.genericScore.configId == configId && DateUtils.filterByDate(
                record.genericScore.playTimestamp,
                selectedDateRange,
                selectedDateOption,
                customStartDate,
                customEndDate
            )
        }?.sortedBy { it.genericScore.playTimestamp } ?: emptyList()
    }

    val totalPlays = gameScores.size
    val uniqueCharts = gameScores.distinctBy { 
        "${it.genericScore.songTitle}|${it.genericScore.difficultyName}|${it.genericScore.difficultyVal}" 
    }.size

    var sortMode by remember { mutableStateOf("Score") }
    val sortOptions = listOf("Score", "Accuracy", "Max Combo", "Plays")

    var isComboNormalized by remember { mutableStateOf(true) }
    var isJudgementNormalized by remember { mutableStateOf(true) }

    var selectedScoreRecord by remember { mutableStateOf<FullScoreRecord?>(null) }
    var scoreToEdit by remember { mutableStateOf<FullScoreRecord?>(null) }

    val chartLeaderboard = remember(gameScores, sortMode, gameConfig) {
        gameScores.groupBy { "${it.genericScore.songTitle}|${it.genericScore.difficultyName}|${it.genericScore.difficultyVal}" }
            .map { (_, group) ->
                val bestRecord = if (sortMode == "Plays") {
                    group.maxByOrNull { it.genericScore.playTimestamp }!!
                } else {
                    group.maxByOrNull { record ->
                        RankingUtils.calculateRankingValue(record, gameConfig, sortMode)
                    }!!
                }
                
                RankingUtils.mapToTopScoreItem(
                    record = bestRecord,
                    config = gameConfig,
                    playCount = group.size,
                    showRank = (sortMode != "Plays")
                ).copy(showIcon = false)
            }
            .sortedByDescending { item ->
                if (sortMode == "Plays") {
                    item.playCount.toDouble()
                } else {
                    val originalRecord = gameScores.find { 
                        it.genericScore.songTitle == item.songTitle &&
                        it.genericScore.difficultyName == item.difficultyName &&
                        it.genericScore.difficultyVal == item.difficultyVal &&
                        it.genericScore.id == item.scoreId
                    }
                    if (originalRecord != null) {
                        RankingUtils.calculateRankingValue(originalRecord, gameConfig, sortMode)
                    } else 0.0
                }
            }
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
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
            // Header Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (gameConfig?.displayIconUri != null) {
                            AsyncImage(
                                model = gameConfig.displayIconUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column {
                        Text(
                            text = gameConfig?.gameName ?: "Unknown Game",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatItem(label = "Plays", value = totalPlays.toString())
                            StatItem(label = "Charts", value = uniqueCharts.toString())
                        }
                    }
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
                    val startDate = gameScores.firstOrNull()?.genericScore?.playTimestamp?.let { sdf.format(java.util.Date(it)) }
                    val endDate = gameScores.lastOrNull()?.genericScore?.playTimestamp?.let { sdf.format(java.util.Date(it)) }

                    // 1. Score Graph
                    LineGraph(
                        data = gameScores.map { it.genericScore.totalScore.toFloat() },
                        label = "Score over time",
                        startLabel = startDate,
                        endLabel = endDate,
                        valueFormatter = { "%,d".format(it.toLong()) },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onGraphClick(configId, "Score", null, null, null, false) }
                    )

                    // 2. Accuracy Graph
                    LineGraph(
                        data = gameScores.map { it.genericScore.accuracy.toFloat() },
                        label = "Accuracy over time",
                        startLabel = startDate,
                        endLabel = endDate,
                        valueFormatter = { "%.2f%%".format(it) },
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { onGraphClick(configId, "Accuracy", null, null, null, false) }
                    )

                    // 3. Combo Graph
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Max Combo Trend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = !isComboNormalized,
                                    onClick = { isComboNormalized = false },
                                    label = { Text("Raw", fontSize = 10.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                                FilterChip(
                                    selected = isComboNormalized,
                                    onClick = { isComboNormalized = true },
                                    label = { Text("Normalized", fontSize = 10.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                        LineGraph(
                            data = gameScores.map { record ->
                                if (isComboNormalized) {
                                    val totalNotes = record.details.filter { it.category == "Judgment" }
                                        .sumOf { it.value.toLongOrNull() ?: 0L }
                                    if (totalNotes > 0) (record.genericScore.maxCombo.toFloat() / totalNotes * 100f) else 0f
                                } else {
                                    record.genericScore.maxCombo.toFloat()
                                }
                            },
                            label = "",
                            startLabel = startDate,
                            endLabel = endDate,
                            valueFormatter = { if (isComboNormalized) "%.1f%%".format(it) else "${it.toInt()}x" },
                            color = Color(0xFFFFA000),
                            modifier = Modifier.clickable { onGraphClick(configId, "Combo", null, null, null, isComboNormalized) }
                        )
                    }

                    // 4. Judgement Breakdown Graph
                    val judgementSeries = remember(gameScores, gameConfig, isJudgementNormalized) {
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
                                data = gameScores.map { record ->
                                    val rawVal = record.details.find { it.key == key }?.value?.toFloatOrNull() ?: 0f
                                    if (isJudgementNormalized) {
                                        val totalNotes = record.details.filter { it.category == "Judgment" }
                                            .sumOf { it.value.toLongOrNull() ?: 0L }
                                        if (totalNotes > 0) (rawVal / totalNotes * 100f) else 0f
                                    } else {
                                        rawVal
                                    }
                                },
                                color = colors.getOrElse(index) { Color.Gray }
                            )
                        }
                    }

                    if (judgementSeries.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Judgement Breakdown",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = !isJudgementNormalized,
                                        onClick = { isJudgementNormalized = false },
                                        label = { Text("Raw", fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                    FilterChip(
                                        selected = isJudgementNormalized,
                                        onClick = { isJudgementNormalized = true },
                                        label = { Text("Normalized", fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                            MultiLineGraph(
                                series = judgementSeries,
                                label = "",
                                startLabel = startDate,
                                endLabel = endDate,
                                valueFormatter = { if (isJudgementNormalized) "%.1f".format(it) else it.toInt().toString() },
                                modifier = Modifier.clickable { onGraphClick(configId, "Judgement", null, null, null, isJudgementNormalized) }
                            )
                        }
                    }

                    // 5. Metric Breakdown Graph
                    val metricSeries = remember(gameScores, gameConfig) {
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
                                data = gameScores.map { record ->
                                    record.details.find { it.key == field.key }?.value?.toFloatOrNull() ?: 0f
                                },
                                color = colors.getOrElse(index) { Color.Gray }
                            )
                        }
                    }

                    if (metricSeries.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Metric Breakdown",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            MultiLineGraph(
                                series = metricSeries,
                                label = "",
                                startLabel = startDate,
                                endLabel = endDate,
                                valueFormatter = { it.toInt().toString() },
                                modifier = Modifier.clickable { onGraphClick(configId, "Metric", null, null, null, false) }
                            )
                        }
                    }
                }
            }

            // Chart Leaderboard Section
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
                        sortOptions.forEach { mode ->
                            FilterChip(
                                selected = sortMode == mode,
                                onClick = { sortMode = mode },
                                label = {
                                    Text(
                                        text = mode,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    if (chartLeaderboard.isEmpty()) {
                        Text(
                            text = "No charts matching date filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            chartLeaderboard.forEach { chart ->
                                ScoreListItem(
                                    score = chart,
                                    selectedSort = sortMode,
                                    onClick = { 
                                        if (sortMode == "Plays") {
                                            onChartClick(configId, chart.songTitle, chart.difficultyName, chart.difficultyVal)
                                        } else {
                                            selectedScoreRecord = gameScores.find { it.genericScore.id == chart.scoreId }
                                        }
                                    }
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
        com.example.ritsu.ui.components.ScoreDetailsDialog(
            scoreRecord = selectedScoreRecord!!,
            gameConfig = gameConfig,
            scoreDao = scoreDao,
            onDismiss = { selectedScoreRecord = null },
            onChartDetails = {
                val s = selectedScoreRecord!!.genericScore
                selectedScoreRecord = null
                onChartClick(s.configId, s.songTitle, s.difficultyName, s.difficultyVal)
            },
            onGameDetails = {
                selectedScoreRecord = null
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

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
