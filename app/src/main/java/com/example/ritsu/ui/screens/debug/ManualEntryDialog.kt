package com.example.ritsu.ui.screens.debug

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.GenericScore
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.data.ScoreDetail
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.AccuracyCalculator
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ManualEntryDialog(
    initialRecord: FullScoreRecord? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val json = remember { Json { ignoreUnknownKeys = true } }

    var selectedConfig by remember(initialRecord, configs) { 
        mutableStateOf(initialRecord?.let { rec -> configs.find { it.id == rec.genericScore.configId } }) 
    }
    var selectedConfigData by remember(selectedConfig) { 
        mutableStateOf(selectedConfig?.let { json.decodeFromString<GameConfigData>(it.configData) }) 
    }
    var expanded by remember { mutableStateOf(false) }

    // Generic Score Fields
    var songTitle by remember(initialRecord) { mutableStateOf(initialRecord?.genericScore?.songTitle ?: "") }
    var difficultyName by remember(initialRecord) { mutableStateOf(initialRecord?.genericScore?.difficultyName ?: "") }
    var difficultyVal by remember(initialRecord) { mutableStateOf(initialRecord?.genericScore?.difficultyVal ?: "") }
    var totalScore by remember(initialRecord) { mutableStateOf(initialRecord?.genericScore?.totalScore?.toString() ?: "") }
    var maxCombo by remember(initialRecord) { mutableStateOf(initialRecord?.genericScore?.maxCombo?.toString() ?: "") }
    var accuracy by remember(initialRecord) { mutableStateOf(initialRecord?.genericScore?.accuracy?.toString() ?: "") }
    var playRank by remember(initialRecord) { mutableStateOf(initialRecord?.genericScore?.playRank ?: "") }
    var playTimestamp by remember(initialRecord) { mutableStateOf(initialRecord?.genericScore?.playTimestamp ?: System.currentTimeMillis()) }
    var importTimestamp by remember(initialRecord) { mutableStateOf(initialRecord?.genericScore?.importTimestamp ?: System.currentTimeMillis()) }

    // Dynamic Fields for ScoreDetail
    var detailFields by remember(initialRecord, selectedConfigData) { 
        val initialMap = if (initialRecord != null && selectedConfigData != null) {
            initialRecord.details.associate { it.key to it.value }
        } else {
            selectedConfigData?.allFieldsWithCategory?.associate { it.first.key to "" } ?: emptyMap()
        }
        mutableStateOf<Map<String, String>>(initialMap)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Manual Database Entry", style = MaterialTheme.typography.headlineSmall)

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
                                    val configData = json.decodeFromString<GameConfigData>(config.configData)
                                    selectedConfigData = configData
                                    expanded = false
                                    // Reset detail fields when config changes
                                    detailFields = configData.allFieldsWithCategory.associate { it.first.key to "" }
                                }
                            )
                        }
                    }
                }

                if (selectedConfig != null && selectedConfigData != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextField(value = songTitle, onValueChange = { songTitle = it }, label = { Text("Song Title") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = difficultyName, onValueChange = { difficultyName = it }, label = { Text("Difficulty Name") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = difficultyVal, onValueChange = { difficultyVal = it }, label = { Text("Difficulty Value") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = totalScore, onValueChange = { totalScore = it }, label = { Text("Total Score") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = maxCombo, onValueChange = { maxCombo = it }, label = { Text("Max Combo") }, modifier = Modifier.fillMaxWidth())
                    
                    val calculatedAccuracy = remember(detailFields, selectedConfigData) {
                        selectedConfigData?.let { AccuracyCalculator.calculate(detailFields, it) }
                    }

                    TextField(
                        value = accuracy, 
                        onValueChange = { accuracy = it }, 
                        label = { 
                            Text(if (calculatedAccuracy != null) "Accuracy (%) - Auto: ${"%.2f".format(calculatedAccuracy)}" else "Accuracy (%)") 
                        }, 
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (calculatedAccuracy != null) {
                                TextButton(onClick = { accuracy = "%.2f".format(calculatedAccuracy) }) {
                                    Text("Apply Auto")
                                }
                            }
                        }
                    )

                    TextField(value = playRank, onValueChange = { playRank = it }, label = { Text("Play Rank") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Play Timestamp", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val calendar = remember { Calendar.getInstance() }
                        calendar.timeInMillis = playTimestamp
                        
                        val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                        val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        playTimestamp = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(dateFormatter.format(Date(playTimestamp)))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        playTimestamp = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(timeFormatter.format(Date(playTimestamp)))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Game Specific Fields", style = MaterialTheme.typography.titleMedium)
                    
                    selectedConfigData!!.allFieldsWithCategory.forEach { (field, _) ->
                        TextField(
                            value = detailFields[field.key] ?: "",
                            onValueChange = { newValue ->
                                val mutableMap = detailFields.toMutableMap()
                                mutableMap[field.key] = newValue
                                detailFields = mutableMap
                            },
                            label = { Text(field.label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                val score = GenericScore(
                                    id = initialRecord?.genericScore?.id ?: 0,
                                    configId = selectedConfig!!.id,
                                    songTitle = songTitle,
                                    difficultyName = difficultyName,
                                    difficultyVal = difficultyVal,
                                    difficultySortValue = GenericScore.parseDifficulty(difficultyVal),
                                    totalScore = totalScore.toLongOrNull() ?: 0L,
                                    maxCombo = maxCombo.toIntOrNull() ?: 0,
                                    accuracy = accuracy.toDoubleOrNull() ?: 0.0,
                                    playRank = playRank,
                                    playTimestamp = playTimestamp,
                                    importTimestamp = importTimestamp
                                )
                                val scoreId = if (initialRecord != null) {
                                    database.scoreDao().updateScore(score)
                                    initialRecord.genericScore.id
                                } else {
                                    database.scoreDao().insertScore(score)
                                }
                                
                                val details = selectedConfigData!!.allFieldsWithCategory.map { (field, category) ->
                                    ScoreDetail(
                                        scoreId = scoreId,
                                        key = field.key,
                                        value = detailFields[field.key] ?: "",
                                        category = category
                                    )
                                }
                                if (initialRecord != null) {
                                    database.scoreDao().deleteDetailsForScore(scoreId)
                                }
                                database.scoreDao().insertDetails(details)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (initialRecord != null) "Update Entry" else "Save to Database")
                    }
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
