package com.example.ritsu.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.RitsuDatabase
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ManageConfigsScreen() {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())

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
                    }
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
