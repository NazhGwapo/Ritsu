package com.example.ritsu.ui

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.OcrRect
import com.example.ritsu.data.RitsuDatabase
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class EditorMode {
    EDIT, VIEW
}

@Composable
fun BoxEditorScreen() {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true; prettyPrint = true } }

    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    var selectedConfig by remember { mutableStateOf<GameConfig?>(null) }
    var configData by remember { mutableStateOf<GameConfigData?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedRectKey by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
    )

    LaunchedEffect(selectedConfig) {
        configData = selectedConfig?.let {
            try {
                json.decodeFromString<GameConfigData>(it.configData)
            } catch (e: Exception) {
                null
            }
        }
        selectedRectKey = null
    }

    Scaffold(
        topBar = {
            TopSection(
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
                                    configData = json.encodeToString(data)
                                )
                                database.scoreDao().updateConfig(updatedConfig)
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            configData?.let { data ->
                BottomSelector(
                    data = data,
                    selectedKey = selectedRectKey,
                    onKeySelected = { selectedRectKey = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null && configData != null) {
                EditorCanvas(
                    imageUri = imageUri!!,
                    configData = configData!!,
                    selectedKey = selectedRectKey,
                    onConfigDataChanged = { configData = it }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (selectedConfig == null) "Select a configuration first"
                        else if (imageUri == null) "Import a reference image from gallery"
                        else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedConfig != null && imageUri == null) {
                        Button(
                            onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Import Image")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopSection(
    configs: List<GameConfig>,
    selectedConfig: GameConfig?,
    onConfigSelected: (GameConfig) -> Unit,
    onImportImage: () -> Unit,
    onSave: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedConfig?.gameName ?: "Select Config",
                        maxLines = 1
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
                                onConfigSelected(config)
                                expanded = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = onImportImage) {
                Icon(Icons.Default.Image, contentDescription = "Import Image")
            }

            IconButton(onClick = onSave, enabled = selectedConfig != null) {
                Icon(Icons.Default.Save, contentDescription = "Save Config")
            }
        }
    }
}

@Composable
fun BottomSelector(
    data: GameConfigData,
    selectedKey: String?,
    onKeySelected: (String) -> Unit
) {
    val keys = remember(data) {
        mutableListOf<Pair<String, String>>().apply {
            add("titleRect" to "Title")
            add("scoreRect" to "Score")
            add("comboRect" to "Combo")
            add("difficultyNameRect" to "Diff Name")
            add("difficultyValRect" to "Diff Val")
            add("rankRect" to "Rank")
            data.fields.forEachIndexed { index, field ->
                add("field_$index" to field.label)
            }
        }
    }

    Surface(tonalElevation = 2.dp) {
        Column {
            Text(
                "Select field to edit:",
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelMedium
            )
            ScrollableTabRow(
                selectedTabIndex = keys.indexOfFirst { it.first == selectedKey }.coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                keys.forEach { (key, label) ->
                    Tab(
                        selected = selectedKey == key,
                        onClick = { onKeySelected(key) },
                        text = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
fun EditorCanvas(
    imageUri: Uri,
    configData: GameConfigData,
    selectedKey: String?,
    onConfigDataChanged: (GameConfigData) -> Unit
) {
    val context = LocalContext.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var mode by remember { mutableStateOf(EditorMode.EDIT) }

    val bitmap = remember(imageUri) {
        try {
            val contentResolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { containerSize = it.size }
            .pointerInput(mode) {
                if (mode == EditorMode.VIEW) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { btm ->
            val imageBitmap = remember(btm) { btm.asImageBitmap() }
            val density = LocalDensity.current
            val srcSize = Size(btm.width.toFloat(), btm.height.toFloat())
            val dstSize = containerSize.toSize()
            
            if (dstSize.width > 0 && dstSize.height > 0) {
                val scaleFactor = ContentScale.Fit.computeScaleFactor(srcSize, dstSize)
                val finalImageSize = Size(
                    width = srcSize.width * scaleFactor.scaleX,
                    height = srcSize.height * scaleFactor.scaleY
                )

                Box(
                    modifier = Modifier
                        .size(
                            width = with(density) { finalImageSize.width.toDp() },
                            height = with(density) { finalImageSize.height.toDp() }
                        )
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    val rects = remember(configData) {
                        mutableListOf<Triple<String, String, OcrRect?>>().apply {
                            add(Triple("titleRect", "Title", configData.titleRect))
                            add(Triple("scoreRect", "Score", configData.scoreRect))
                            add(Triple("comboRect", "Combo", configData.comboRect))
                            add(Triple("difficultyNameRect", "Diff Name", configData.difficultyNameRect))
                            add(Triple("difficultyValRect", "Diff Val", configData.difficultyValRect))
                            add(Triple("rankRect", "Rank", configData.rankRect))
                            configData.fields.forEachIndexed { index, field ->
                                add(Triple("field_$index", field.label, field.ocrRect))
                            }
                        }
                    }

                    rects.forEach { (key, label, rect) ->
                        if (rect != null) {
                            BoxOverlay(
                                rect = rect,
                                isSelected = selectedKey == key && mode == EditorMode.EDIT,
                                onRectChanged = { newRect ->
                                    onConfigDataChanged(configData.updateRect(key, newRect))
                                },
                                label = label
                            )
                        } else if (selectedKey == key && mode == EditorMode.EDIT) {
                            LaunchedEffect(key) {
                                onConfigDataChanged(configData.updateRect(key, OcrRect(0.1f, 0.1f, 0.2f, 0.05f)))
                            }
                        }
                    }
                }
            }
        }

        // Mode and Zoom Controls
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { mode = EditorMode.EDIT }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Mode",
                        tint = if (mode == EditorMode.EDIT) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                IconButton(onClick = { mode = EditorMode.VIEW }) {
                    Icon(
                        Icons.Default.PanTool,
                        contentDescription = "View Mode",
                        tint = if (mode == EditorMode.VIEW) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }

                HorizontalDivider(modifier = Modifier.width(32.dp))

                IconButton(onClick = { scale = (scale * 1.2f).coerceAtMost(5f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                }
                IconButton(onClick = { scale = (scale / 1.2f).coerceAtLeast(0.5f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                }
                IconButton(onClick = {
                    scale = 1f
                    offset = Offset.Zero
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom")
                }
            }
        }
    }
}

@Composable
fun BoxOverlay(
    rect: OcrRect,
    isSelected: Boolean,
    onRectChanged: (OcrRect) -> Unit,
    label: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pxWidth = constraints.maxWidth.toFloat()
        val pxHeight = constraints.maxHeight.toFloat()
        
        val currentRect by rememberUpdatedState(rect)
        val currentOnRectChanged by rememberUpdatedState(onRectChanged)

        Box(
            modifier = Modifier
                .offset(
                    x = maxWidth * rect.x,
                    y = maxHeight * rect.y
                )
                .size(
                    width = maxWidth * rect.w,
                    height = maxHeight * rect.h
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color.Red else Color.Blue.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(2.dp)
                )
                .then(
                    if (isSelected) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                currentOnRectChanged(
                                    currentRect.copy(
                                        x = (currentRect.x + dragAmount.x / pxWidth).coerceIn(0f, 1f - currentRect.w),
                                        y = (currentRect.y + dragAmount.y / pxHeight).coerceIn(0f, 1f - currentRect.h)
                                    )
                                )
                            }
                        }
                    } else Modifier
                )
        ) {
            if (isSelected) {
                // Resize handle at bottom right
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                currentOnRectChanged(
                                    currentRect.copy(
                                        w = (currentRect.w + dragAmount.x / pxWidth).coerceIn(0.01f, 1f - currentRect.x),
                                        h = (currentRect.h + dragAmount.y / pxHeight).coerceIn(0.01f, 1f - currentRect.y)
                                    )
                                )
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color.Red, RoundedCornerShape(topStart = 4.dp))
                    )
                }
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.Red else Color.Blue,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// Helpers
fun GameConfigData.updateRect(key: String, rect: OcrRect?): GameConfigData {
    return when {
        key == "titleRect" -> copy(titleRect = rect)
        key == "scoreRect" -> copy(scoreRect = rect)
        key == "comboRect" -> copy(comboRect = rect)
        key == "difficultyNameRect" -> copy(difficultyNameRect = rect)
        key == "difficultyValRect" -> copy(difficultyValRect = rect)
        key == "rankRect" -> copy(rankRect = rect)
        key.startsWith("field_") -> {
            val index = key.substringAfter("field_").toIntOrNull()
            if (index != null && index in fields.indices) {
                val newFields = fields.toMutableList()
                newFields[index] = newFields[index].copy(ocrRect = rect)
                copy(fields = newFields)
            } else this
        }
        else -> this
    }
}
