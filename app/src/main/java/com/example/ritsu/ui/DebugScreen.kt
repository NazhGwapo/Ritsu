package com.example.ritsu.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.OCRManager
import com.example.ritsu.data.OcrRect
import com.example.ritsu.data.RitsuDatabase
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun DebugScreen() {
    var showOcrDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ListItem(
                headlineContent = { Text("OCR test") },
                modifier = Modifier.clickable { showOcrDialog = true }
            )
        }
    }

    if (showOcrDialog) {
        OcrTestDialog(onDismiss = { showOcrDialog = false })
    }
}

@Composable
fun OcrTestDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val configs by database.scoreDao().getAllConfigs().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(false) }
    var selectedConfig by remember { mutableStateOf<GameConfig?>(null) }
    var selectedConfigData by remember { mutableStateOf<GameConfigData?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullTextResult by remember { mutableStateOf<Text?>(null) }
    var ocrResults by remember { mutableStateOf<Map<String, String>?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val config = selectedConfig ?: return@let
            scope.launch {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, it)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    }
                    selectedBitmap = bitmap

                    val json = Json { ignoreUnknownKeys = true }
                    val gameConfigData = json.decodeFromString<GameConfigData>(config.configData)
                    selectedConfigData = gameConfigData
                    
                    val ocrManager = OCRManager()
                    val textResult = ocrManager.recognizeText(bitmap)
                    fullTextResult = textResult
                    
                    if (textResult != null) {
                        val results = ocrManager.processImage(bitmap, gameConfigData)
                        ocrResults = results
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "OCR Test", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.padding(8.dp))

                Box {
                    Row(
                        modifier = Modifier
                            .clickable { expanded = true }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedConfig?.gameName ?: "Select Config")
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
                                    selectedConfig = config
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(8.dp))

                Button(
                    onClick = {
                        if (selectedConfig != null) {
                            pickerLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedConfig != null
                ) {
                    Text("Import from Gallery")
                }

                selectedBitmap?.let { bitmap ->
                    Spacer(modifier = Modifier.padding(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Draw bounding boxes
                        selectedConfigData?.let { config ->
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasSize = size
                                val imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                                
                                val scale = minOf(canvasSize.width / imageSize.width, canvasSize.height / imageSize.height)
                                val offsetX = (canvasSize.width - imageSize.width * scale) / 2
                                val offsetY = (canvasSize.height - imageSize.height * scale) / 2

                                val drawRect: (OcrRect, Color) -> Unit = { rect, color ->
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(
                                            x = offsetX + rect.x * imageSize.width * scale,
                                            y = offsetY + rect.y * imageSize.height * scale
                                        ),
                                        size = Size(
                                            width = rect.w * imageSize.width * scale,
                                            height = rect.h * imageSize.height * scale
                                        ),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }

                                config.titleRect?.let { drawRect(it, Color.Red) }
                                config.scoreRect?.let { drawRect(it, Color.Green) }
                                config.comboRect?.let { drawRect(it, Color.Blue) }
                                config.difficultyNameRect?.let { drawRect(it, Color.Yellow) }
                                config.difficultyValRect?.let { drawRect(it, Color.Cyan) }
                                config.rankRect?.let { drawRect(it, Color.Magenta) }
                                config.fields.forEach { field ->
                                    field.ocrRect?.let { drawRect(it, Color.White) }
                                }

                                // Draw all detected OCR elements in light gray to see what we missed
                                fullTextResult?.textBlocks?.forEach { block ->
                                    block.lines.forEach { line ->
                                        line.elements.forEach { element ->
                                            val box = element.boundingBox ?: return@forEach
                                            
                                            drawRect(
                                                color = Color.Gray.copy(alpha = 0.3f),
                                                topLeft = Offset(
                                                    x = offsetX + (box.left.toFloat() / bitmap.width) * imageSize.width * scale,
                                                    y = offsetY + (box.top.toFloat() / bitmap.height) * imageSize.height * scale
                                                ),
                                                size = Size(
                                                    width = (box.width().toFloat() / bitmap.width) * imageSize.width * scale,
                                                    height = (box.height().toFloat() / bitmap.height) * imageSize.height * scale
                                                ),
                                                style = Stroke(width = 1.dp.toPx())
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ocrResults?.let { results ->
                    Spacer(modifier = Modifier.padding(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        results.forEach { (key, value) ->
                            Text(
                                text = "$key: $value",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
