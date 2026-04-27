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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ritsu.data.ConfigManager
import com.example.ritsu.data.DataManager
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.RitsuDatabase

@Composable
fun OptionsScreen(
    onManageConfigsClick: () -> Unit,
    onDebugClick: () -> Unit,
    onThemeClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    val scores by database.scoreDao().getAllScores().collectAsState(initial = emptyList())
    
    var showConfigExportDialog by remember { mutableStateOf(value = false) }
    var showDataExportDialog by remember { mutableStateOf(false) }
    var selectedConfigToExport by remember { mutableStateOf<GameConfig?>(null) }

    // --- Configuration Import/Export Launchers ---
    val configImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> ConfigManager.handleImport(context, uri) }

    val configExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let {
            selectedConfigToExport?.let { config ->
                ConfigManager.handleExport(context, it, config.configData)
            }
        }
    }

    // --- Data Import/Export Launchers ---
    val dataImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> DataManager.handleImport(context, uri) }

    val dataJsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { DataManager.exportToJson(context, it, configs, scores) } }

    val dataCsvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let { DataManager.exportToCsv(context, it, configs, scores) } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "Configurations", 
                style = MaterialTheme.typography.labelLarge, 
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Import Config") },
                    leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    modifier = Modifier.weight(1f).clickable { configImportLauncher.launch(arrayOf("application/json")) }
                )
                ListItem(
                    headlineContent = { Text("Export Config") },
                    leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    modifier = Modifier.weight(1f).clickable {
                        if (configs.isNotEmpty()) showConfigExportDialog = true
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

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        
        item {
            Text(
                "Play Data", 
                style = MaterialTheme.typography.labelLarge, 
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Import Data") },
                    leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    modifier = Modifier.weight(1f).clickable { dataImportLauncher.launch(arrayOf("application/json")) }
                )
                ListItem(
                    headlineContent = { Text("Export Data") },
                    leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    modifier = Modifier.weight(1f).clickable {
                        if (scores.isNotEmpty()) showDataExportDialog = true
                    }
                )
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item {
            ListItem(
                headlineContent = { Text("Theme") },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                modifier = Modifier.clickable { onThemeClick() }
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

    // --- Format Selection Dialogs ---
    if (showConfigExportDialog) {
        AlertDialog(
            onDismissRequest = { showConfigExportDialog = false },
            title = { Text("Select Config to Export") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(configs.size) { index ->
                        val config = configs[index]
                        ListItem(
                            headlineContent = { Text(config.gameName) },
                            supportingContent = { Text("Version: ${config.configVersion}") },
                            modifier = Modifier.clickable {
                                selectedConfigToExport = config
                                configExportLauncher.launch("${config.gameName.replace(" ", "_")}.json")
                                showConfigExportDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showConfigExportDialog = false }) { Text("Cancel") } },
        )
    }

    if (showDataExportDialog) {
        AlertDialog(
            onDismissRequest = { showDataExportDialog = false },
            title = { Text("Select Data Format") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("JSON (App Importable)") },
                        supportingContent = { Text("Best for backups and migrating to another device.") },
                        modifier = Modifier.clickable {
                            dataJsonExportLauncher.launch("ritsu_data_${System.currentTimeMillis()}.json")
                            showDataExportDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("CSV (Human Readable)") },
                        supportingContent = { Text("Best for viewing in Excel or Google Sheets.") },
                        modifier = Modifier.clickable {
                            dataCsvExportLauncher.launch("ritsu_data_${System.currentTimeMillis()}.csv")
                            showDataExportDialog = false
                        }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showDataExportDialog = false }) { Text("Cancel") } },
        )
    }
}
