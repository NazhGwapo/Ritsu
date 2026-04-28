package com.example.ritsu.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ritsu.data.FullScoreRecord
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.components.GraphSeries
import com.example.ritsu.ui.components.ScoreDetailsDialog
import com.example.ritsu.ui.navigation.DetailFilterSheet
import com.example.ritsu.ui.utils.DateUtils
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

private fun getNiceInterval(range: Float, targetSteps: Int): Float {
    if (range <= 0) return 1f
    val rawInterval = range / targetSteps
    val magnitude = 10.0.pow(floor(log10(rawInterval.toDouble()))).toFloat()
    val residual = rawInterval / magnitude
    val niceResidual = when {
        residual < 1.5 -> 1f
        residual < 3.0 -> 2f
        residual < 7.0 -> 5f
        else -> 10f
    }
    return niceResidual * magnitude
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphDetailScreen(
    configId: Long,
    graphType: String,
    songTitle: String? = null,
    difficultyName: String? = null,
    difficultyVal: String? = null,
    isNormalized: Boolean = false,
    onBack: () -> Unit,
    onChartDetails: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    onGameDetails: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { RitsuDatabase.getDatabase(context) }
    val scoreDao = database.scoreDao()
    val json = remember { Json { ignoreUnknownKeys = true } }
    val density = LocalDensity.current

    val configs by scoreDao.getAllConfigs().collectAsState(initial = emptyList())
    val gameConfig = configs.find { it.id == configId }
    val scores by scoreDao.getAllScores().collectAsState(initial = emptyList())

    // --- FILTERS STATE ---
    val allDiffNames = remember(scores, configId, songTitle) {
        scores.filter { it.genericScore.configId == configId && (songTitle == null || it.genericScore.songTitle == songTitle) }
            .map { it.genericScore.difficultyName }.distinct().sorted()
    }
    val allDiffVals = remember(scores, configId, songTitle) {
        scores.filter { it.genericScore.configId == configId && (songTitle == null || it.genericScore.songTitle == songTitle) }
            .map { it.genericScore.difficultySortValue }
            .filter { it > 0.0 }.distinct().sorted()
    }

    var selectedDiffNames by remember { mutableStateOf(if (difficultyName != null) setOf(difficultyName) else allDiffNames.toSet()) }
    var diffValueRange by remember(allDiffVals) { 
        val min = allDiffVals.minOrNull()?.toFloat() ?: 0f
        val max = allDiffVals.maxOrNull()?.toFloat() ?: 20f
        mutableStateOf(min..max) 
    }
    
    // Date Filtering
    val dateRanges = listOf("Day", "Week", "Month", "Year", "All Time", "Custom")
    var selectedDateRange by remember { mutableStateOf("All Time") }
    val dateOptions = remember(selectedDateRange) { DateUtils.getDateOptions(selectedDateRange) }
    var selectedDateOption by remember(selectedDateRange) { mutableStateOf(dateOptions.firstOrNull() ?: "") }
    var customStartDate by remember { mutableLongStateOf(0L) }
    var customEndDate by remember { mutableLongStateOf(Long.MAX_VALUE) }
    
    // UI State
    var normalized by remember { mutableStateOf(isNormalized) }
    var currentFilterSheet by remember { mutableStateOf(DetailFilterSheet.NONE) }
    var datePickerVisible by remember { mutableStateOf(false) }
    var diffFilterVisible by remember { mutableStateOf(false) }
    
    // Graph State
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var showScoreDetails by remember { mutableStateOf<FullScoreRecord?>(null) }
    var scoreToEdit by remember { mutableStateOf<FullScoreRecord?>(null) }

    val textMeasurer = rememberTextMeasurer()

    // --- DATA FILTERING ---
    val filteredScores = remember(scores, configId, songTitle, selectedDiffNames, diffValueRange, selectedDateRange, selectedDateOption, customStartDate, customEndDate) {
        scores.filter { record ->
            val s = record.genericScore
            val dVal = s.difficultySortValue
            
            val matchesBasic = s.configId == configId &&
                (songTitle == null || s.songTitle == songTitle) &&
                (selectedDiffNames.isEmpty() || selectedDiffNames.contains(s.difficultyName)) &&
                (dVal >= diffValueRange.start && dVal <= diffValueRange.endInclusive)

            if (!matchesBasic) return@filter false

            DateUtils.filterByDate(
                s.playTimestamp,
                selectedDateRange,
                selectedDateOption,
                customStartDate,
                customEndDate
            )
        }.sortedBy { it.genericScore.playTimestamp }
    }

    // --- COLORS & CALCS ---
    val accColor = MaterialTheme.colorScheme.secondary
    val scoreColor = MaterialTheme.colorScheme.primary
    val comboColor = Color(0xFFFFA000)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val graphSeries = remember(filteredScores, gameConfig, graphType, normalized, accColor, scoreColor) {
        val configData = gameConfig?.let { try { json.decodeFromString<GameConfigData>(it.configData) } catch (_: Exception) { null } }
        fun calculateTotalNotes(record: FullScoreRecord) = record.details.filter { it.category == "Judgment" }.sumOf { it.value.toLongOrNull() ?: 0L }

        when (graphType) {
            "Judgement" -> {
                val keys = configData?.judgments?.map { it.key } ?: emptyList()
                val labels = configData?.judgments?.associate { it.key to it.label } ?: emptyMap()
                val colors = listOf(Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFF03A9F4), Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFFFF9800))
                keys.mapIndexed { i, key ->
                    GraphSeries(label = labels[key] ?: key, data = filteredScores.map { r ->
                        val raw = r.details.find { d -> d.key == key }?.value?.toFloatOrNull() ?: 0f
                        if (normalized) { val t = calculateTotalNotes(r); if (t > 0) (raw/t*100f) else 0f } else raw
                    }, color = colors.getOrElse(i) { Color.Gray })
                }
            }
            "Metric" -> {
                val fields = configData?.metrics?.filter { it.type != "boolean" } ?: emptyList()
                val colors = listOf(Color(0xFF00BCD4), Color(0xFFFF5722), Color(0xFF8BC34A), Color(0xFFE91E63), Color(0xFF673AB7), Color(0xFFCDDC39))
                fields.mapIndexed { i, f ->
                    GraphSeries(label = f.label, data = filteredScores.map { r -> r.details.find { d -> d.key == f.key }?.value?.toFloatOrNull() ?: 0f }, color = colors.getOrElse(i) { Color.Gray })
                }
            }
            else -> {
                val c = when(graphType) { "Accuracy" -> accColor; "Score" -> scoreColor; "Combo" -> comboColor; else -> Color.Cyan }
                listOf(GraphSeries(label = graphType, data = filteredScores.map { r ->
                    when (graphType) {
                        "Accuracy" -> r.genericScore.accuracy.toFloat()
                        "Score" -> r.genericScore.totalScore.toFloat()
                        "Combo" -> { val raw = r.genericScore.maxCombo.toFloat(); if (normalized) { val t = calculateTotalNotes(r); if (t > 0) (raw/t*100f) else 0f } else raw }
                        else -> 0f
                    }
                }, color = c))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // --- TOP FILTERS BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (graphType == "Judgement" || graphType == "Combo") {
                FilterChip(selected = normalized, onClick = { normalized = !normalized }, label = { Text("Normalized", fontSize = 12.sp) })
            }
            FilterChip(
                selected = selectedDiffNames.size != allDiffNames.size,
                onClick = { diffFilterVisible = true },
                label = { Text(if (selectedDiffNames.size == allDiffNames.size) "Difficulty" else "${selectedDiffNames.size} Sel", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = selectedDateRange != "All Time",
                onClick = { currentFilterSheet = DetailFilterSheet.DATE_RANGE },
                label = { Text(selectedDateRange, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp)) }
            )
            if (selectedDateRange != "All Time" && selectedDateRange != "Custom") {
                FilterChip(selected = true, onClick = { currentFilterSheet = DetailFilterSheet.DATE_PERIOD }, label = { Text(selectedDateOption, fontSize = 12.sp) })
            } else if (selectedDateRange == "Custom") {
                FilterChip(selected = customStartDate != 0L, onClick = { datePickerVisible = true }, label = { Text(if (customStartDate == 0L) "Select Range" else "Custom Range", fontSize = 12.sp) })
            }
        }

        // Difficulty Range Slider
        if (allDiffVals.size > 1) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("Difficulty Range: ${"%.1f".format(diffValueRange.start)} - ${"%.1f".format(diffValueRange.endInclusive)}", style = MaterialTheme.typography.labelSmall, color = onSurface.copy(alpha = 0.6f))
                RangeSlider(
                    value = diffValueRange,
                    onValueChange = { diffValueRange = it },
                    valueRange = (allDiffVals.minOrNull()?.toFloat() ?: 0f)..(allDiffVals.maxOrNull()?.toFloat() ?: 20f),
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        // --- INTERACTIVE GRAPH ---
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
            val scope = this
            val constraintsW = with(density) { scope.maxWidth.toPx() }
            val constraintsH = with(density) { scope.maxHeight.toPx() }
            val drawHeight = if (constraintsH > constraintsW * 1.2f) constraintsW * 1.2f else constraintsH
            val verticalOffset = (constraintsH - drawHeight) / 2
            val horizontalPadding = with(density) { 32.dp.toPx() }

            if (graphSeries.isEmpty() || graphSeries.all { it.data.isEmpty() }) {
                Text("No data points matching filters", modifier = Modifier.align(Alignment.Center), color = onSurfaceVariant.copy(alpha = 0.5f))
            } else {
                val allData = graphSeries.flatMap { it.data }
                val maxVal = allData.maxOrNull() ?: 1f
                val minVal = allData.minOrNull() ?: 0f
                val range = (maxVal - minVal).coerceAtLeast(0.01f)
                val dataSize = graphSeries.first().data.size
                val stepX = (constraintsW - horizontalPadding * 2) / (dataSize - 1).coerceAtLeast(1)

                // Viewport Calculations
                val viewportTopVal = (minVal + range) - (panOffset.y / (drawHeight * zoomScale) * range)
                val viewportBottomVal = viewportTopVal - (constraintsH / (drawHeight * zoomScale) * range)
                val viewportLeftIdx = ((-panOffset.x - horizontalPadding) / (stepX * zoomScale))
                val viewportRightIdx = ((constraintsW - panOffset.x - horizontalPadding) / (stepX * zoomScale))

                Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = zoomScale
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 20f)
                        val scaleChange = zoomScale / oldScale
                        panOffset = (panOffset - centroid) * scaleChange + centroid + pan
                    }
                }.pointerInput(Unit) {
                    detectTapGestures { offset ->
                        var bestI = -1; var minDist = Float.MAX_VALUE
                        for (i in 0 until dataSize) {
                            for (series in graphSeries) {
                                val dx = (i * stepX * zoomScale) + panOffset.x + horizontalPadding
                                val dy = (drawHeight - ((series.data[i] - minVal) / range * drawHeight)) * zoomScale + panOffset.y + verticalOffset
                                val dist = sqrt((dx - offset.x)*(dx - offset.x) + (dy - offset.y)*(dy - offset.y))
                                if (dist < 60f && dist < minDist) { minDist = dist; bestI = i }
                            }
                        }
                        selectedPointIndex = if (bestI != -1) bestI else null
                    }
                }) {
                    val gridColor = onSurface.copy(alpha = 0.1f)
                    val labelStyle = TextStyle(fontSize = 8.sp, color = onSurfaceVariant.copy(alpha = 0.6f))
                    
                    fun dataToScreenX(idx: Float) = (idx * stepX * zoomScale) + panOffset.x + horizontalPadding
                    fun dataToScreenY(v: Float) = (drawHeight - ((v - minVal) / range * drawHeight)) * zoomScale + panOffset.y + verticalOffset

                    // --- DRAW GRID & Y-AXIS LABELS (Value-based) ---
                    val targetYLines = 6
                    val viewportRangeY = viewportTopVal - viewportBottomVal
                    val niceIntervalY = getNiceInterval(viewportRangeY, targetYLines)
                    val firstLineY = floor(viewportBottomVal / niceIntervalY) * niceIntervalY
                    
                    var currentLineY = firstLineY
                    while (currentLineY <= viewportTopVal + niceIntervalY) {
                        val dy = dataToScreenY(currentLineY)
                        if (dy in 0f..size.height) {
                            drawLine(color = gridColor, start = Offset(0f, dy), end = Offset(size.width, dy), strokeWidth = 1f)
                            
                            val label = if (normalized || graphType == "Accuracy") "%.1f%%".format(currentLineY) else "%,d".format(currentLineY.toLong())
                            val layoutResult = textMeasurer.measure(label, labelStyle)
                            val labelY = (dy + 2.dp.toPx()).coerceAtMost(size.height - layoutResult.size.height.toFloat())
                            if (labelY >= 0f) {
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = label,
                                    style = labelStyle,
                                    topLeft = Offset(4.dp.toPx(), labelY)
                                )
                            }
                        }
                        currentLineY += niceIntervalY
                    }

                    // --- DRAW X-AXIS LABELS (Index-based) ---
                    val targetXLines = 5
                    val viewportRangeX = viewportRightIdx - viewportLeftIdx
                    val niceIntervalX = getNiceInterval(viewportRangeX.toFloat(), targetXLines).coerceAtLeast(1f).toInt()
                    val firstLineX = floor(viewportLeftIdx / niceIntervalX).toInt() * niceIntervalX
                    
                    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                    val sdfShort = SimpleDateFormat("MM/dd", Locale.getDefault())
                    
                    var currentLineX = firstLineX
                    while (currentLineX <= viewportRightIdx + niceIntervalX) {
                        if (currentLineX in 0 until dataSize) {
                            val dx = dataToScreenX(currentLineX.toFloat())
                            if (dx in 0f..size.width) {
                                drawLine(color = gridColor, start = Offset(dx, 0f), end = Offset(dx, size.height), strokeWidth = 1f)
                                
                                val date = Date(filteredScores[currentLineX].genericScore.playTimestamp)
                                val label = if (zoomScale > 5f) sdf.format(date) else sdfShort.format(date)
                                
                                val layoutResult = textMeasurer.measure(label, labelStyle)
                                val labelX = (dx + 4.dp.toPx()).coerceAtMost(size.width - layoutResult.size.width.toFloat())
                                val labelY = (size.height - layoutResult.size.height.toFloat() - 4.dp.toPx())
                                
                                if (labelX >= 0f && labelY >= 0f) {
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = label,
                                        style = labelStyle,
                                        topLeft = Offset(labelX, labelY)
                                    )
                                }
                            }
                        }
                        currentLineX += niceIntervalX
                    }

                    // --- DRAW DATA ---
                    val trendColor = onSurface.copy(alpha = 0.4f)
                    if (graphSeries.size == 1 && dataSize > 1) {
                        val d = graphSeries.first().data
                        var sX = 0f; var sY = 0f; var sXY = 0f; var sX2 = 0f
                        d.forEachIndexed { i, y -> val x = i.toFloat(); sX += x; sY += y; sXY += x*y; sX2 += x*x }
                        val denom = dataSize * sX2 - sX * sX
                        if (denom != 0f) {
                            val m = (dataSize * sXY - sX * sY) / denom; val b = (sY - m * sX) / dataSize
                            drawLine(color = trendColor, start = Offset(dataToScreenX(0f), dataToScreenY(b)), end = Offset(dataToScreenX(dataSize - 1f), dataToScreenY(m * (dataSize - 1) + b)), strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        }
                    }
                    graphSeries.forEach { series ->
                        val path = Path()
                        series.data.forEachIndexed { i, v ->
                            val dx = dataToScreenX(i.toFloat())
                            val dy = dataToScreenY(v)
                            if (i == 0) path.moveTo(dx, dy) else path.lineTo(dx, dy)
                            drawCircle(color = series.color, radius = 2.dp.toPx(), center = Offset(dx, dy))
                        }
                        drawPath(path = path, color = series.color, style = Stroke(width = 2.dp.toPx()))
                    }
                    selectedPointIndex?.let { idx ->
                        val dx = dataToScreenX(idx.toFloat())
                        drawLine(color = onSurface.copy(alpha = 0.2f), start = Offset(dx, 0f), end = Offset(dx, size.height), strokeWidth = 1.dp.toPx())
                        graphSeries.forEach { s ->
                            val dy = dataToScreenY(s.data[idx])
                            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(dx, dy))
                            drawCircle(color = s.color, radius = 3.dp.toPx(), center = Offset(dx, dy))
                        }
                    }
                }
            }
        }

        // Legend
        if (graphSeries.size > 1) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center) {
                graphSeries.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(s.color))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(s.label, fontSize = 10.sp, color = onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // Detail Widget
        selectedPointIndex?.let { idx ->
            val r = filteredScores[idx]
            Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.genericScore.songTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${r.genericScore.difficultyName} ${r.genericScore.difficultyVal} • ${SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(r.genericScore.playTimestamp))}", style = MaterialTheme.typography.bodySmall)
                        val txt = when (graphType) {
                            "Judgement", "Metric" -> graphSeries.joinToString(", ") { s -> "${s.label}: ${s.data[idx].toInt()}" }
                            "Accuracy" -> "Accuracy: ${"%.2f".format(r.genericScore.accuracy)}%"
                            "Score" -> "Score: %,d".format(r.genericScore.totalScore)
                            "Combo" -> if (normalized) "Combo: %.1f%%".format(graphSeries.first().data[idx]) else "Max Combo: ${r.genericScore.maxCombo}x"
                            else -> ""
                        }
                        Text(txt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = scoreColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Button(onClick = { showScoreDetails = r }, shape = RoundedCornerShape(8.dp)) { Text("Details", fontSize = 12.sp) }
                }
            }
        }
    }

    // --- MODAL BOTTOM SHEETS ---
    if (currentFilterSheet != DetailFilterSheet.NONE) {
        ModalBottomSheet(onDismissRequest = { currentFilterSheet = DetailFilterSheet.NONE }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text(text = when(currentFilterSheet) {
                    DetailFilterSheet.DATE_RANGE -> "Select Date Range"
                    DetailFilterSheet.DATE_PERIOD -> "Select $selectedDateRange"
                    else -> ""
                }, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

                when (currentFilterSheet) {
                    DetailFilterSheet.DATE_RANGE -> {
                        dateRanges.forEach { r ->
                            ListItem(
                                headlineContent = { Text(r) },
                                modifier = Modifier.clickable { 
                                    selectedDateRange = r
                                    if (r == "Custom") datePickerVisible = true else currentFilterSheet = DetailFilterSheet.NONE
                                }
                            )
                        }
                    }
                    DetailFilterSheet.DATE_PERIOD -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(dateOptions) { opt ->
                                ListItem(headlineContent = { Text(opt) }, modifier = Modifier.clickable { selectedDateOption = opt; currentFilterSheet = DetailFilterSheet.NONE })
                            }
                        }
                    }
                    else -> {}
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (diffFilterVisible) {
        ModalBottomSheet(onDismissRequest = { diffFilterVisible = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text(text = "Filter Difficulties", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(allDiffNames) { name ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedDiffNames = if (selectedDiffNames.contains(name)) selectedDiffNames - name else selectedDiffNames + name }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selectedDiffNames.contains(name), onCheckedChange = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (datePickerVisible) {
        val state = rememberDateRangePickerState()
        Dialog(onDismissRequest = { datePickerVisible = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Select Date Range", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { datePickerVisible = false }) { Icon(Icons.Default.Close, null) }
                    }
                    DateRangePicker(state = state, modifier = Modifier.weight(1f))
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { customStartDate = state.selectedStartDateMillis ?: 0L; customEndDate = state.selectedEndDateMillis?.let { it + 86400000 - 1 } ?: Long.MAX_VALUE; selectedDateRange = "Custom"; datePickerVisible = false; currentFilterSheet = DetailFilterSheet.NONE }) { Text("Apply Range") }
                }
            }
        }
    }

    if (showScoreDetails != null && gameConfig != null) {
        ScoreDetailsDialog(
            scoreRecord = showScoreDetails!!,
            gameConfig = gameConfig,
            scoreDao = scoreDao,
            onDismiss = { showScoreDetails = null },
            onEdit = {
                scoreToEdit = showScoreDetails
                showScoreDetails = null
            },
            onChartDetails = {
                val s = showScoreDetails!!.genericScore
                showScoreDetails = null
                onChartDetails(s.configId, s.songTitle, s.difficultyName, s.difficultyVal)
            },
            onGameDetails = {
                val s = showScoreDetails!!.genericScore
                showScoreDetails = null
                onGameDetails(s.configId)
            }
        )
    }

    if (scoreToEdit != null) {
        com.example.ritsu.ui.screens.debug.ManualEntryDialog(
            initialRecord = scoreToEdit,
            onDismiss = { scoreToEdit = null }
        )
    }
}
