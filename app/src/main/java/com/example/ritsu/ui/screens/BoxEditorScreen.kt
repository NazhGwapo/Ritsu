package com.example.ritsu.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.screens.editor.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class EditorMode {
    EDIT, VIEW, EYEDROP
}

@Composable
fun BoxEditorScreen() {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true; prettyPrint = true } }
    
    val editorViewModel: EditorViewModel = viewModel()

    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    var selectedConfig by remember { mutableStateOf<GameConfig?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showFieldEditor by remember { mutableStateOf(value = false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val configData by editorViewModel.configData.collectAsState()
    val selectedRectKey by editorViewModel.selectedKey.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> imageUri = uri }

    LaunchedEffect(selectedConfig) {
        val data = selectedConfig?.let {
            try {
                json.decodeFromString<GameConfigData>(it.configData)
            } catch (_: Exception) {
                null
            }
        }
        editorViewModel.setConfigData(data)
        editorViewModel.setSelectedKey(null)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EditorTopSection(
                configs = configs,
                selectedConfig = selectedConfig,
                onConfigSelected = { selectedConfig = it },
                onImportImage = {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onSave = {
                    selectedConfig?.let { config ->
                        configData?.let { data ->
                            scope.launch {
                                val updatedConfig = config.copy(
                                    configData = json.encodeToString(data),
                                    configVersion = data.configVersion,
                                )
                                database.scoreDao().updateConfig(updatedConfig)
                                snackbarHostState.showSnackbar("Configuration saved")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (configData != null) {
                BottomSelector(
                    viewModel = editorViewModel,
                    onEditField = { showFieldEditor = true },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if ((imageUri != null) && (configData != null)) {
                EditorCanvas(
                    imageUri = imageUri!!,
                    viewModel = editorViewModel,
                    snackbarHostState = snackbarHostState
                )

                if (showFieldEditor && (selectedRectKey != null)) {
                    FieldEditorDialog(
                        configData = configData!!,
                        selectedKey = selectedRectKey!!,
                        onDismiss = { showFieldEditor = false },
                        onConfigDataChanged = { 
                            editorViewModel.updateConfigData(it)
                            showFieldEditor = false 
                        },
                        onDeleteField = {
                            editorViewModel.removeField(selectedRectKey!!)
                            showFieldEditor = false
                        }
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (selectedConfig == null) "Select a configuration first"
                        else if (imageUri == null) "Import a reference image from gallery"
                        else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if ((selectedConfig != null) && (imageUri == null)) {
                        Button(
                            onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Import Image")
                        }
                    }
                }
            }
        }
    }
}
