package com.example.ritsu.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomSelector(
    viewModel: EditorViewModel,
    onEditField: () -> Unit
) {
    val data by viewModel.configData.collectAsState()
    val selectedKey by viewModel.selectedKey.collectAsState()
    
    val categories = listOf("Generic", "Judgment", "Metric", "Misc")
    var selectedCategoryIndex by remember(selectedKey) {
        val key = selectedKey
        val catIndex = when {
            key == null || key.endsWith("Rect") -> 0
            key.startsWith("judgment_") -> 1
            key.startsWith("metric_") -> 2
            key.startsWith("misc_") -> 3
            else -> 0
        }
        mutableIntStateOf(catIndex)
    }

    val currentCategory = categories[selectedCategoryIndex]
    
    val fieldsInCategory = remember(data, currentCategory) {
        mutableListOf<Pair<String, String>>().apply {
            data?.let { d ->
                when (currentCategory) {
                    "Generic" -> {
                        add("titleRect" to "Title")
                        add("scoreRect" to "Score")
                        add("comboRect" to "Combo")
                        add("difficultyNameRect" to "Diff Name")
                        add("difficultyValRect" to "Diff Val")
                        add("rankRect" to "Rank")
                    }
                    "Judgment" -> d.judgments.forEachIndexed { i, f -> add("judgment_$i" to f.label) }
                    "Metric" -> d.metrics.forEachIndexed { i, f -> add("metric_$i" to f.label) }
                    "Misc" -> d.misc.forEachIndexed { i, f -> add("misc_$i" to f.label) }
                }
            }
        }
    }

    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column {
            TabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentKey = selectedKey
                    if (currentKey != null && !currentKey.endsWith("Rect")) {
                        FilledTonalButton(
                            onClick = onEditField,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Properties", fontSize = 13.sp)
                        }
                    }
                }
                
                if (currentCategory != "Generic") {
                    Button(
                        onClick = { viewModel.addField(currentCategory) },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Field", fontSize = 13.sp)
                    }
                }
            }

            val currentKey = selectedKey
            ScrollableTabRow(
                selectedTabIndex = fieldsInCategory.indexOfFirst { it.first == currentKey }.coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                fieldsInCategory.forEach { (fieldKey, label) ->
                    Tab(
                        selected = currentKey == fieldKey,
                        onClick = { viewModel.setSelectedKey(fieldKey) },
                        text = { Text(label, fontSize = 13.sp) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
