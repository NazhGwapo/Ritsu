package com.example.ritsu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ritsu.ui.screens.debug.DatabaseViewerDialog
import com.example.ritsu.ui.screens.debug.ManualEntryDialog
import com.example.ritsu.ui.screens.debug.OcrTestDialog
import com.example.ritsu.ui.screens.debug.ScoreCardEditorDialog

@Composable
fun DebugScreen() {
    var showOcrDialog by remember { mutableStateOf(false) }
    var showCardEditor by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showDatabaseViewer by remember { mutableStateOf(false) }

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
}
