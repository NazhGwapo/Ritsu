package com.example.ritsu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NewConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var gameName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Configuration") },
        text = {
            Column {
                Text("Enter the name of the game for this configuration.")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = gameName,
                    onValueChange = { gameName = it },
                    label = { Text("Game Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (gameName.isNotBlank()) onConfirm(gameName) },
                enabled = gameName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
