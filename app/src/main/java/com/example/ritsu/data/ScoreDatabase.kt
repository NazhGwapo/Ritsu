package com.example.ritsu.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. THE ENTITIES (The Tables) ---

@Entity(
    tableName = "game_configs",
    indices = [Index(value = ["gameName"], unique = true)],
)
data class GameConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameName: String,
    // Store JSON representation of fields and formula
    val configData: String,
    val configVersion: Int = 1,
    val displayIconUri: String? = null
)

@Entity(
    tableName = "generic_scores",
    foreignKeys = [
        ForeignKey(
            entity = GameConfig::class,
            parentColumns = ["id"],
            childColumns = ["configId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["configId"])]
)
data class GenericScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: Long, // Links to the game template
    val songTitle: String,
    val difficultyName: String,
    val difficultyVal: String,
    val difficultySortValue: Double,
    val totalScore: Long,
    val maxCombo: Int,
    val accuracy: Double,
    val playRank: String,
    val timestamp: Long
) {
    companion object {
        fun parseDifficulty(diffStr: String): Double {
            val numericPart = diffStr.filter { (it.isDigit() || it == '.') }.toDoubleOrNull() ?: 0.0
            var bonus = 0.0
            if (diffStr.contains('+')) bonus += 0.5
            return numericPart + bonus
        }
    }
}

@Entity(
    tableName = "score_details",
    foreignKeys = [
        ForeignKey(
            entity = GenericScore::class,
            parentColumns = ["id"],
            childColumns = ["scoreId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scoreId"])]
)
data class ScoreDetail(
    @PrimaryKey(autoGenerate = true) val detailId: Long = 0,
    val scoreId: Long,
    val key: String,   // e.g., "Perfect"
    val value: String, // e.g., "450"
    val category: String // e.g., "Judgment", "Metric", "Misc"
)

// --- 2. THE RELATION (The "Full" Object) ---

data class FullScoreRecord(
    @Embedded val genericScore: GenericScore,
    @Relation(
        parentColumn = "id",
        entityColumn = "scoreId"
    )
    val details: List<ScoreDetail>
)

// --- 3. THE DAO (The Queries) ---

@Dao
interface ScoreDao {
    @Insert
    suspend fun insertConfig(config: GameConfig): Long

    @Update
    suspend fun updateConfig(config: GameConfig)

    @Query("SELECT * FROM game_configs WHERE gameName = :name LIMIT 1")
    suspend fun getConfigByName(name: String): GameConfig?

    @Transaction
    suspend fun upsertConfig(newConfig: GameConfig) {
        val existing = getConfigByName(newConfig.gameName)
        if (existing == null) {
            insertConfig(newConfig)
        } else if (newConfig.configVersion > existing.configVersion) {
            updateConfig(newConfig.copy(id = existing.id))
        }
    }

    @Insert
    suspend fun insertScore(score: GenericScore): Long

    @Insert
    suspend fun insertDetails(details: List<ScoreDetail>)

    @Transaction // Necessary because it queries multiple tables
    @Query("SELECT * FROM generic_scores ORDER BY timestamp DESC")
    fun getAllScores(): Flow<List<FullScoreRecord>>

    @Query("SELECT * FROM game_configs")
    fun getAllConfigs(): Flow<List<GameConfig>>

    @Delete
    suspend fun deleteConfig(config: GameConfig)

    @Query("DELETE FROM generic_scores")
    suspend fun deleteAllScores()

    @Query("DELETE FROM game_configs")
    suspend fun deleteAllConfigs()
}

// --- 4. THE DATABASE ---

@Database(entities = [GameConfig::class, GenericScore::class, ScoreDetail::class], version = 6)
abstract class RitsuDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        @Volatile
        private var INSTANCE: RitsuDatabase? = null

        fun getDatabase(context: android.content.Context): RitsuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RitsuDatabase::class.java,
                    "ritsu-database"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
