package com.example.ritsu.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScoreRepository(private val scoreDao: ScoreDao) {

    /**
     * Saves a complete score record including its generic details and specific field values.
     * This handles the 1-to-N relationship between GenericScore and ScoreDetail.
     */
    suspend fun saveFullScore(genericScore: GenericScore, details: List<ScoreDetail>) {
        withContext(Dispatchers.IO) {
            // 1. Insert the main score and get its auto-generated ID
            val scoreId = scoreDao.insertScore(genericScore)

            // 2. Map the details to include the new scoreId
            val detailsWithId = details.map { it.copy(scoreId = scoreId) }

            // 3. Insert all the details linked to this score
            scoreDao.insertDetails(detailsWithId)
        }
    }

    /**
     * Retrieves all scores from the database with their associated details.
     */
    fun getAllScores(): Flow<List<FullScoreRecord>> {
        return scoreDao.getAllScores()
    }

    /**
     * Retrieves all game configurations.
     */
    fun getAllConfigs(): Flow<List<GameConfig>> {
        return scoreDao.getAllConfigs()
    }

}
