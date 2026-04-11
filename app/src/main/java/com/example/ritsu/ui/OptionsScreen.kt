package com.example.ritsu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun OptionsScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ListItem(
                headlineContent = { Text("Import Configuration") },
                leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable { /* No functionality */ }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Manage Configurations") },
                leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                modifier = Modifier.clickable { /* No functionality */ }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Delete Data") },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                modifier = Modifier.clickable { /* No functionality */ }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Export Data") },
                leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                modifier = Modifier.clickable { /* No functionality */ }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Help") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                modifier = Modifier.clickable { /* No functionality */ }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Debug") },
                leadingContent = { Icon(Icons.Default.BugReport, contentDescription = null) },
                modifier = Modifier.clickable { /* No functionality */ }
            )
        }
    }
}
