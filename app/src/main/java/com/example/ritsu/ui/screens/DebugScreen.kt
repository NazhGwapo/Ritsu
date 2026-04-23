package com.example.ritsu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.screens.debug.DatabaseDummyEntriesDialog
import com.example.ritsu.ui.screens.debug.DatabaseViewerDialog
import com.example.ritsu.ui.screens.debug.ManualEntryDialog
import com.example.ritsu.ui.screens.debug.OcrTestDialog
import com.example.ritsu.ui.screens.debug.ScoreCardEditorDialog
import kotlinx.coroutines.launch

@Composable
fun DebugScreen() {
    var showOcrDialog by remember { mutableStateOf(false) }
    var showCardEditor by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showDatabaseViewer by remember { mutableStateOf(false) }
    var showDummyEntriesDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { RitsuDatabase.getDatabase(context) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ListItem(
                headlineContent = { Text("OCR test") },
                modifier = Modifier.clickable { showOcrDialog = true }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("ScoreCard Editor") },
                modifier = Modifier.clickable { showCardEditor = true }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Database entry") },
                modifier = Modifier.clickable { showManualEntryDialog = true }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("View database") },
                modifier = Modifier.clickable { showDatabaseViewer = true }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Database dummy entries") },
                modifier = Modifier.clickable { showDummyEntriesDialog = true }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Clear database") },
                modifier = Modifier.clickable { showClearConfirm = true }
            )
        }
    }

    if (showOcrDialog) {
        OcrTestDialog(onDismiss = { showOcrDialog = false })
    }
    
    if (showCardEditor) {
        ScoreCardEditorDialog(onDismiss = { showCardEditor = false })
    }

    if (showManualEntryDialog) {
        ManualEntryDialog(onDismiss = { showManualEntryDialog = false })
    }

    if (showDatabaseViewer) {
        DatabaseViewerDialog(onDismiss = { showDatabaseViewer = false })
    }

    if (showDummyEntriesDialog) {
        DatabaseDummyEntriesDialog(onDismiss = { showDummyEntriesDialog = false })
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Database") },
            text = { Text("This will delete ALL scores and ALL configurations. Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            database.scoreDao().deleteAllScores()
                            database.scoreDao().deleteAllConfigs()
                            showClearConfirm = false
                        }
                    }
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
