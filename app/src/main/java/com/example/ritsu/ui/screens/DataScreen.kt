package com.example.ritsu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritsu.R
import com.example.ritsu.data.AccuracyCalculator
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.cards.*
import com.example.ritsu.ui.components.ScoreDetailsDialog
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    navController: androidx.navigation.NavController
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scoresFlow = remember { database.scoreDao().getAllScores() }
    val scores by scoresFlow.collectAsState(initial = null)
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    val json = remember { Json { ignoreUnknownKeys = true } }

    val ranges = listOf("Day", "Week", "Month", "Year", "All Time")
    var selectedRange by remember { mutableStateOf("Month") }
    var rangeExpanded by remember { mutableStateOf(false) }

    val options = remember(selectedRange) {
        val list = mutableListOf<String>()
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        when (selectedRange) {
            "Day" -> {
                for (i in 0..30) {
                    val d = Calendar.getInstance()
                    d.add(Calendar.DAY_OF_YEAR, -i)
                    list.add(
                        when (i) {
                            0 -> "Today"
                            1 -> "Yesterday"
                            else -> sdf.format(d.time)
                        },
                    )
                }
            }
            "Week" -> {
                for (i in 0..12) {
                    val d = Calendar.getInstance()
                    d[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
                    d.add(Calendar.WEEK_OF_YEAR, -i)
                    val end = d.clone() as Calendar
                    end.add(Calendar.DAY_OF_YEAR, 6)
                    list.add(if (i == 0) "This Week" else "${sdf.format(d.time)} - ${sdf.format(end.time)}")
                }
            }
            "Month" -> {
                val monthSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                for (i in 0..12) {
                    val d = Calendar.getInstance()
                    d.add(Calendar.MONTH, -i)
                    list.add(monthSdf.format(d.time))
                }
            }
            "Year" -> {
                val year = Calendar.getInstance()[Calendar.YEAR]
                for (i in 0..5) {
                    list.add((year - i).toString())
                }
            }
            "All Time" -> {
                list.add("Entire History")
            }
        }
        list
    }

    var selectedOption by remember(selectedRange) { mutableStateOf(options.firstOrNull() ?: "") }
    var optionExpanded by remember { mutableStateOf(false) }

    var topScoresSortMode by remember { mutableStateOf("Accuracy") }
    var topScoresGameFilter by remember { mutableStateOf("All") }
    var selectedScoreForDetails by remember { mutableStateOf<FullScoreRecord?>(null) }

    // Data filtering and aggregation logic
    val filteredScores = remember(scores, selectedOption, selectedRange) {
        val currentScores = scores ?: return@remember emptyList<FullScoreRecord>()
        
        val sdfDay = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        
        currentScores.filter { record ->
            val playDate = Date(record.genericScore.playTimestamp)
            val playCalendar = Calendar.getInstance().apply { time = playDate }
            
            when (selectedRange) {
                "Day" -> {
                    when (selectedOption) {
                        "Today" -> isSameDay(playCalendar, Calendar.getInstance())
                        "Yesterday" -> {
                            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                            isSameDay(playCalendar, yesterday)
                        }
                        else -> sdfDay.format(playDate) == selectedOption
                    }
                }
                "Week" -> {
                    val weekRange = selectedOption.split(" - ")
                    if (weekRange.size == 2) {
                        try {
                            val start = sdfDay.parse(weekRange[0])
                            val end = sdfDay.parse(weekRange[1])
                            if (start != null && end != null) {
                                playDate.after(start) && playDate.before(Date(end.time + 86400000))
                            } else false
                        } catch (e: Exception) {
                            false
                        }
                    } else if (selectedOption == "This Week") {
                        val startOfWeek = Calendar.getInstance().apply { 
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        playDate.after(startOfWeek.time)
                    } else false
                }
                "Month" -> sdfMonth.format(playDate) == selectedOption
                "Year" -> playCalendar[Calendar.YEAR].toString() == selectedOption
                "All Time" -> true
                else -> false
            }
        }
    }

    val topGames = remember(filteredScores, configs) {
        filteredScores.groupBy { it.genericScore.configId }
            .map { (configId, groupScores) ->
                val config = configs.find { it.id == configId }
                val booleanFields = config?.let {
                    try {
                        json.decodeFromString<GameConfigData>(it.configData).allFieldsWithCategory
                            .filter { (field, _) -> field.type == "boolean" }
                            .map { (field, _) -> field.key to field.label }
                    } catch (e: Exception) { emptyList<Pair<String, String>>() }
                } ?: emptyList()

                val achievementList = booleanFields.mapNotNull { (key, label) ->
                    val count = groupScores.sumOf { record ->
                        record.details.count { it.key == key && it.value == "true" }
                    }
                    if (count > 0) label to count else null
                }

                TopGameItem(
                    rank = 0,
                    gameName = config?.gameName ?: "Unknown",
                    playCount = groupScores.size,
                    configId = configId,
                    achievements = achievementList,
                    displayIconUri = config?.displayIconUri
                )
            }
            .sortedByDescending { it.playCount }
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    val topCharts = remember(filteredScores, configs) {
        filteredScores.groupBy { "${it.genericScore.configId}|${it.genericScore.songTitle}|${it.genericScore.difficultyName}|${it.genericScore.difficultyVal}" }
            .map { (key, groupScores) ->
                val configId = key.split("|").first().toLong()
                val config = configs.find { it.id == configId }
                val first = groupScores.first().genericScore

                val booleanFields = config?.let {
                    try {
                        json.decodeFromString<GameConfigData>(it.configData).allFieldsWithCategory
                            .filter { (field, _) -> field.type == "boolean" }
                            .map { (field, _) -> field.key to field.label }
                    } catch (e: Exception) { emptyList<Pair<String, String>>() }
                } ?: emptyList()

                val achievementList = booleanFields.mapNotNull { (fieldKey, label) ->
                    val count = groupScores.sumOf { record ->
                        record.details.count { it.key == fieldKey && it.value == "true" }
                    }
                    if (count > 0) label to count else null
                }

                TopChartItem(
                    rank = 0,
                    songTitle = first.songTitle,
                    playCount = groupScores.size,
                    configId = configId,
                    difficultyName = first.difficultyName,
                    difficultyVal = first.difficultyVal,
                    achievements = achievementList,
                    displayIconUri = config?.displayIconUri
                )
            }
            .sortedByDescending { it.playCount }
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    val topScores = remember(filteredScores, configs, topScoresSortMode, topScoresGameFilter) {
        filteredScores
            .filter { record ->
                if (topScoresGameFilter == "All") true
                else {
                    val config = configs.find { it.id == record.genericScore.configId }
                    config?.gameName == topScoresGameFilter
                }
            }
            .map { record ->
                val config = configs.find { it.id == record.genericScore.configId }
                val totalNoteCount = if (config != null) {
                    try {
                        val cData = json.decodeFromString<GameConfigData>(config.configData)
                        AccuracyCalculator.getTotalNoteCount(record.details, cData)
                    } catch (e: Exception) { 0 }
                } else 0
                
                // Fallback weight if note count is unavailable (avoid 0 weight)
                val weight = totalNoteCount.coerceAtLeast(1).toDouble()

                val rankingValue = if (topScoresSortMode == "Accuracy") {
                    weight * record.genericScore.accuracy
                } else {
                    weight * record.genericScore.maxCombo
                }
                Pair(record, rankingValue)
            }
            .sortedByDescending { it.second }
            .mapIndexed { index, (record, _) ->
                val config = configs.find { it.id == record.genericScore.configId }
                TopScoreItem(
                    scoreId = record.genericScore.id,
                    rank = index + 1,
                    songTitle = record.genericScore.songTitle,
                    difficultyName = record.genericScore.difficultyName,
                    difficultyVal = record.genericScore.difficultyVal,
                    accuracy = record.genericScore.accuracy,
                    maxCombo = record.genericScore.maxCombo,
                    playRank = record.genericScore.playRank,
                    gameName = config?.gameName ?: "Unknown",
                    displayIconUri = config?.displayIconUri
                )
            }
    }

    val activityData = remember(filteredScores, selectedRange) {
        if (filteredScores.isEmpty()) return@remember emptyList<ActivityDataPoint>()

        val calendar = Calendar.getInstance()
        when (selectedRange) {
            "Day" -> {
                val hourlyCounts = IntArray(24) { 0 }
                filteredScores.forEach {
                    calendar.timeInMillis = it.genericScore.playTimestamp
                    hourlyCounts[calendar.get(Calendar.HOUR_OF_DAY)]++
                }
                hourlyCounts.mapIndexed { index, count ->
                    ActivityDataPoint("${index}h", count)
                }
            }
            "Week" -> {
                val dailyCounts = IntArray(7) { 0 }
                filteredScores.forEach {
                    calendar.timeInMillis = it.genericScore.playTimestamp
                    val dow = calendar.get(Calendar.DAY_OF_WEEK)
                    val index = (dow + 5) % 7 // Map Mon=0, Sun=6
                    dailyCounts[index]++
                }
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                dailyCounts.mapIndexed { index, count ->
                    ActivityDataPoint(days[index], count)
                }
            }
            "Month" -> {
                val bins = IntArray(4) { 0 }
                filteredScores.forEach {
                    calendar.timeInMillis = it.genericScore.playTimestamp
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    val binIndex = ((day - 1) / 7).coerceAtMost(3)
                    bins[binIndex]++
                }
                bins.mapIndexed { index, count ->
                    ActivityDataPoint("W${index + 1}", count)
                }
            }
            "Year" -> {
                val monthlyCounts = IntArray(12) { 0 }
                filteredScores.forEach {
                    calendar.timeInMillis = it.genericScore.playTimestamp
                    monthlyCounts[calendar.get(Calendar.MONTH)]++
                }
                val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                monthlyCounts.mapIndexed { index, count ->
                    ActivityDataPoint(months[index], count)
                }
            }
            "All Time" -> {
                val yearlyMap = mutableMapOf<Int, Int>()
                filteredScores.forEach {
                    calendar.timeInMillis = it.genericScore.playTimestamp
                    val year = calendar.get(Calendar.YEAR)
                    yearlyMap[year] = yearlyMap.getOrDefault(year, 0) + 1
                }
                yearlyMap.keys.sorted().map { year ->
                    ActivityDataPoint(year.toString(), yearlyMap[year] ?: 0)
                }
            }
            else -> emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.weight(0.4f)) {
                        Surface(
                            onClick = { rangeExpanded = true },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedRange, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = rangeExpanded,
                            onDismissRequest = { rangeExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            ranges.forEach { range ->
                                DropdownMenuItem(
                                    text = { Text(range) },
                                    onClick = {
                                        selectedRange = range
                                        rangeExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(0.6f)) {
                        Surface(
                            onClick = { if (selectedRange != "All Time") optionExpanded = true },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    selectedOption,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                if (selectedRange != "All Time") {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (scores == null) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredScores.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.ohnoes),
                                contentDescription = null,
                                modifier = Modifier.size(128.dp),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "No data for $selectedOption", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            } else {
                item {
                    TopGamesCard(
                        games = topGames,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        onCardClick = { 
                            val encodedOption = android.net.Uri.encode(selectedOption)
                            navController.navigate(com.example.ritsu.Screen.TopGames.name + "/$selectedRange/$encodedOption") 
                        },
                        onMoreClick = { 
                            val encodedOption = android.net.Uri.encode(selectedOption)
                            navController.navigate(com.example.ritsu.Screen.TopGames.name + "/$selectedRange/$encodedOption") 
                        }
                    )
                }
                
                item {
                    TopChartsCard(
                        charts = topCharts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        onCardClick = { 
                            val encodedOption = android.net.Uri.encode(selectedOption)
                            navController.navigate(com.example.ritsu.Screen.TopCharts.name + "/$selectedRange/$encodedOption") 
                        },
                        onMoreClick = { 
                            val encodedOption = android.net.Uri.encode(selectedOption)
                            navController.navigate(com.example.ritsu.Screen.TopCharts.name + "/$selectedRange/$encodedOption") 
                        },
                        onChartClick = { chart ->
                            val encodedTitle = android.net.Uri.encode(chart.songTitle)
                            val encodedDiffName = android.net.Uri.encode(chart.difficultyName)
                            val encodedDiffVal = android.net.Uri.encode(chart.difficultyVal)
                            navController.navigate(com.example.ritsu.Screen.ChartDetails.name + "/${chart.configId}/$encodedTitle/$encodedDiffName/$encodedDiffVal")
                        }
                    )
                }
                
                item {
                    TopScoresCard(
                        scores = topScores,
                        selectedSort = topScoresSortMode,
                        onSortChange = { topScoresSortMode = it },
                        availableGames = configs.map { it.gameName }.distinct(),
                        selectedGame = topScoresGameFilter,
                        onGameChange = { topScoresGameFilter = it },
                        onCardClick = { 
                            val encodedOption = android.net.Uri.encode(selectedOption)
                            val encodedGameFilter = android.net.Uri.encode(topScoresGameFilter)
                            navController.navigate(com.example.ritsu.Screen.TopScores.name + "/$selectedRange/$encodedOption/$topScoresSortMode/$encodedGameFilter") 
                        },
                        onMoreClick = { 
                            val encodedOption = android.net.Uri.encode(selectedOption)
                            val encodedGameFilter = android.net.Uri.encode(topScoresGameFilter)
                            navController.navigate(com.example.ritsu.Screen.TopScores.name + "/$selectedRange/$encodedOption/$topScoresSortMode/$encodedGameFilter") 
                        },
                        onScoreClick = { item ->
                            selectedScoreForDetails = filteredScores.find { it.genericScore.id == item.scoreId }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                
                item {
                    ActivityGraphCard(
                        dataPoints = activityData,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }

    if (optionExpanded) {
        ModalBottomSheet(
            onDismissRequest = { optionExpanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                item {
                    Text(
                        text = "Select $selectedRange",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(options) { option ->
                    ListItem(
                        headlineContent = { Text(option) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            selectedOption = option
                            optionExpanded = false
                        },
                    )
                }
            }
        }
    }

    if (selectedScoreForDetails != null) {
        val config = configs.find { it.id == selectedScoreForDetails!!.genericScore.configId }
        if (config != null) {
            ScoreDetailsDialog(
                scoreRecord = selectedScoreForDetails!!,
                gameConfig = config,
                scoreDao = database.scoreDao(),
                onDismiss = { selectedScoreForDetails = null },
                onChartDetails = {
                    val s = selectedScoreForDetails!!.genericScore
                    val encodedTitle = android.net.Uri.encode(s.songTitle)
                    val encodedDiffName = android.net.Uri.encode(s.difficultyName)
                    val encodedDiffVal = android.net.Uri.encode(s.difficultyVal)
                    selectedScoreForDetails = null
                    navController.navigate(com.example.ritsu.Screen.ChartDetails.name + "/${s.configId}/$encodedTitle/$encodedDiffName/$encodedDiffVal")
                }
            )
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1[Calendar.YEAR] == cal2[Calendar.YEAR] &&
            cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]
}
