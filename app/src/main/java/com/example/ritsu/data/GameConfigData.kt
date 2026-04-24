package com.example.ritsu.data

import kotlinx.serialization.Serializable

@Serializable
data class OcrRect(
    val x: Float, // Top-left X (0.0 - 1.0)
    val y: Float, // Top-left Y (0.0 - 1.0)
    val w: Float, // Width (0.0 - 1.0)
    val h: Float  // Height (0.0 - 1.0)
)

@Serializable
data class ConfigField(
    val key: String,
    val label: String,
    val type: String = "number",
    val ocrRect: OcrRect? = null // Optional: if null, OCR ignores this field
)

@Serializable
data class GameConfigData(
    val gameName: String,
    val configVersion: Int = 1,
    val judgments: List<ConfigField> = emptyList(),
    val metrics: List<ConfigField> = emptyList(),
    val misc: List<ConfigField> = emptyList(),
    val formula: String? = null,
    // Add specific rects for the "Generic" table data
    val titleRect: OcrRect? = null,
    val scoreRect: OcrRect? = null,
    val comboRect: OcrRect? = null,
    val difficultyNameRect: OcrRect? = null,
    val difficultyValRect: OcrRect? = null,
    val rankRect: OcrRect? = null
) {
    val allFieldsWithCategory: List<Pair<ConfigField, String>>
        get() = judgments.map { it to "Judgment" } +
                metrics.map { it to "Metric" } +
                misc.map { it to "Misc" }
}
