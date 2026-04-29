package com.example.ritsu.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DifficultyBadge(name: String, value: String) {
    val badgeText = remember(name, value) {
        val n = name.trim().takeIf { (it.isNotBlank() && it != "Unknown") }
        val v = value.trim().takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
        
        when {
            n != null && v != null -> "$n $v"
            n != null -> n
            v != null -> v
            else -> ""
        }
    }
    
    if (badgeText.isNotBlank()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                text = badgeText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 11.sp
            )
        }
    }
}
