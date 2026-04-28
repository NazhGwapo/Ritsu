package com.example.ritsu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.RankingUtils
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.cards.ScoreListItem
import com.example.ritsu.ui.cards.TopScoreItem
import com.example.ritsu.ui.components.ScoreDetailsDialog
import com.example.ritsu.ui.navigation.Screen
import com.example.ritsu.ui.screens.debug.ManualEntryDialog
import com.example.ritsu.ui.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TopChartsScreen(
    range: String,
    option: String,
    navController: NavController
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scores by database.scoreDao().getAllScores().collectAsState(initial = emptyList())
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())

    val sortMode = "Plays"

    val filteredScores = remember(scores, range, option) {
        val currentScores = scores ?: return@remember emptyList<FullScoreRecord>()
        val sdfDay = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        
        currentScores.filter { record ->
            val playDate = Date(record.genericScore.playTimestamp)
            val playCalendar = Calendar.getInstance().apply { time = playDate }
            
            when (range) {
                "Day" -> {
                    when (option) {
                        "Today" -> DateUtils.isSameDay(playCalendar, Calendar.getInstance())
                        "Yesterday" -> {
                            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                            DateUtils.isSameDay(playCalendar, yesterday)
                        }
                        else -> sdfDay.format(playDate) == option
                    }
                }
                "Week" -> {
                    val weekRange = option.split(" - ")
                    if (weekRange.size == 2) {
                        try {
                            val start = sdfDay.parse(weekRange[0])
                            val end = sdfDay.parse(weekRange[1])
                            if (start != null && end != null) {
                                playDate.after(start) && playDate.before(Date(end.time + 86400000))
                            } else false
                        } catch (e: Exception) { false }
                    } else if (option == "This Week") {
                        val startOfWeek = Calendar.getInstance().apply { 
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        playDate.after(startOfWeek.time)
                    } else false
                }
                "Month" -> sdfMonth.format(playDate) == option
                "Year" -> playCalendar[Calendar.YEAR].toString() == option
                "All Time" -> true
                else -> false
            }
        }
    }

    val topCharts = remember(filteredScores, configs, sortMode) {
        filteredScores.groupBy { "${it.genericScore.configId}|${it.genericScore.songTitle}|${it.genericScore.difficultyName}|${it.genericScore.difficultyVal}" }
            .map { (key, groupScores) ->
                val configId = key.split("|").first().toLong()
                val config = configs.find { it.id == configId }
                
                val bestRecord = if (sortMode == "Plays") {
                    groupScores.maxByOrNull { it.genericScore.playTimestamp }!!
                } else {
                    groupScores.maxByOrNull { record ->
                        RankingUtils.calculateRankingValue(record, config, sortMode)
                    }!!
                }

                RankingUtils.mapToTopScoreItem(
                    record = bestRecord,
                    config = config,
                    playCount = groupScores.size,
                    showRank = (sortMode != "Plays")
                )
            }
            .sortedByDescending { item ->
                if (sortMode == "Plays") {
                    item.playCount.toDouble()
                } else {
                    val config = configs.find { it.gameName == item.gameName && it.displayIconUri == item.displayIconUri }
                    val originalRecord = filteredScores.find { 
                        it.genericScore.songTitle == item.songTitle &&
                        it.genericScore.difficultyName == item.difficultyName &&
                        it.genericScore.difficultyVal == item.difficultyVal &&
                        it.genericScore.id == item.scoreId
                    }
                    if (originalRecord != null) {
                        RankingUtils.calculateRankingValue(originalRecord, config, sortMode)
                    } else 0.0
                }
            }
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    var selectedScoreForDetails by remember { mutableStateOf<FullScoreRecord?>(null) }
    var scoreToEdit by remember { mutableStateOf<FullScoreRecord?>(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(topCharts) { score ->
                ScoreListItem(score = score, selectedSort = sortMode, onClick = { 
                    if (sortMode == "Plays") {
                        val config = configs.find { it.gameName == score.gameName && it.displayIconUri == score.displayIconUri }
                        val configId = config?.id ?: 0L
                        val encodedTitle = android.net.Uri.encode(score.songTitle)
                        val encodedDiffName = android.net.Uri.encode(score.difficultyName)
                        val encodedDiffVal = android.net.Uri.encode(score.difficultyVal)
                        navController.navigate(Screen.ChartDetails.name + "/$configId/$encodedTitle/$encodedDiffName/$encodedDiffVal")
                    } else {
                        selectedScoreForDetails = filteredScores.find { it.genericScore.id == score.scoreId }
                    }
                })
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
                    navController.navigate(Screen.ChartDetails.name + "/${s.configId}/$encodedTitle/$encodedDiffName/$encodedDiffVal")
                },
                onGameDetails = {
                    val s = selectedScoreForDetails!!.genericScore
                    selectedScoreForDetails = null
                    navController.navigate(Screen.GameDetails.name + "/${s.configId}")
                },
                onEdit = {
                    scoreToEdit = selectedScoreForDetails
                    selectedScoreForDetails = null
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
