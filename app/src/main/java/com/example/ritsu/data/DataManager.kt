package com.example.ritsu.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class ExportData(
    val exportTimestamp: Long,
    val scores: List<FullScoreRecordSerializable>
)

@Serializable
data class FullScoreRecordSerializable(
    val genericScore: GenericScoreSerializable,
    val details: List<ScoreDetailSerializable>,
    val gameName: String // Essential for re-linking on import
)

@Serializable
data class GenericScoreSerializable(
    val songTitle: String,
    val difficultyName: String,
    val difficultyVal: String,
    val difficultySortValue: Double,
    val totalScore: Long,
    val maxCombo: Int,
    val accuracy: Double,
    val playRank: String,
    val playTimestamp: Long,
    val importTimestamp: Long
)

@Serializable
data class ScoreDetailSerializable(
    val key: String,
    val value: String,
    val category: String
)

object DataManager {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun exportToJson(context: Context, uri: Uri, configs: List<GameConfig>, scores: List<FullScoreRecord>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val configMap = configs.associateBy { it.id }
                val serializableScores = scores.map { record ->
                    val gameName = configMap[record.genericScore.configId]?.gameName ?: "Unknown"
                    FullScoreRecordSerializable(
                        genericScore = GenericScoreSerializable(
                            songTitle = record.genericScore.songTitle,
                            difficultyName = record.genericScore.difficultyName,
                            difficultyVal = record.genericScore.difficultyVal,
                            difficultySortValue = record.genericScore.difficultySortValue,
                            totalScore = record.genericScore.totalScore,
                            maxCombo = record.genericScore.maxCombo,
                            accuracy = record.genericScore.accuracy,
                            playRank = record.genericScore.playRank,
                            playTimestamp = record.genericScore.playTimestamp,
                            importTimestamp = record.genericScore.importTimestamp
                        ),
                        details = record.details.map { detail ->
                            ScoreDetailSerializable(detail.key, detail.value, detail.category)
                        },
                        gameName = gameName
                    )
                }

                val exportData = ExportData(System.currentTimeMillis(), serializableScores)
                val jsonString = json.encodeToString(exportData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Data exported to JSON", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError(context, "Export failed: ${e.message}")
            }
        }
    }

    fun exportToCsv(context: Context, uri: Uri, configs: List<GameConfig>, scores: List<FullScoreRecord>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val configMap = configs.associateBy { it.id }
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                
                val csvContent = StringBuilder()
                // Headers
                csvContent.append("Game,Date,Song,Difficulty,Level,Score,Combo,Accuracy,Rank,Details\n")

                scores.forEach { record ->
                    val gameName = configMap[record.genericScore.configId]?.gameName ?: "Unknown"
                    val score = record.genericScore
                    val dateStr = sdf.format(Date(score.playTimestamp))
                    
                    val detailsStr = record.details.joinToString("; ") { "${it.key}: ${it.value}" }
                    
                    val line = listOf(
                        gameName,
                        dateStr,
                        score.songTitle,
                        score.difficultyName,
                        score.difficultyVal,
                        score.totalScore.toString(),
                        score.maxCombo.toString(),
                        "${score.accuracy}%",
                        score.playRank,
                        detailsStr
                    ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                    
                    csvContent.append("$line\n")
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toString().toByteArray())
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Data exported to CSV", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError(context, "CSV Export failed: ${e.message}")
            }
        }
    }

    fun handleImport(context: Context, uri: Uri?) {
        if (uri == null) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = RitsuDatabase.getDatabase(context)
                val dao = database.scoreDao()
                val currentConfigs = dao.getAllConfigs().first()
                val nameToConfigId = currentConfigs.associate { it.gameName to it.id }

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonString = reader.readText()
                    val data = json.decodeFromString<ExportData>(jsonString)

                    var importedCount = 0
                    data.scores.forEach { record ->
                        val targetConfigId = nameToConfigId[record.gameName]
                        if (targetConfigId != null) {
                            val scoreId = dao.insertScore(GenericScore(
                                configId = targetConfigId,
                                songTitle = record.genericScore.songTitle,
                                difficultyName = record.genericScore.difficultyName,
                                difficultyVal = record.genericScore.difficultyVal,
                                difficultySortValue = record.genericScore.difficultySortValue,
                                totalScore = record.genericScore.totalScore,
                                maxCombo = record.genericScore.maxCombo,
                                accuracy = record.genericScore.accuracy,
                                playRank = record.genericScore.playRank,
                                playTimestamp = record.genericScore.playTimestamp,
                                importTimestamp = System.currentTimeMillis()
                            ))
                            
                            val details = record.details.map { detail ->
                                ScoreDetail(
                                    scoreId = scoreId,
                                    key = detail.key,
                                    value = detail.value,
                                    category = detail.category
                                )
                            }
                            dao.insertDetails(details)
                            importedCount++
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Imported $importedCount scores", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError(context, "Import failed: ${e.message}")
            }
        }
    }

    private suspend fun showError(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
