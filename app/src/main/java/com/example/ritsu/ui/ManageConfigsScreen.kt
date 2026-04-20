package com.example.ritsu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.RitsuDatabase
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ManageConfigsScreen() {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    var selectedConfig by remember { mutableStateOf<GameConfig?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(configs) { config ->
                ListItem(
                    headlineContent = { Text(config.gameName) },
                    supportingContent = {
                        val fieldsCount = try {
                            val json = JSONObject(config.configData)
                            json.getJSONArray("fields").length()
                        } catch (e: Exception) { 0 }
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

    selectedConfig?.let { config ->
        AlertDialog(
            onDismissRequest = { selectedConfig = null },
            title = { Text(config.gameName) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = config.configData,
                        style = MaterialTheme.typography.bodySmall
                    )
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
