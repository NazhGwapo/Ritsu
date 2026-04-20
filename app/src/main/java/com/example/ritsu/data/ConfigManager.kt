package com.example.ritsu.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object ConfigManager {
    fun handleImport(context: Context, uri: Uri?) {
        if (uri == null) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contentResolver = context.contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonString = reader.readText()
                    val jsonObject = JSONObject(jsonString)

                    val name = jsonObject.getString("gameName")
                    val version = jsonObject.optInt("configVersion", 1)

                    val config = GameConfig(
                        gameName = name,
                        configData = jsonString,
                        configVersion = version
                    )

                    val database = RitsuDatabase.getDatabase(context)
                    val dao = database.scoreDao()
                    
                    val existing = dao.getConfigByName(name)
                    val message = when {
                        existing == null -> {
                            dao.insertConfig(config)
                            "Imported $name (v$version)"
                        }
                        version > existing.configVersion -> {
                            dao.updateConfig(config.copy(id = existing.id))
                            "Updated $name to v$version"
                        }
                        else -> "Config $name is already up to date (v${existing.configVersion})"
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
}
