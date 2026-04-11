package com.example.ritsu.data

import androidx.room.*

// --- 1. THE ENTITIES (The Tables) ---@Entity(tableName = "game_configs")
data class GameConfig(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameName: String,
    // Store extra fields as a comma-separated string or JSON (e.g., "Perfect,Great,Good,Miss")
    val customFields: String
)

@androidx.room.Entity(
    tableName = "generic_scores",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = GameConfig::class,
            parentColumns = ["id"],
            childColumns = ["configId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class GenericScore(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: Long, // Links to the game template
    val songTitle: String,
    val difficultyName: String,
    val difficultyVal: Double,
    val totalScore: Long,
    val maxCombo: Int,
    val accuracy: Double,
    val timestamp: Long
)

@androidx.room.Entity(
    tableName = "score_details",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = GenericScore::class,
            parentColumns = ["id"],
            childColumns = ["scoreId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class ScoreDetail(
    @androidx.room.PrimaryKey(autoGenerate = true) val detailId: Long = 0,
    val scoreId: Long,
    val key: String,   // e.g., "Perfect"
    val value: String  // e.g., "450"
)

// --- 2. THE RELATION (The "Full" Object) ---

data class FullScoreRecord(
    @androidx.room.Embedded val genericScore: GenericScore,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "scoreId"
    )
    val details: List<ScoreDetail>
)

// --- 3. THE DAO (The Queries) ---

@androidx.room.Dao
interface ScoreDao {
    @androidx.room.Insert
    suspend fun insertConfig(config: GameConfig): Long

    @androidx.room.Insert
    suspend fun insertScore(score: GenericScore): Long

    @androidx.room.Insert
    suspend fun insertDetails(details: List<ScoreDetail>)

    @androidx.room.Transaction // Necessary because it queries multiple tables
    @androidx.room.Query("SELECT * FROM generic_scores ORDER BY timestamp DESC")
    fun getAllScores(): List<FullScoreRecord>

    @androidx.room.Query("SELECT * FROM game_configs")
    suspend fun getAllConfigs(): List<GameConfig>
}

// --- 4. THE DATABASE ---

@androidx.room.Database(entities = [GameConfig::class, GenericScore::class, ScoreDetail::class], version = 2)
abstract class RitsuDatabase : androidx.room.RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
}
