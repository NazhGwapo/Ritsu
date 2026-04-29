package com.example.ritsu.ui.screens.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.RitsuDatabase

@Composable
fun DatabaseViewerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    val scores by database.scoreDao().getAllScores().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Scores", "Configs")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> ScoresList(scores)
                        1 -> ConfigsList(configs)
                    }
                }

                HorizontalDivider()
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun ScoresList(scores: List<FullScoreRecord>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(scores) { record ->
            ScoreItem(record)
            HorizontalDivider()
        }
    }
}

@Composable
fun ScoreItem(record: FullScoreRecord) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = record.genericScore.songTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${record.genericScore.difficultyName} (${record.genericScore.difficultyVal})",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Score: ${record.genericScore.totalScore} | Combo: ${record.genericScore.maxCombo} | Rank: ${record.genericScore.playRank}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Accuracy: ${record.genericScore.accuracy}%",
            style = MaterialTheme.typography.bodySmall
        )
        
        if (record.details.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Details:", style = MaterialTheme.typography.labelSmall)
            record.details.forEach { detail ->
                Text(
                    text = "${detail.key}: ${detail.value}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ConfigsList(configs: List<GameConfig>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(configs) { config ->
            ConfigItem(config)
            HorizontalDivider()
        }
    }
}

@Composable
fun ConfigItem(config: GameConfig) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = config.gameName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "ID: ${config.id} | Version: ${config.configVersion}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Data: ${config.configData}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3
        )
    }
}
