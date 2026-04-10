package com.example.ritsu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ritsu.Screen

@Composable
fun NavigationComponent(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentScreen == Screen.Score,
            onClick = { onScreenSelected(Screen.Score) },
            icon = { Icon(Icons.Default.History, contentDescription = "Scores") },
            label = { Text("Scores") }
        )
        
        // Primary capture button (middle)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = { /* Handle camera action */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture")
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
