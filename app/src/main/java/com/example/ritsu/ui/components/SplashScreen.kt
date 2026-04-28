package com.example.ritsu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun SplashScreenContent() {
    // A simple full-screen surface with the primary theme color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Outline Text
            Text(
                text = "律",
                style = MaterialTheme.typography.displayLarge.copy(
                    drawStyle = Stroke(
                        miter = 10f,
                        width = 6f, // This creates the 3px-out / 3px-in outline
                        join = StrokeJoin.Round
                    )
                ),
                color = MaterialTheme.colorScheme.secondary // Contrasting outline color
            )
            // Fill Text
            Text(
                text = "律",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
