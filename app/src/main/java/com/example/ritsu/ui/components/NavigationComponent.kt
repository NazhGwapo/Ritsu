package com.example.ritsu.ui.components

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ritsu.Screen

@Composable
fun NavigationComponent(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
) {
    var showMenu by remember { mutableStateOf(value = false) }

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
                    onClick = { showMenu = false },
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
