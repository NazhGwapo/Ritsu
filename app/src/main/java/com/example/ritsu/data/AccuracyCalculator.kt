package com.example.ritsu.data

object AccuracyCalculator {
    /**
     * Calculates accuracy based on judgement counts and their configured weights.
     * @param extractedData Map of field keys to extracted string values.
     * @param config The game configuration containing field weights.
     * @return Calculated accuracy (0.0 - 100.0) or null if not enough data.
     */
    fun calculate(extractedData: Map<String, String>, config: GameConfigData): Double? {
        val judgments = config.judgments
        if (judgments.isEmpty()) return null

        var totalNotes = 0
        var weightedHits = 0.0
        var foundAny = false

        judgments.forEach { field ->
            val countStr = extractedData[field.key] ?: return@forEach
            val count = countStr.filter { it.isDigit() }.toIntOrNull() ?: 0
            val weight = field.weight ?: 0.0
            
            totalNotes += count
            weightedHits += count * weight
            foundAny = true
        }

        if (!(foundAny) || totalNotes == 0) return null

        return (weightedHits / totalNotes) * 100.0
    }


    /**
     * Calculates the total note count from a list of ScoreDetail based on judgments.
     */
    fun getTotalNoteCount(details: List<ScoreDetail>, config: GameConfigData): Int {
        val judgments = config.judgments
        if (judgments.isEmpty()) return 0

        var totalNotes = 0
        judgments.forEach { field ->
            val detail = details.find { it.key == field.key } ?: return@forEach
            val count = detail.value.filter { it.isDigit() }.toIntOrNull() ?: 0
            totalNotes += count
        }
        return totalNotes
    }
}
