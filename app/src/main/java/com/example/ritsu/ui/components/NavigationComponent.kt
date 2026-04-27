package com.example.ritsu.ui.components

import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ritsu.Screen
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.GenericScore
import com.example.ritsu.data.OCRManager
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.data.ScoreDetail
import com.example.ritsu.data.ScoreRepository
import com.example.ritsu.data.AccuracyCalculator
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun NavigationComponent(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { RitsuDatabase.getDatabase(context) }
    val repository = remember { ScoreRepository(database.scoreDao()) }
    val ocrManager = remember { OCRManager() }
    val json = remember { Json { ignoreUnknownKeys = true } }

    var showMenu by remember { mutableStateOf(value = false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var selectedConfigForGallery by remember { mutableStateOf<GameConfig?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty() && selectedConfigForGallery != null) {
            val config = selectedConfigForGallery!!
            uris.forEach { uri ->
                scope.launch {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }.copy(android.graphics.Bitmap.Config.ARGB_8888, true)

                    val configData = json.decodeFromString<GameConfigData>(config.configData)
                    val extractedData = ocrManager.processImage(bitmap, configData)

                    // Extract play timestamp from metadata
                    var playTimestamp = System.currentTimeMillis()
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val exif = ExifInterface(stream)
                            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) 
                                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                            if (dateTime != null) {
                                val sdf = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.getDefault())
                                playTimestamp = sdf.parse(dateTime)?.time ?: playTimestamp
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Get accuracy from OCR or default to calculation if empty
                    var accuracyVal = extractedData["accuracy"]?.toDoubleOrNull()
                    if (accuracyVal == null) {
                        accuracyVal = AccuracyCalculator.calculate(extractedData, configData) ?: 0.0
                    }

                    val score = GenericScore(
                        configId = config.id,
                        songTitle = extractedData["songTitle"] ?: "Unknown",
                        difficultyName = extractedData["difficultyName"] ?: "Unknown",
                        difficultyVal = extractedData["difficultyVal"] ?: "0.0",
                        difficultySortValue = GenericScore.parseDifficulty(extractedData["difficultyVal"] ?: "0.0"),
                        totalScore = extractedData["totalScore"]?.filter { it.isDigit() }?.toLongOrNull() ?: 0L,
                        maxCombo = extractedData["maxCombo"]?.filter { it.isDigit() }?.toIntOrNull() ?: 0,
                        accuracy = accuracyVal,
                        playRank = if (configData.useRankOcr) (extractedData["playRank"] ?: "N/A") else "",
                        playTimestamp = playTimestamp,
                        importTimestamp = System.currentTimeMillis()
                    )

                    val details = configData.allFieldsWithCategory.map { (field, category) ->
                        ScoreDetail(
                            scoreId = 0, // Will be set in repository
                            key = field.key,
                            value = extractedData[field.key] ?: "",
                            category = category
                        )
                    }

                    repository.saveFullScore(score, details)
                }
            }
        }
        selectedConfigForGallery = null
    }

    if (showConfigDialog) {
        GalleryConfigDialog(
            onDismiss = { showConfigDialog = false },
            onConfigSelected = { config ->
                selectedConfigForGallery = config
                showConfigDialog = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }

    NavigationBar {
        NavigationBarItem(
            selected = currentScreen == Screen.Score,
            onClick = { onScreenSelected(Screen.Score) },
            icon = { Icon(Icons.Default.History, contentDescription = "Scores") },
            label = { Text("Scores") }
        )
        
        // Primary capture button (middle)
        Box(
            modifier = Modifier.weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = { showMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(80.dp)
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Capture",
                    modifier = Modifier.size(32.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Activate Notification Service") },
                    onClick = { showMenu = false },
                    leadingIcon = {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Import from Gallery") },
                    onClick = {
                        showMenu = false
                        showConfigDialog = true
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        NavigationBarItem(
            selected = currentScreen == Screen.Data,
            onClick = { onScreenSelected(Screen.Data) },
            icon = { Icon(Icons.Default.Assessment, contentDescription = "Data") },
            label = { Text("Data") }
        )
    }
}
