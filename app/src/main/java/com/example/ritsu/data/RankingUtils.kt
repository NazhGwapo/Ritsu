package com.example.ritsu.data

import com.example.ritsu.ui.cards.TopScoreItem
import kotlinx.serialization.json.Json

object RankingUtils {
    private val json = Json { ignoreUnknownKeys = true }

    fun calculateRankingValue(
        record: FullScoreRecord,
        config: GameConfig?,
        sortMode: String, // "Accuracy", "Max Combo", or "Score"
    ): Double {
        val totalNoteCount = if (config != null) {
            try {
                val cData = json.decodeFromString<GameConfigData>(config.configData)
                AccuracyCalculator.getTotalNoteCount(record.details, cData)
            } catch (_: Exception) { 0 }
        } else 0
        
        val weight = totalNoteCount.coerceAtLeast(1).toDouble()

        return when (sortMode) {
            "Accuracy" -> weight * record.genericScore.accuracy
            "Max Combo" -> weight * record.genericScore.maxCombo.toDouble()
            "Score" -> weight * record.genericScore.totalScore.toDouble()
            else -> weight * record.genericScore.accuracy
        }
    }

    fun mapToTopScoreItem(
        record: FullScoreRecord,
        config: GameConfig?,
        rank: Int = 0,
        playCount: Int = 0,
        showRank: Boolean = true
    ): TopScoreItem {
        val s = record.genericScore
        val labels = if (showRank) getBooleanLabels(record, config) else emptyList()
        return TopScoreItem(
            scoreId = s.id,
            rank = rank,
            songTitle = s.songTitle,
            difficultyName = s.difficultyName,
            difficultyVal = s.difficultyVal,
            accuracy = s.accuracy,
            maxCombo = s.maxCombo,
            playRank = s.playRank,
            gameName = config?.gameName ?: "Unknown",
            displayIconUri = config?.displayIconUri,
            totalScore = s.totalScore,
            playCount = playCount,
            booleanLabels = labels,
            showRank = showRank
        )
    }

    fun getBooleanLabels(record: FullScoreRecord, config: GameConfig?): List<String> {
        if (config == null) return emptyList()
        return try {
            val cData = json.decodeFromString<GameConfigData>(config.configData)
            val booleanFields = cData.allFieldsWithCategory.asSequence()
                .filter { it.first.type == "boolean" }
                .associate { it.first.key to it.first.label }
            
            record.details.asSequence()
                .filter { (booleanFields.containsKey(it.key)) && it.value.lowercase() == "true" }
                .map { booleanFields[it.key] ?: "" }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
