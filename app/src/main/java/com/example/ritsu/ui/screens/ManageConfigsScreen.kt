package com.example.ritsu.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ritsu.ui.utils.AsyncImage
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.components.AppPickerDialog
import com.example.ritsu.ui.components.ConfigDetailDialog
import com.example.ritsu.ui.components.NewConfigDialog
import com.example.ritsu.ui.utils.saveDrawableToFile
import com.example.ritsu.ui.utils.saveUriToFile
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun ManageConfigsScreen(onEditConfig: () -> Unit) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true; prettyPrint = true } }
    
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    var selectedConfigId by remember { mutableStateOf<Long?>(null) }
    val selectedConfig = remember(selectedConfigId, configs) {
        configs.find { it.id == selectedConfigId }
    }
    var showAppPicker by remember { mutableStateOf(value = false) }
    var showNewConfigDialog by remember { mutableStateOf(value = false) }
    var showReminder by remember { mutableStateOf(value = false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                selectedConfig?.let { config ->
                    val file = saveUriToFile(context, it, "config_${config.id}.png")
                    val updated = config.copy(displayIconUri = file.toURI().toString() + "?t=${System.currentTimeMillis()}")
                    database.scoreDao().updateConfig(updated)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(configs) { config ->
                    val configData = remember(config.configData) {
                        try {
                            json.decodeFromString<GameConfigData>(config.configData)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)),
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
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        },
                        headlineContent = { Text(config.gameName) },
                        supportingContent = {
                            val fieldsCount = configData?.allFieldsWithCategory?.size ?: 0
                            Text("Fields: $fieldsCount | Version: ${config.configVersion}")
                        },
                        modifier = Modifier.clickable { selectedConfigId = config.id }
                    )
                    HorizontalDivider()
                }

                item {
                    Button(
                        onClick = { showNewConfigDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Make a new config")
                    }
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

        FloatingActionButton(
            onClick = onEditConfig,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Configs")
        }
    }

    selectedConfig?.let { config ->
        ConfigDetailDialog(
            config = config,
            onDismiss = { selectedConfigId = null },
            onDelete = {
                scope.launch {
                    database.scoreDao().deleteConfig(config)
                    selectedConfigId = null
                }
            },
            onPickFromGallery = { galleryLauncher.launch("image/*") },
            onPickFromApps = { showAppPicker = true }
        ) { updated ->
            scope.launch {
                database.scoreDao().updateConfig(updated)
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { appInfo ->
                scope.launch {
                    selectedConfig?.let { config ->
                        val icon = appInfo.loadIcon(context.packageManager)
                        val file = saveDrawableToFile(context, icon, "config_${config.id}.png")
                        val updated = config.copy(
                            displayIconUri = file.toURI().toString() + "?t=${System.currentTimeMillis()}",
                        )
                        database.scoreDao().updateConfig(updated)
                    }
                }
                showAppPicker = false
            },
        )
    }

    if (showNewConfigDialog) {
        NewConfigDialog(
            onDismiss = { showNewConfigDialog = false },
            onConfirm = { name, acc, rank ->
                scope.launch {
                    val newConfigData = GameConfigData(
                        gameName = name,
                        useAccuracyOcr = acc,
                        useRankOcr = rank
                    )
                    val config = GameConfig(
                        gameName = name,
                        configData = json.encodeToString(newConfigData)
                    )
                    database.scoreDao().insertConfig(config)
                    showNewConfigDialog = false
                    showReminder = true
                }
            }
        )
    }

    if (showReminder) {
        AlertDialog(
            onDismissRequest = { showReminder = false },
            title = { Text("Config Created") },
            text = { Text("Your new configuration has been created. To actually map the fields and bounding boxes, press the Edit button in the lower right corner.") },
            confirmButton = {
                TextButton(onClick = { showReminder = false }) {
                    Text("OK")
                }
            }
        )
    }
}
