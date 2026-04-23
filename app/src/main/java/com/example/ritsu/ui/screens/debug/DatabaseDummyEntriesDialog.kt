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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
                                repeat(amount) {
                                    val randomScore = createRandomScore(config.id)
                                    val scoreId = database.scoreDao().insertScore(randomScore)
                                    
                                    val details = configData.fields.map { field ->
                                        ScoreDetail(
                                            scoreId = scoreId,
                                            key = field.key,
                                            value = Random.nextInt(0, 1000).toString()
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

private fun createRandomScore(configId: Long): GenericScore {
    val songs = listOf("Stellar Stellar", "Ghost", "Phony", "Identity", "Salamander", "Lower One's Eyes")
    val diffs = listOf("Easy", "Normal", "Hard", "Expert", "Master")
    val ranks = listOf("S", "SS", "SSS", "A", "B")
    
    val randomDiffVal = (Random.nextDouble(1.0, 30.0) * 100).toInt() / 100.0
    val diffStr = if (Random.nextBoolean()) "$randomDiffVal+" else "$randomDiffVal"
    
    return GenericScore(
        configId = configId,
        songTitle = songs.random(),
        difficultyName = diffs.random(),
        difficultyVal = diffStr,
        difficultySortValue = GenericScore.parseDifficulty(diffStr),
        totalScore = Random.nextLong(500000, 1000000),
        maxCombo = Random.nextInt(100, 2000),
        accuracy = (Random.nextDouble(80.0, 100.0) * 100).toInt() / 100.0,
        playRank = ranks.random(),
        timestamp = System.currentTimeMillis() - Random.nextLong(0, 1000L * 60 * 60 * 24 * 30) // Within last 30 days
    )
}
