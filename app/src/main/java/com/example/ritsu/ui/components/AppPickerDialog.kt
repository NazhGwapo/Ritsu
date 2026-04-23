package com.example.ritsu.ui.components

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (ApplicationInfo) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val apps = remember {
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { it.loadLabel(packageManager).toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(apps) { app ->
                    ListItem(
                        leadingContent = {
                            val icon = remember { app.loadIcon(packageManager) }
                            Box(modifier = Modifier.size(32.dp)) {
                                AsyncImage(model = icon, contentDescription = null)
                            }
                        },
                        headlineContent = { Text(app.loadLabel(packageManager).toString()) },
                        modifier = Modifier.clickable { onAppSelected(app) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
