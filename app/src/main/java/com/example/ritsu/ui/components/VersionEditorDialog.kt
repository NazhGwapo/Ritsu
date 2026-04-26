package com.example.ritsu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun VersionEditorDialog(
    currentVersion: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var versionText by remember { mutableStateOf(currentVersion.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Config Version") },
        text = {
            Column {
                Text("Enter the new version number for this configuration.")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = versionText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) versionText = it },
                    label = { Text("Version Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newVersion = versionText.toIntOrNull()
                    if (newVersion != null) onConfirm(newVersion)
                },
                enabled = versionText.isNotBlank() && versionText.toIntOrNull() != null
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
