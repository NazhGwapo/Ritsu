package com.example.ritsu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.ritsu.ui.utils.AsyncImage
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import kotlinx.serialization.json.Json

@Composable
fun ConfigDetailDialog(
    config: GameConfig,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onPickFromGallery: () -> Unit,
    onPickFromApps: () -> Unit,
    onUpdateConfig: (GameConfig) -> Unit = {},
) {
    val json = remember { Json { ignoreUnknownKeys = true; prettyPrint = true } }
    val configData = remember(config) {
        try {
            json.decodeFromString<GameConfigData>(config.configData)
        } catch (_: Exception) {
            null
        }
    }

    var showRaw by remember { mutableStateOf(value = false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(config.gameName)
                TextButton(onClick = { showRaw = !showRaw }) {
                    Text(if (showRaw) "Show Fields" else "Show Raw")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Icon Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (config.displayIconUri != null) {
                            AsyncImage(
                                model = config.displayIconUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                    }
                    
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Button(
                            onClick = onPickFromGallery,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pick from Gallery")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onPickFromApps,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pick from Apps")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (showRaw || (configData == null)) {
                    val prettyJson = remember(config.configData) {
                        try {
                            val obj = json.decodeFromString<GameConfigData>(config.configData)
                            json.encodeToString(obj)
                        } catch (_: Exception) {
                            config.configData
                        }
                    }
                    Text(
                        text = prettyJson,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    // System Settings
                    Text("System Settings", style = MaterialTheme.typography.titleSmall)
                    
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newData = configData.copy(useRankOcr = !configData.useRankOcr)
                                    onUpdateConfig(config.copy(configData = json.encodeToString(newData)))
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = configData.useRankOcr,
                                onCheckedChange = {
                                    val newData = configData.copy(useRankOcr = it)
                                    onUpdateConfig(config.copy(configData = json.encodeToString(newData)))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Rank OCR", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "Standard OCR may fail on highly stylized rank icons (e.g., SSS+ with sparkles).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 48.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newData = configData.copy(useAccuracyOcr = !configData.useAccuracyOcr)
                                    onUpdateConfig(config.copy(configData = json.encodeToString(newData)))
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = configData.useAccuracyOcr,
                                onCheckedChange = {
                                    val newData = configData.copy(useAccuracyOcr = it)
                                    onUpdateConfig(config.copy(configData = json.encodeToString(newData)))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Accuracy OCR", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "Disable this if the game doesn't show accuracy percentage. Ritsu will calculate it from judge counts instead.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 48.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Custom Fields", style = MaterialTheme.typography.titleSmall)

                    configData.allFieldsWithCategory.forEach { (field, category) ->
                        ListItem(
                            headlineContent = { Text(field.label) },
                            supportingContent = {
                                Column {
                                    Text("Category: $category | Key: ${field.key} | Type: ${field.type}")
                                    if ((field.type == "boolean") && (field.targetColor != null)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Target Color: ")
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline,
                                                        RoundedCornerShape(2.dp),
                                                    )
                                                    .background(androidx.compose.ui.graphics.Color(field.targetColor))
                                            )
                                            Text(" | Threshold: ${"%.2f".format(field.threshold)}")
                                        }
                                    }
                                }
                            },
                        )
                    }
                    configData.formula?.let {
                        Text(
                            "Formula: $it",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Config",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}
