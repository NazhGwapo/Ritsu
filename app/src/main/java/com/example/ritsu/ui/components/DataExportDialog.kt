package com.example.ritsu.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.scale
import coil3.ImageLoader
import coil3.request.allowHardware
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ritsu.ui.cards.ActivityDataPoint
import com.example.ritsu.ui.cards.GameListItem
import com.example.ritsu.ui.cards.ScoreListItem
import com.example.ritsu.ui.cards.TopGameItem
import com.example.ritsu.ui.cards.TopScoreItem
import com.example.ritsu.ui.theme.RitsuTheme
import androidx.compose.ui.text.font.FontWeight
import com.example.ritsu.ui.utils.CaptureUtils
import com.example.ritsu.ui.utils.LocalImageLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
fun DataExportDialog(
    dateRange: String,
    topGames: List<TopGameItem>,
    topCharts: List<TopScoreItem>,
    topScores: List<TopScoreItem>,
    activityData: List<ActivityDataPoint>,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf("Summary") }
    var captureMode by remember { mutableStateOf<String?>(null) } // "Save" or "Share"
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data as Image") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Content", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    val options = listOf("Summary", "Top Games", "Top Charts", "Top Scores")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.take(2).forEach { option ->
                                FilterChip(
                                    selected = selectedOption == option,
                                    onClick = { selectedOption = option },
                                    label = { Text(option) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.drop(2).forEach { option ->
                                FilterChip(
                                    selected = selectedOption == option,
                                    onClick = { selectedOption = option },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }
                }

                if (captureMode != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Text(
                            "Preparing image for ${captureMode?.lowercase()}...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { captureMode = "Save" },
                    enabled = captureMode == null
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
                Button(
                    onClick = { captureMode = "Share" },
                    enabled = captureMode == null
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = captureMode == null) {
                Text("Cancel")
            }
        }
    )

    if (captureMode != null) {
        // Use a hidden ComposeView to render and capture
        HiddenCaptureView(
            dateRange = dateRange,
            topGames = topGames,
            topCharts = topCharts,
            topScores = topScores,
            activityData = activityData,
            option = selectedOption,
            onCaptured = { bitmap ->
                val fileName = "Ritsu_Data_${selectedOption.replace(" ", "_")}"
                if (captureMode == "Save") {
                    CaptureUtils.saveBitmapToGallery(context, bitmap, fileName)
                } else {
                    CaptureUtils.shareBitmap(context, bitmap, fileName)
                }
                captureMode = null
                onDismiss()
            }
        )
    }
}

@Composable
fun HiddenCaptureView(
    dateRange: String,
    topGames: List<TopGameItem>,
    topCharts: List<TopScoreItem>,
    topScores: List<TopScoreItem>,
    activityData: List<ActivityDataPoint>,
    option: String,
    onCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Create a specialized ImageLoader that disables hardware bitmaps for capturing
    val captureImageLoader = remember {
        ImageLoader.Builder(context)
            .allowHardware(false)
            .build()
    }

    // Use a fixed density to ensure consistent scaling across devices.
    // 1080px / 1.5 density = 720dp (standard tablet/large screen width)
    val customDensity = remember {
        Density(density = 1.5f, fontScale = 0.8f)
    }

    AndroidView(
        factory = { ctx ->
            ComposeView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(1080, 1080)
                visibility = View.INVISIBLE
                setContent {
                    CompositionLocalProvider(
                        LocalImageLoader provides captureImageLoader,
                        LocalDensity provides customDensity
                    ) {
                        RitsuTheme {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                when (option) {
                                    "Summary" -> SummaryExportContent(
                                        dateRange = dateRange,
                                        topGames = topGames,
                                        topCharts = topCharts,
                                        topScores = topScores,
                                        activityData = activityData
                                    )
                                    "Top Games" -> IndividualExportCard(
                                        title = "Top Games",
                                        dateRange = dateRange
                                    ) {
                                        topGames.take(10).forEach { 
                                            GameListItem(it, isCompact = true, onClick = {}) 
                                        }
                                    }
                                    "Top Charts" -> IndividualExportCard(
                                        title = "Top Charts",
                                        dateRange = dateRange
                                    ) {
                                        topCharts.take(10).forEach { 
                                            ScoreListItem(it, selectedSort = "Plays", isCompact = true, onClick = {}) 
                                        }
                                    }
                                    "Top Scores" -> IndividualExportCard(
                                        title = "Top Scores",
                                        dateRange = dateRange
                                    ) {
                                        topScores.take(10).forEach { 
                                            ScoreListItem(it, selectedSort = "Accuracy", isCompact = true, onClick = {}) 
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        update = { view ->
            scope.launch {
                // Wait for layout and multiple frames to ensure all images are loaded in software mode
                delay(2000)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                view.draw(canvas)
                onCaptured(bitmap)
            }
        }
    )
}

@Composable
fun IndividualExportCard(
    title: String,
    dateRange: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(dateRange, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content
                )
            }
            
            Text("Generated by Ritsu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        }
    }
}
