package com.example.ritsu.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.OcrRect
import com.example.ritsu.ui.screens.EditorMode
import kotlinx.coroutines.launch
import kotlin.math.abs

data class FieldInfo(
    val key: String,
    val label: String,
    val rect: OcrRect?,
    val field: com.example.ritsu.data.ConfigField? = null
)

enum class HandleType {
    NONE, TOP_LEFT, TOP, TOP_RIGHT, LEFT, RIGHT, BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT, MOVE
}

@Composable
fun EditorCanvas(
    imageUri: Uri,
    viewModel: EditorViewModel,
    snackbarHostState: SnackbarHostState? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val configData by viewModel.configData.collectAsState()
    val selectedKey by viewModel.selectedKey.collectAsState()
    val mode by viewModel.mode.collectAsState()

    // Load Bitmap
    LaunchedEffect(imageUri) {
        viewModel.bitmap = try {
            val contentResolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                val btm = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                btm.copy(Bitmap.Config.ARGB_8888, true)
            }
        } catch (e: Exception) {
            null
        }
    }

    val bitmap = viewModel.bitmap
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    var toolbarRect by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { 
                viewModel.containerSize = it.size.toSize()
            }
            .pointerInput(viewModel.containerSize, bitmap, mode, selectedKey, toolbarRect) {
                if (bitmap == null) return@pointerInput
                
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var currentStartPos = down.position
                    
                    if (toolbarRect.contains(currentStartPos)) {
                        // Ignore gestures starting on the toolbar
                        return@awaitEachGesture
                    }
                    
                    if (mode == EditorMode.VIEW) {
                        // Navigation handled by detectTransformGestures
                    } else {
                        val normalizedPos = viewModel.screenToNormalized(currentStartPos)
                        val selectedField = configData?.let { data ->
                            rectsFromData(data).find { it.key == selectedKey }
                        }
                        
                        var activeHandle = HandleType.NONE
                        if (mode == EditorMode.EDIT && selectedField?.rect != null) {
                            val r = selectedField.rect
                            val left = viewModel.normalizedToScreen(Offset(r.x, r.y))
                            val right = viewModel.normalizedToScreen(Offset(r.x + r.w, r.y + r.h))
                            
                            val hitRadius = 40f
                            
                            activeHandle = when {
                                (currentStartPos - left).getDistance() < hitRadius -> HandleType.TOP_LEFT
                                (currentStartPos - Offset(right.x, left.y)).getDistance() < hitRadius -> HandleType.TOP_RIGHT
                                (currentStartPos - Offset(left.x, right.y)).getDistance() < hitRadius -> HandleType.BOTTOM_LEFT
                                (currentStartPos - right).getDistance() < hitRadius -> HandleType.BOTTOM_RIGHT
                                abs(currentStartPos.y - left.y) < hitRadius && currentStartPos.x in left.x..right.x -> HandleType.TOP
                                abs(currentStartPos.y - right.y) < hitRadius && currentStartPos.x in left.x..right.x -> HandleType.BOTTOM
                                abs(currentStartPos.x - left.x) < hitRadius && currentStartPos.y in left.y..right.y -> HandleType.LEFT
                                abs(currentStartPos.x - right.x) < hitRadius && currentStartPos.y in left.y..right.y -> HandleType.RIGHT
                                currentStartPos.x in left.x..right.x && currentStartPos.y in left.y..right.y -> HandleType.MOVE
                                else -> HandleType.NONE
                            }
                        }

                        if (mode == EditorMode.EYEDROP) {
                            val pixelX = (normalizedPos.x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                            val pixelY = (normalizedPos.y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                            val color = bitmap.getPixel(pixelX, pixelY)
                            selectedKey?.let { key ->
                                val fieldInfo = rectsFromData(configData!!).find { it.key == key }
                                if (fieldInfo?.field != null) {
                                    viewModel.updateField(
                                        key.substringBefore("_").replaceFirstChar { it.uppercase() },
                                        key.substringAfter("_").toInt(),
                                        fieldInfo.field.copy(targetColor = color)
                                    )
                                    scope.launch {
                                        snackbarHostState?.showSnackbar("Sampled color: #${Integer.toHexString(color).uppercase()}")
                                    }
                                }
                            }
                        } else if (activeHandle != HandleType.NONE) {
                            val startRect = selectedField!!.rect!!
                            while (true) {
                                val event = awaitPointerEvent()
                                val dragChange = event.changes.firstOrNull() ?: break
                                if (dragChange.pressed) {
                                    dragChange.consume()
                                    val currentNorm = viewModel.screenToNormalized(dragChange.position)
                                    val startNorm = viewModel.screenToNormalized(currentStartPos)
                                    val diff = currentNorm - startNorm
                                    
                                    var newX = startRect.x
                                    var newY = startRect.y
                                    var newW = startRect.w
                                    var newH = startRect.h
                                    
                                    when (activeHandle) {
                                        HandleType.TOP_LEFT -> {
                                            newX = (startRect.x + diff.x).coerceIn(0f, startRect.x + startRect.w - 0.005f)
                                            newY = (startRect.y + diff.y).coerceIn(0f, startRect.y + startRect.h - 0.005f)
                                            newW = startRect.w - (newX - startRect.x)
                                            newH = startRect.h - (newY - startRect.y)
                                        }
                                        HandleType.TOP -> {
                                            newY = (startRect.y + diff.y).coerceIn(0f, startRect.y + startRect.h - 0.005f)
                                            newH = startRect.h - (newY - startRect.y)
                                        }
                                        HandleType.TOP_RIGHT -> {
                                            newY = (startRect.y + diff.y).coerceIn(0f, startRect.y + startRect.h - 0.005f)
                                            newH = startRect.h - (newY - startRect.y)
                                            newW = (startRect.w + diff.x).coerceAtLeast(0.005f)
                                        }
                                        HandleType.LEFT -> {
                                            newX = (startRect.x + diff.x).coerceIn(0f, startRect.x + startRect.w - 0.005f)
                                            newW = startRect.w - (newX - startRect.x)
                                        }
                                        HandleType.RIGHT -> {
                                            newW = (startRect.w + diff.x).coerceAtLeast(0.005f)
                                        }
                                        HandleType.BOTTOM_LEFT -> {
                                            newX = (startRect.x + diff.x).coerceIn(0f, startRect.x + startRect.w - 0.005f)
                                            newW = startRect.w - (newX - startRect.x)
                                            newH = (startRect.h + diff.y).coerceAtLeast(0.005f)
                                        }
                                        HandleType.BOTTOM -> {
                                            newH = (startRect.h + diff.y).coerceAtLeast(0.005f)
                                        }
                                        HandleType.BOTTOM_RIGHT -> {
                                            newW = (startRect.w + diff.x).coerceAtLeast(0.005f)
                                            newH = (startRect.h + diff.y).coerceAtLeast(0.005f)
                                        }
                                        HandleType.MOVE -> {
                                            newX = (startRect.x + diff.x).coerceIn(0f, 1f - startRect.w)
                                            newY = (startRect.y + diff.y).coerceIn(0f, 1f - startRect.h)
                                        }
                                        else -> {}
                                    }
                                    viewModel.updateRect(selectedKey!!, OcrRect(newX, newY, newW, newH))
                                } else break
                            }
                        } else if (selectedKey != null) {
                            // Draw new
                            while (true) {
                                val event = awaitPointerEvent()
                                val dragChange = event.changes.firstOrNull() ?: break
                                if (dragChange.pressed) {
                                    dragChange.consume()
                                    val endNorm = viewModel.screenToNormalized(dragChange.position)
                                    val left = minOf(normalizedPos.x, endNorm.x)
                                    val top = minOf(normalizedPos.y, endNorm.y)
                                    val w = abs(normalizedPos.x - endNorm.x)
                                    val h = abs(normalizedPos.y - endNorm.y)
                                    
                                    val newRect = OcrRect(left, top, w, h)
                                    if (selectedField?.field?.type == "boolean") {
                                        val cX = (left + w/2) * bitmap.width
                                        val cY = (top + h/2) * bitmap.height
                                        val color = bitmap.getPixel(cX.toInt().coerceIn(0, bitmap.width-1), cY.toInt().coerceIn(0, bitmap.height-1))
                                        viewModel.updateRect(selectedKey!!, newRect, color)
                                    } else {
                                        viewModel.updateRect(selectedKey!!, newRect)
                                    }
                                } else {
                                    if ((dragChange.position - currentStartPos).getDistance() < 10f && selectedField?.field?.type == "boolean") {
                                        val color = bitmap.getPixel((normalizedPos.x * bitmap.width).toInt().coerceIn(0, bitmap.width-1), (normalizedPos.y * bitmap.height).toInt().coerceIn(0, bitmap.height-1))
                                        viewModel.updateRect(selectedKey!!, OcrRect(normalizedPos.x - 0.005f, normalizedPos.y - 0.005f, 0.01f, 0.01f), color)
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (viewModel.mode.value == EditorMode.VIEW) {
                        val oldScale = viewModel.scale
                        val newScale = (oldScale * zoom).coerceIn(1f, 20f)
                        viewModel.offset = (viewModel.offset - centroid) * (newScale / oldScale) + centroid + pan
                        viewModel.scale = newScale
                    }
                }
            }
    ) {
        if (imageBitmap != null) {
            val srcSize = Size(bitmap!!.width.toFloat(), bitmap.height.toFloat())
            val dstSize = viewModel.containerSize
            val scaleFactor = ContentScale.Fit.computeScaleFactor(srcSize, dstSize)
            viewModel.imageSize = Size(srcSize.width * scaleFactor.scaleX, srcSize.height * scaleFactor.scaleY)
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                withTransform({
                    translate(viewModel.offset.x, viewModel.offset.y)
                    scale(viewModel.scale, viewModel.scale, pivot = Offset.Zero)
                }) {
                    val dx = (dstSize.width - viewModel.imageSize.width) / 2
                    val dy = (dstSize.height - viewModel.imageSize.height) / 2
                    
                    drawImage(
                        image = imageBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(dx.toInt(), dy.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(viewModel.imageSize.width.toInt(), viewModel.imageSize.height.toInt())
                    )

                    configData?.let { data ->
                        rectsFromData(data).forEach { info ->
                            val r = info.rect ?: return@forEach
                            val isSelected = info.key == selectedKey
                            
                            val left = dx + r.x * viewModel.imageSize.width
                            val top = dy + r.y * viewModel.imageSize.height
                            val width = r.w * viewModel.imageSize.width
                            val height = r.h * viewModel.imageSize.height

                            // Dual-layer stroke for contrast
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(left, top),
                                size = Size(width, height),
                                style = Stroke(width = 6f / viewModel.scale)
                            )
                            drawRect(
                                color = if (isSelected) Color.Red else Color.Blue.copy(alpha = 0.8f),
                                topLeft = Offset(left, top),
                                size = Size(width, height),
                                style = Stroke(width = 3f / viewModel.scale)
                            )
                            
                            if (info.field?.type == "boolean" && info.field.targetColor != null) {
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(left + 2f/viewModel.scale, top + 2f/viewModel.scale),
                                    size = Size(20f/viewModel.scale, 20f/viewModel.scale)
                                )
                                drawRect(
                                    color = Color(info.field.targetColor),
                                    topLeft = Offset(left + 4f/viewModel.scale, top + 4f/viewModel.scale),
                                    size = Size(16f/viewModel.scale, 16f/viewModel.scale)
                                )
                            }

                            if (isSelected) {
                                val handlePoints = listOf(
                                    Offset(left, top), Offset(left + width/2, top), Offset(left + width, top),
                                    Offset(left, top + height/2), Offset(left + width, top + height/2),
                                    Offset(left, top + height), Offset(left + width/2, top + height), Offset(left + width, top + height)
                                )
                                handlePoints.forEach { p ->
                                    drawCircle(color = Color.White, radius = 10f / viewModel.scale, center = p)
                                    drawCircle(color = Color.Red, radius = 7f / viewModel.scale, center = p)
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .onGloballyPositioned {
                        val pos = it.positionInParent()
                        toolbarRect = Rect(pos, it.size.toSize())
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { /* Block tap */ }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { _, _ -> /* Block drag */ }
                    },
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    ControlBtn(Icons.Default.Edit, "Edit", mode == EditorMode.EDIT) { viewModel.setMode(EditorMode.EDIT) }
                    ControlBtn(Icons.Default.PanTool, "View", mode == EditorMode.VIEW) { viewModel.setMode(EditorMode.VIEW) }
                    
                    val isBool = selectedKey?.let { key ->
                        val info = rectsFromData(configData!!).find { it.key == key }
                        info?.field?.type == "boolean"
                    } ?: false
                    
                    if (isBool) {
                        ControlBtn(Icons.Default.Colorize, "Eyedrop", mode == EditorMode.EYEDROP) { viewModel.setMode(EditorMode.EYEDROP) }
                    }
                    
                    HorizontalDivider(modifier = Modifier.width(32.dp).padding(vertical = 4.dp))
                    
                    ControlBtn(Icons.Default.Refresh, "Reset") {
                        viewModel.scale = 1f
                        viewModel.offset = Offset.Zero
                    }
                }
            }
            
            if (viewModel.scale > 1.1f) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        "Zoom: ${"%.1f".format(viewModel.scale)}x",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ControlBtn(icon: ImageVector, desc: String, active: Boolean = false, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, desc, tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current)
    }
}

private fun rectsFromData(data: GameConfigData): List<FieldInfo> {
    return mutableListOf<FieldInfo>().apply {
        add(FieldInfo("titleRect", "Title", data.titleRect))
        add(FieldInfo("scoreRect", "Score", data.scoreRect))
        add(FieldInfo("comboRect", "Combo", data.comboRect))
        add(FieldInfo("accuracyRect", "Accuracy", data.accuracyRect))
        add(FieldInfo("difficultyNameRect", "Diff Name", data.difficultyNameRect))
        add(FieldInfo("difficultyValRect", "Diff Val", data.difficultyValRect))
        add(FieldInfo("rankRect", "Rank", data.rankRect))
        data.judgments.forEachIndexed { i, f -> add(FieldInfo("judgment_$i", f.label, f.ocrRect, f)) }
        data.metrics.forEachIndexed { i, f -> add(FieldInfo("metric_$i", f.label, f.ocrRect, f)) }
        data.misc.forEachIndexed { i, f -> add(FieldInfo("misc_$i", f.label, f.ocrRect, f)) }
    }
}
