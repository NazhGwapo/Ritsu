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
    val type: String = "number", // "number", "text", "boolean"
    val ocrRect: OcrRect? = null,
    val targetColor: Int? = null, // ARGB color to detect if type is "boolean"
    val threshold: Float = 0.1f,    // Threshold for color matching (0.0 - 1.0)
    val weight: Double? = null,      // Weight for accuracy calculation (e.g., 1.0 for Perfect)
    val shortLabel: String? = null  // Shortened label for display in compact views
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
    val accuracyRect: OcrRect? = null,
    val difficultyNameRect: OcrRect? = null,
    val difficultyValRect: OcrRect? = null,
    val rankRect: OcrRect? = null,
    val useRankOcr: Boolean = true
) {
    val allFieldsWithCategory: List<Pair<ConfigField, String>>
        get() = judgments.map { it to "Judgment" } +
                metrics.map { it to "Metric" } +
                misc.map { it to "Misc" }

    /**
     * Updates a field's properties based on its category and index.
     */
    fun updateField(category: String, index: Int, updatedField: ConfigField): GameConfigData {
        return when (category) {
            "Judgment" -> copy(judgments = judgments.toMutableList().apply { set(index, updatedField) })
            "Metric" -> copy(metrics = metrics.toMutableList().apply { set(index, updatedField) })
            "Misc" -> copy(misc = misc.toMutableList().apply { set(index, updatedField) })
            else -> this
        }
    }

    fun updateFieldByKey(key: String, updatedField: ConfigField): GameConfigData {
        val category = when {
            key.startsWith("judgment_") -> "Judgment"
            key.startsWith("metric_") -> "Metric"
            key.startsWith("misc_") -> "Misc"
            else -> return this
        }
        val index = key.substringAfter("_").toIntOrNull() ?: return this
        return updateField(category, index, updatedField)
    }

    /**
     * Adds a new field to a category.
     */
    fun addField(category: String): GameConfigData {
        val newField = ConfigField(key = "new_key", label = "New Field")
        return when (category) {
            "Judgment" -> copy(judgments = judgments + newField)
            "Metric" -> copy(metrics = metrics + newField)
            "Misc" -> copy(misc = misc + newField)
            else -> this
        }
    }

    /**
     * Removes a field from a category at the specified index.
     */
    fun removeField(category: String, index: Int): GameConfigData {
        return when (category) {
            "Judgment" -> copy(judgments = judgments.toMutableList().apply { removeAt(index) })
            "Metric" -> copy(metrics = metrics.toMutableList().apply { removeAt(index) })
            "Misc" -> copy(misc = misc.toMutableList().apply { removeAt(index) })
            else -> this
        }
    }

    fun removeFieldByKey(key: String): GameConfigData {
        return when {
            key.startsWith("judgment_") -> {
                val index = key.substringAfter("judgment_").toIntOrNull()
                if (index != null) removeField("Judgment", index) else this
            }
            key.startsWith("metric_") -> {
                val index = key.substringAfter("metric_").toIntOrNull()
                if (index != null) removeField("Metric", index) else this
            }
            key.startsWith("misc_") -> {
                val index = key.substringAfter("misc_").toIntOrNull()
                if (index != null) removeField("Misc", index) else this
            }
            else -> this
        }
    }

    /**
     * Moves a field from one category to another.
     */
    fun moveField(fromCategory: String, index: Int, toCategory: String): GameConfigData {
        if (fromCategory == toCategory) return this
        
        val field = when (fromCategory) {
            "Judgment" -> judgments.getOrNull(index)
            "Metric" -> metrics.getOrNull(index)
            "Misc" -> misc.getOrNull(index)
            else -> null
        } ?: return this

        val removed = removeField(fromCategory, index)
        return when (toCategory) {
            "Judgment" -> removed.copy(judgments = removed.judgments + field)
            "Metric" -> removed.copy(metrics = removed.metrics + field)
            "Misc" -> removed.copy(misc = removed.misc + field)
            else -> this
        }
    }

    fun updateRect(key: String, rect: OcrRect?, targetColor: Int? = null): GameConfigData {
        return when {
            key == "titleRect" -> copy(titleRect = rect)
            key == "scoreRect" -> copy(scoreRect = rect)
            key == "comboRect" -> copy(comboRect = rect)
            key == "accuracyRect" -> copy(accuracyRect = rect)
            key == "difficultyNameRect" -> copy(difficultyNameRect = rect)
            key == "difficultyValRect" -> copy(difficultyValRect = rect)
            key == "rankRect" -> copy(rankRect = rect)
            key == "useRankOcr" -> copy(useRankOcr = rect != null) // Simplistic mapping for toggle
            key.startsWith("judgment_") -> {
                val index = key.substringAfter("judgment_").toIntOrNull()
                if (index != null && index in judgments.indices) {
                    val newList = judgments.toMutableList()
                    newList[index] = if (targetColor != null) {
                        newList[index].copy(ocrRect = rect, targetColor = targetColor)
                    } else {
                        newList[index].copy(ocrRect = rect)
                    }
                    copy(judgments = newList)
                } else this
            }
            key.startsWith("metric_") -> {
                val index = key.substringAfter("metric_").toIntOrNull()
                if (index != null && index in metrics.indices) {
                    val newList = metrics.toMutableList()
                    newList[index] = if (targetColor != null) {
                        newList[index].copy(ocrRect = rect, targetColor = targetColor)
                    } else {
                        newList[index].copy(ocrRect = rect)
                    }
                    copy(metrics = newList)
                } else this
            }
            key.startsWith("misc_") -> {
                val index = key.substringAfter("misc_").toIntOrNull()
                if (index != null && index in misc.indices) {
                    val newList = misc.toMutableList()
                    newList[index] = if (targetColor != null) {
                        newList[index].copy(ocrRect = rect, targetColor = targetColor)
                    } else {
                        newList[index].copy(ocrRect = rect)
                    }
                    copy(misc = newList)
                } else this
            }
            else -> this
        }
    }
}
