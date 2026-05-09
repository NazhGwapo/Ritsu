package com.example.ritsu.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ritsu.data.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreEditDialog(
    scoreRecord: FullScoreRecord,
    gameConfig: GameConfig,
    onDismiss: () -> Unit,
    onSave: (GenericScore, List<ScoreDetail>) -> Unit
) {
    val context = LocalContext.current
    val json = remember { Json { ignoreUnknownKeys = true } }
    val configData = remember(gameConfig) { json.decodeFromString<GameConfigData>(gameConfig.configData) }

    // Core score fields
    var songTitle by remember { mutableStateOf(scoreRecord.genericScore.songTitle) }
    var difficultyName by remember { mutableStateOf(scoreRecord.genericScore.difficultyName) }
    var difficultyVal by remember { mutableStateOf(scoreRecord.genericScore.difficultyVal) }
    var totalScore by remember { mutableStateOf(scoreRecord.genericScore.totalScore.toString()) }
    var maxCombo by remember { mutableStateOf(scoreRecord.genericScore.maxCombo.toString()) }
    var accuracy by remember { mutableStateOf(scoreRecord.genericScore.accuracy.toString()) }
    var playRank by remember { mutableStateOf(scoreRecord.genericScore.playRank) }
    var playTimestamp by remember { mutableStateOf(scoreRecord.genericScore.playTimestamp) }

    // Dynamic detail fields
    var detailValues by remember { 
        mutableStateOf(scoreRecord.details.associate { it.key to it.value }) 
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Edit Score", style = MaterialTheme.typography.titleLarge) }
                    )
                },
                bottomBar = {
                    Surface(tonalElevation = 8.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val updatedScore = scoreRecord.genericScore.copy(
                                        songTitle = songTitle,
                                        difficultyName = difficultyName,
                                        difficultyVal = difficultyVal,
                                        difficultySortValue = GenericScore.parseDifficulty(difficultyVal),
                                        totalScore = totalScore.toLongOrNull() ?: 0L,
                                        maxCombo = maxCombo.toIntOrNull() ?: 0,
                                        accuracy = accuracy.toDoubleOrNull() ?: 0.0,
                                        playRank = playRank,
                                        playTimestamp = playTimestamp
                                    )
                                    val updatedDetails = configData.allFieldsWithCategory.map { (field, category) ->
                                        ScoreDetail(
                                            scoreId = scoreRecord.genericScore.id,
                                            key = field.key,
                                            value = detailValues[field.key] ?: "",
                                            category = category
                                        )
                                    }
                                    onSave(updatedScore, updatedDetails)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Basic Information", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = songTitle,
                        onValueChange = { songTitle = it },
                        label = { Text("Song Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = difficultyName,
                            onValueChange = { difficultyName = it },
                            label = { Text("Difficulty") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = difficultyVal,
                            onValueChange = { difficultyVal = it },
                            label = { Text("Level") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Result Details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = totalScore,
                            onValueChange = { totalScore = it },
                            label = { Text("Score") },
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = playRank,
                            onValueChange = { playRank = it },
                            label = { Text("Rank") },
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = accuracy,
                            onValueChange = { accuracy = it },
                            label = { Text("Accuracy (%)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxCombo,
                            onValueChange = { maxCombo = it },
                            label = { Text("Max Combo") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Timestamp Editor
                    Column {
                        Text("Play Date & Time", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val calendar = remember { Calendar.getInstance() }
                            calendar.timeInMillis = playTimestamp
                            val sdfDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

                            AssistChip(
                                onClick = {
                                    DatePickerDialog(context, { _, y, m, d ->
                                        calendar.set(y, m, d)
                                        playTimestamp = calendar.timeInMillis
                                    }, calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DAY_OF_MONTH]).show()
                                },
                                label = { Text(sdfDate.format(Date(playTimestamp))) },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp)) }
                            )
                            AssistChip(
                                onClick = {
                                    TimePickerDialog(context, { _, h, m ->
                                        calendar.set(Calendar.HOUR_OF_DAY, h)
                                        calendar.set(Calendar.MINUTE, m)
                                        playTimestamp = calendar.timeInMillis
                                    }, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], true).show()
                                },
                                label = { Text(sdfTime.format(Date(playTimestamp))) },
                                leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }

                    if (configData.allFieldsWithCategory.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Game Specific Fields", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        
                        configData.allFieldsWithCategory.forEach { (field, _) ->
                            OutlinedTextField(
                                value = detailValues[field.key] ?: "",
                                onValueChange = { val m = detailValues.toMutableMap(); m[field.key] = it; detailValues = m },
                                label = { Text(field.label) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
