package com.example.ritsu.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ritsu.data.ConfigManager
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.RitsuDatabase

@Composable
fun OptionsScreen(onManageConfigsClick: () -> Unit, onDebugClick: () -> Unit) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    var showExportDialog by remember { mutableStateOf(value = false) }
    var selectedConfigToExport by remember { mutableStateOf<GameConfig?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        ConfigManager.handleImport(context, uri)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let {
            selectedConfigToExport?.let { config ->
                ConfigManager.handleExport(context, it, config.configData)
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Import Configuration") },
                    leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                )
                ListItem(
                    headlineContent = { Text("Export Configuration") },
                    leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (configs.isEmpty()) {
                                // Show message that no configs are available
                            } else {
                                showExportDialog = true
                            }
                        }
                )
            }
        }
        item {
            ListItem(
                headlineContent = { Text("Manage Configurations") },
                leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                modifier = Modifier.clickable { onManageConfigsClick() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Delete Data") },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                modifier = Modifier.clickable { /* No functionality */ }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Help") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                modifier = Modifier.clickable { /* No functionality */ }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Debug") },
                leadingContent = { Icon(Icons.Default.BugReport, contentDescription = null) },
                modifier = Modifier.clickable { onDebugClick() }
            )
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Select Configuration to Export") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(configs.size) { index ->
                        val config = configs[index]
                        ListItem(
                            headlineContent = { Text(config.gameName) },
                            supportingContent = { Text("Version: ${config.configVersion}") },
                            modifier = Modifier.clickable {
                                selectedConfigToExport = config
                                exportLauncher.launch("${config.gameName.replace(" ", "_")}.json")
                                showExportDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
