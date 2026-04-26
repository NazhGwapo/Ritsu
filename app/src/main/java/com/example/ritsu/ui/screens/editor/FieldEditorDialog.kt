package com.example.ritsu.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.ritsu.data.GameConfigData

@Composable
fun FieldEditorDialog(
    configData: GameConfigData,
    selectedKey: String,
    onDismiss: () -> Unit,
    onConfigDataChanged: (GameConfigData) -> Unit,
    onDeleteField: () -> Unit
) {
    val fieldWithCategory = remember(configData, selectedKey) {
        val category = when {
            selectedKey.startsWith("judgment_") -> "Judgment"
            selectedKey.startsWith("metric_") -> "Metric"
            selectedKey.startsWith("misc_") -> "Misc"
            else -> null
        }
        val index = selectedKey.substringAfter("_").toIntOrNull()
        if (category != null && index != null) {
            val field = when (category) {
                "Judgment" -> configData.judgments.getOrNull(index)
                "Metric" -> configData.metrics.getOrNull(index)
                "Misc" -> configData.misc.getOrNull(index)
                else -> null
            }
            if (field != null) Triple(field, category, index) else null
        } else null
    }

    if (fieldWithCategory == null) return

    val (field, category, index) = fieldWithCategory
    var key by remember { mutableStateOf(field.key) }
    var label by remember { mutableStateOf(field.label) }
    var type by remember { mutableStateOf(field.type) }
    var threshold by remember { mutableStateOf(field.threshold) }
    var targetColor by remember { mutableStateOf(field.targetColor) }
    var weight by remember { mutableStateOf(field.weight?.toString() ?: "") }
    var newCategory by remember { mutableStateOf(category) }

    var hexString by remember(targetColor) {
        mutableStateOf(targetColor?.let { String.format("#%08X", it) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Field Properties") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Key") }, modifier = Modifier.fillMaxWidth())
                
                Text("Category:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = newCategory == "Judgment", onClick = { newCategory = "Judgment" }, label = { Text("Judgment") })
                    FilterChip(selected = newCategory == "Metric", onClick = { newCategory = "Metric" }, label = { Text("Metric") })
                    FilterChip(selected = newCategory == "Misc", onClick = { newCategory = "Misc" }, label = { Text("Misc") })
                }

                Text("Type:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "number", onClick = { type = "number" }, label = { Text("Number") })
                    FilterChip(selected = type == "text", onClick = { type = "text" }, label = { Text("Text") })
                    FilterChip(selected = type == "boolean", onClick = { type = "boolean" }, label = { Text("Boolean") })
                }

                if (type == "number" && newCategory == "Judgment") {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Accuracy Weight (e.g., 1.0 or 0.5)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.0 - 1.0") }
                    )
                }

                if (type == "boolean") {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Boolean Detection Settings", style = MaterialTheme.typography.titleSmall)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    targetColor?.let { Color(it) } ?: Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        )
                        
                        OutlinedTextField(
                            value = hexString,
                            onValueChange = { 
                                hexString = it
                                try {
                                    val cleaned = it.removePrefix("#")
                                    if (cleaned.length == 8 || cleaned.length == 6) {
                                        targetColor = android.graphics.Color.parseColor(if (cleaned.startsWith("#")) cleaned else "#$cleaned")
                                    }
                                } catch (e: Exception) {}
                            },
                            label = { Text("Target Color (Hex)") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("#AARRGGBB") }
                        )
                    }

                    Text("Threshold: ${"%.2f".format(threshold)}", style = MaterialTheme.typography.labelMedium)
                    Slider(value = threshold, onValueChange = { threshold = it }, valueRange = 0.01f..0.5f)
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "Tip: While this field is selected, you can:\n" +
                            "1. Drag on the image to draw its detection region (color will be auto-sampled from center).\n" +
                            "2. Use the Eyedrop tool (color icon) to precisely sample a color by tapping.\n" +
                            "3. Simply tap on a color to set both a small region and its target color.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updatedField = field.copy(
                    key = key,
                    label = label,
                    type = type,
                    threshold = threshold,
                    targetColor = targetColor,
                    weight = weight.toDoubleOrNull()
                )
                val dataWithUpdatedField = configData.updateField(category, index, updatedField)
                val finalData = if (newCategory != category) {
                    dataWithUpdatedField.moveField(category, index, newCategory)
                } else {
                    dataWithUpdatedField
                }
                onConfigDataChanged(finalData)
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDeleteField, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
