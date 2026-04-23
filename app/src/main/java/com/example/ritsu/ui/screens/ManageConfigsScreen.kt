package com.example.ritsu.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import coil3.compose.AsyncImage
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

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
    var showAppPicker by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
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
                            val fieldsCount = configData?.fields?.size ?: 0
                            Text("Fields: $fieldsCount | Version: ${config.configVersion}")
                        },
                        modifier = Modifier.clickable { selectedConfigId = config.id }
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
            onDismissRequest = { selectedConfigId = null },
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
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Pick from Gallery")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { showAppPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Pick from Apps")
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                TextButton(onClick = { selectedConfigId = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                IconButton(
                    onClick = {
                        scope.launch {
                            database.scoreDao().deleteConfig(config)
                            selectedConfigId = null
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

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { appInfo ->
                scope.launch {
                    selectedConfig?.let { config ->
                        val icon = appInfo.loadIcon(context.packageManager)
                        val file = saveDrawableToFile(context, icon, "config_${config.id}.png")
                        val updated = config.copy(
                            displayIconUri = file.toURI().toString() + "?t=${System.currentTimeMillis()}"
                        )
                        database.scoreDao().updateConfig(updated)
                    }
                }
                showAppPicker = false
            }
        )
    }
}

@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (ApplicationInfo) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val apps = remember {
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { it.loadLabel(packageManager).toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(apps) { app ->
                    ListItem(
                        leadingContent = {
                            val icon = remember { app.loadIcon(packageManager) }
                            Box(modifier = Modifier.size(32.dp)) {
                                AsyncImage(model = icon, contentDescription = null)
                            }
                        },
                        headlineContent = { Text(app.loadLabel(packageManager).toString()) },
                        modifier = Modifier.clickable { onAppSelected(app) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private suspend fun saveUriToFile(context: android.content.Context, uri: Uri, fileName: String): File = withContext(Dispatchers.IO) {
    val file = File(context.filesDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    file
}

private suspend fun saveDrawableToFile(context: android.content.Context, drawable: Drawable, fileName: String): File = withContext(Dispatchers.IO) {
    val bitmap = if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else {
        val b = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(b)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        b
    }
    val file = File(context.filesDir, fileName)
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    file
}

