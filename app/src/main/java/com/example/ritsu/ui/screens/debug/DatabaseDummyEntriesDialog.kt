package com.example.ritsu.ui.screens.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.GenericScore
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.data.ScoreDetail
import com.example.ritsu.ui.theme.RitsuTheme
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Composable
fun DatabaseDummyEntriesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedConfig by remember { mutableStateOf<GameConfig?>(null) }
    var amountText by remember { mutableStateOf("10") }
    var noDecimalDifficulty by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val json = Json { ignoreUnknownKeys = true }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Add Dummy Entries", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                // Config Selector
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { expanded = true }
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedConfig?.gameName ?: "Select Game Config",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        configs.forEach { config ->
                            DropdownMenuItem(
                                text = { Text(config.gameName) },
                                onClick = {
                                    selectedConfig = config
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { noDecimalDifficulty = !noDecimalDifficulty }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "No decimal for difficulty", modifier = Modifier.weight(1f))
                    Switch(checked = noDecimalDifficulty, onCheckedChange = null)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount of entries") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val amount = amountText.toIntOrNull() ?: 0
                        val config = selectedConfig
                        if (amount > 0 && config != null) {
                            scope.launch {
                                val configData = json.decodeFromString<GameConfigData>(config.configData)
                                
                                val songs = listOf("Maihime", "Ghost", "Phony", "Identity", "Harumachi Clover (TV Size)", "Catch a Fire", "Umapyoi Densetsu", "Blue Zenith", "Lagtrain", "Cerise", "CHAMPION GIRL", "Zurui Magnetic Today", "Shining Lights", "Hysteric Night Girl -Awakening-", "Aozora Jumping Heart", "BRAVE JEWEL", "Oedo Controller (feat. Toriena)", "Aimai Attitude (feat. Sanso Nakamura)", "Don't Say Lazy! (YUC'e Remix)", "Super-Slow-Internet-San", "Fluorite", "Tokimeki Scramble", "Raise Your Hand (feat. Such)", "Undercover (Teddyloid Remix)", "Secret Dance Hall (feat. PSYQUI)", "Photon Melodies (TAKU INOUE Remix)", "Attsu Attsu Tokonatsu Love Summer", "Luna say maybe", "Yang Guang Cai Hong Xiao Bai Ma", "I'm getting on the bus to the other world")
                                val diffNames = listOf("Easy", "Normal", "Hard", "Expert", "Master")
                                
                                // Pre-generate consistent charts for each song
                                val songChartMap = songs.associateWith {
                                    val availableDiffs = diffNames.shuffled().take(Random.nextInt(2, 6))
                                    availableDiffs.associateWith { name ->
                                        val valBase = if (noDecimalDifficulty) {
                                            Random.nextInt(1, 30).toDouble()
                                        } else {
                                            (Random.nextDouble(1.0, 30.0) * 10).toInt() / 10.0
                                        }
                                        if (!noDecimalDifficulty && Random.nextBoolean()) "$valBase+" else "${valBase.toInt()}"
                                    }
                                }

                                repeat(amount) {
                                    val songTitle = songs.random()
                                    val charts = songChartMap[songTitle]!!
                                    val diffName = charts.keys.toList().random()
                                    val diffVal = charts[diffName]!!

                                    val judgments = configData.judgments
                                    val totalNotes = Random.nextInt(500, 2000)
                                    var remainingNotes = totalNotes
                                    
                                    val judgmentValues = mutableMapOf<String, Int>()
                                    judgments.forEachIndexed { index, field ->
                                        val count = if (index == judgments.size - 1) { // Assume last is Miss
                                            Random.nextInt(0, 10).coerceAtMost(remainingNotes / 10)
                                        } else {
                                            val share = (remainingNotes * (0.7 / (index + 1))).toInt()
                                            share
                                        }
                                        judgmentValues[field.key] = count
                                        remainingNotes -= count
                                    }
                                    if (judgments.isNotEmpty()) {
                                        judgmentValues[judgments.first().key] = (judgmentValues[judgments.first().key] ?: 0) + remainingNotes
                                    }

                                    // Calculate accuracy and score
                                    var weightedHits = 0.0
                                    judgments.forEachIndexed { index, field ->
                                        val count = judgmentValues[field.key] ?: 0
                                        val weight = if (index == judgments.size - 1) 0.0 else (1.0 - (index * 0.2)).coerceAtLeast(0.1)
                                        weightedHits += count * weight
                                    }
                                    val accuracy = (weightedHits / totalNotes) * 100
                                    val totalScore = (accuracy * 10000).toLong()
                                    val playRank = when {
                                        accuracy >= 99 -> "SSS"
                                        accuracy >= 97 -> "SS"
                                        accuracy >= 95 -> "S"
                                        accuracy >= 90 -> "A"
                                        accuracy >= 80 -> "B"
                                        else -> "C"
                                    }

                                    val randomScore = GenericScore(
                                        configId = config.id,
                                        songTitle = songTitle,
                                        difficultyName = diffName,
                                        difficultyVal = diffVal,
                                        difficultySortValue = GenericScore.parseDifficulty(diffVal),
                                        totalScore = totalScore,
                                        maxCombo = totalNotes - (judgmentValues[judgments.lastOrNull()?.key] ?: 0),
                                        accuracy = (accuracy * 100).toInt() / 100.0,
                                        playRank = playRank,
                                        playTimestamp = System.currentTimeMillis() - Random.nextLong(0, 1000L * 60 * 60 * 24 * 30),
                                        importTimestamp = System.currentTimeMillis()
                                    )
                                    
                                    val scoreId = database.scoreDao().insertScore(randomScore)

                                    val details = configData.allFieldsWithCategory.map { (field, category) ->
                                        val value = when {
                                            field.type == "boolean" -> Random.nextBoolean().toString()
                                            category == "Judgment" -> judgmentValues[field.key]?.toString() ?: "0"
                                            else -> Random.nextInt(0, 1000).toString()
                                        }
                                        ScoreDetail(
                                            scoreId = scoreId,
                                            key = field.key,
                                            value = value,
                                            category = category
                                        )
                                    }
                                    database.scoreDao().insertDetails(details)
                                }
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedConfig != null && amountText.toIntOrNull() != null
                ) {
                    Text("Generate Entries")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DatabaseDummyEntriesDialogPreview() {
    RitsuTheme {
        // Just show the inner content for preview since Dialog might not render
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Add Dummy Entries", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "No decimal for difficulty", modifier = Modifier.weight(1f))
                    Switch(checked = true, onCheckedChange = null)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = "10",
                    onValueChange = {},
                    label = { Text("Amount of entries") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Generate Entries")
                }
            }
        }
    }
}
