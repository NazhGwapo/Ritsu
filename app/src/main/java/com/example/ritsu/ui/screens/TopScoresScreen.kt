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
fun TopScoresScreen(
    range: String,
    option: String,
    sortMode: String,
    gameFilter: String,
    navController: NavController
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scores by database.scoreDao().getAllScores().collectAsState(initial = emptyList())
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())

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

    val topScores = remember(filteredScores, configs, sortMode, gameFilter) {
        filteredScores
            .filter { record ->
                if (gameFilter == "All") true
                else {
                    val config = configs.find { it.id == record.genericScore.configId }
                    config?.gameName == gameFilter
                }
            }
            .map { record ->
                val config = configs.find { it.id == record.genericScore.configId }
                val rankingValue = RankingUtils.calculateRankingValue(record, config, sortMode)
                Pair(record, rankingValue)
            }
            .sortedByDescending { it.second }
            .mapIndexed { index, (record, _) ->
                val config = configs.find { it.id == record.genericScore.configId }
                RankingUtils.mapToTopScoreItem(
                    record = record,
                    config = config,
                    rank = index + 1
                )
            }
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
            items(topScores) { score ->
                ScoreListItem(score = score, selectedSort = sortMode, onClick = { 
                    selectedScoreForDetails = filteredScores.find { it.genericScore.id == score.scoreId }
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
