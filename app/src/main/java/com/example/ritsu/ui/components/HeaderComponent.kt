package com.example.ritsu.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.example.ritsu.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderComponent(
    currentScreen: Screen,
    onActionClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = when (currentScreen) {
                    Screen.Score -> "Scores"
                    Screen.Data -> "Data"
                    Screen.Options -> "Options"
                    Screen.ManageConfigs -> "Manage Configs"
                    Screen.Debug -> "Debug"
                    Screen.BoxEditor -> "Box Editor"
                }
            )
        },
        actions = {
            val icon = when (currentScreen) {
                Screen.Score, Screen.Data -> Icons.Default.Settings
                else -> Icons.Default.Close
            }
            val contentDescription = when (currentScreen) {
                Screen.Score, Screen.Data -> "Options"
                else -> "Close"
            }
            
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription
                )
            }
        }
    )
}
