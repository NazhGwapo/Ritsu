package com.example.ritsu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ManageConfigsScreen(onEditConfig: () -> Unit) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true; prettyPrint = true } }
    
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    var selectedConfig by remember { mutableStateOf<GameConfig?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onEditConfig) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Configs")
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(configs) { config ->
                    val configData = remember(config.configData) {
                        try {
                            json.decodeFromString<GameConfigData>(config.configData)
                        } catch (e: Exception) { null }
                    }

                    ListItem(
                        headlineContent = { Text(config.gameName) },
                        supportingContent = {
                            val fieldsCount = configData?.fields?.size ?: 0
                            Text("Fields: $fieldsCount | Version: ${config.configVersion}")
                        },
                        modifier = Modifier.clickable { selectedConfig = config }
                    )
                    HorizontalDivider()
                }
                if (configs.isEmpty()) {
                    item {
                        Text(
                            "No configurations found.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    selectedConfig?.let { config ->
        val configData = remember(config) {
            try {
                json.decodeFromString<GameConfigData>(config.configData)
            } catch (e: Exception) { null }
        }

        var showRaw by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedConfig = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
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
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (showRaw || configData == null) {
                        val prettyJson = remember(config.configData) {
                            try {
                                val obj = json.decodeFromString<GameConfigData>(config.configData)
                                json.encodeToString(obj)
                            } catch (e: Exception) { config.configData }
                        }
                        Text(
                            text = prettyJson,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        configData.fields.forEach { field ->
                            ListItem(
                                headlineContent = { Text(field.label) },
                                supportingContent = { Text("Key: ${field.key} | Type: ${field.type}") }
                            )
                        }
                        if (configData.formula != null) {
                            Text(
                                "Formula: ${configData.formula}",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedConfig = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                IconButton(
                    onClick = {
                        scope.launch {
                            database.scoreDao().deleteConfig(config)
                            selectedConfig = null
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Config",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}
