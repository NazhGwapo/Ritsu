package com.example.ritsu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.cards.GameListItem
import com.example.ritsu.ui.cards.TopGameItem
import com.example.ritsu.ui.utils.DateUtils
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TopGamesScreen(
    range: String,
    option: String,
    onGameClick: (TopGameItem) -> Unit
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scores by database.scoreDao().getAllScores().collectAsState(initial = emptyList())
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    val json = remember { Json { ignoreUnknownKeys = true } }

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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(topGames) { game ->
                GameListItem(game = game, onClick = { onGameClick(game) })
            }
        }
    }
}
