package com.example.ritsu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.ui.screens.debug.DatabaseDummyEntriesDialog
import com.example.ritsu.ui.screens.debug.DatabaseViewerDialog
import com.example.ritsu.ui.screens.debug.ManualEntryDialog
import com.example.ritsu.ui.screens.debug.OcrTestDialog
import com.example.ritsu.ui.screens.debug.ScoreCardEditorDialog
import kotlinx.coroutines.launch

@Composable
fun DebugScreen() {
    var showOcrDialog by remember { mutableStateOf(value = false) }
    var showCardEditor by remember { mutableStateOf(value = false) }
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
                modifier = Modifier.clickable { showOcrDialog = true },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("ScoreCard Editor") },
                modifier = Modifier.clickable { showCardEditor = true },
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
        ScoreCardEditorDialog { showCardEditor = false }
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
        var clearScores by remember { mutableStateOf(true) }
        var clearConfigs by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Database") },
            text = {
                Column {
                    Text("Select what you want to delete:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearScores = !clearScores }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = clearScores, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Scores")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearConfigs = !clearConfigs }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = clearConfigs, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Configurations")
                    }
                    if (clearScores && clearConfigs) {
                        Text(
                            text = "Warning: This will delete EVERYTHING.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = clearScores || clearConfigs,
                    onClick = {
                        scope.launch {
                            if (clearScores) database.scoreDao().deleteAllScores()
                            if (clearConfigs) database.scoreDao().deleteAllConfigs()
                            showClearConfirm = false
                        }
                    }
                ) {
                    Text(if (clearScores && clearConfigs) "Clear Everything" else "Clear Selected")
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
