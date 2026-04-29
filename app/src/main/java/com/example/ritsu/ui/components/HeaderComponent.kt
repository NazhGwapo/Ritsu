package com.example.ritsu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.example.ritsu.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderComponent(
    currentScreen: Screen,
    subtitle: String? = null,
    onActionClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = when (currentScreen) {
                        Screen.Score -> "Scores"
                        Screen.Data -> "Data"
                        Screen.Options -> "Options"
                        Screen.ManageConfigs -> "Manage Configs"
                        Screen.Debug -> "Debug"
                        Screen.BoxEditor -> "Box Editor"
                        Screen.Theme -> "Theme"
                        Screen.ChartDetails -> "Chart Details"
                        Screen.GameDetails -> "Game Details"
                        Screen.TopGames -> "Top Games"
                        Screen.TopCharts -> "Top Charts"
                        Screen.TopScores -> "Top Scores"
                        Screen.GraphDetail -> "Graph Detail"
                    }
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        navigationIcon = {
            if (currentScreen != Screen.Score && (currentScreen != Screen.Data)) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            if (currentScreen == Screen.Data) {
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Data"
                    )
                }
            }
            if (currentScreen == Screen.Score || currentScreen == Screen.Data) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Options"
                    )
                }
            }
        }
    )
}
