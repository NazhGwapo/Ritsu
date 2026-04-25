package com.example.ritsu.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ritsu.data.GameConfig

@Composable
fun EditorTopSection(
    configs: List<GameConfig>,
    selectedConfig: GameConfig?,
    onConfigSelected: (GameConfig) -> Unit,
    onImportImage: () -> Unit,
    onSave: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedConfig?.gameName ?: "Select Config",
                        maxLines = 1
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    configs.forEach { config ->
                        DropdownMenuItem(
                            text = { Text(config.gameName) },
                            onClick = {
                                onConfigSelected(config)
                                expanded = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = onImportImage) {
                Icon(Icons.Default.Image, contentDescription = "Import Image")
            }

            IconButton(onClick = onSave, enabled = selectedConfig != null) {
                Icon(Icons.Default.Save, contentDescription = "Save Config")
            }
        }
    }
}
