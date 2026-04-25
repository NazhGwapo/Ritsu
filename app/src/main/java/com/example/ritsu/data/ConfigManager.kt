package com.example.ritsu.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

object ConfigManager {
    private val json = Json { ignoreUnknownKeys = true }

    fun handleImport(context: Context, uri: Uri?) {
        if (uri == null) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contentResolver = context.contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonString = reader.readText()
                    
                    val decoded = json.decodeFromString<GameConfigData>(jsonString)

                    val config = GameConfig(
                        gameName = decoded.gameName,
                        configData = jsonString,
                        configVersion = decoded.configVersion
                    )

                    val database = RitsuDatabase.getDatabase(context)
                    val dao = database.scoreDao()
                    
                    val existing = dao.getConfigByName(decoded.gameName)
                    val message = when {
                        existing == null -> {
                            dao.insertConfig(config)
                            "Imported ${decoded.gameName} (v${decoded.configVersion})"
                        }
                        decoded.configVersion > existing.configVersion -> {
                            dao.updateConfig(config.copy(id = existing.id, displayIconUri = existing.displayIconUri))
                            "Updated ${decoded.gameName} to v${decoded.configVersion}"
                        }
                        else -> "Config ${decoded.gameName} is already up to date (v${existing.configVersion})"
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun handleExport(context: Context, uri: Uri, configData: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(configData.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Configuration exported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
