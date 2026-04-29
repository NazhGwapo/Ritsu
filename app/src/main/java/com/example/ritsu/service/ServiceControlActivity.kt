package com.example.ritsu.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ritsu.data.RitsuDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ServiceControlActivity : ComponentActivity() {

    companion object {
        const val EXTRA_COMMAND = "command"
        const val COMMAND_START_SERVICE = "start_service"
        const val COMMAND_CHANGE_CONFIG = "change_config"
        const val COMMAND_CAPTURE = "capture"
        
        const val EXTRA_CONFIG_ID = "config_id"
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if ((result.resultCode == RESULT_OK) && result.data != null) {
            val intent = Intent(this, NotificationService::class.java).apply {
                action = NotificationService.ACTION_START_PROJECTION
                putExtra(NotificationService.EXTRA_PROJECTION_RESULT_CODE, result.resultCode)
                putExtra(NotificationService.EXTRA_PROJECTION_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        when (val command = intent.getStringExtra(EXTRA_COMMAND)) {
            COMMAND_START_SERVICE -> requestProjectionAndStart()
            COMMAND_CHANGE_CONFIG -> showConfigSelector()
            COMMAND_CAPTURE -> {
                val intent = Intent(this, NotificationService::class.java).apply {
                    action = NotificationService.ACTION_CAPTURE
                }
                startService(intent)
                finish()
            }
            else -> finish()
        }
    }

    private fun requestProjectionAndStart() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun showConfigSelector() {
        val database = RitsuDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.Main).launch {
            val configs = database.scoreDao().getAllConfigs().first()
            if (configs.isEmpty()) {
                finish()
                return@launch
            }

            val items = configs.map { it.gameName }.toTypedArray()
            android.app.AlertDialog.Builder(this@ServiceControlActivity)
                .setTitle("Select Game Config")
                .setItems(items) { _, which ->
                    val selected = configs[which]
                    val intent = Intent(this@ServiceControlActivity, NotificationService::class.java).apply {
                        action = NotificationService.ACTION_UPDATE_CONFIG
                        putExtra(EXTRA_CONFIG_ID, selected.id)
                    }
                    startService(intent)
                    finish()
                }
                .setOnCancelListener { finish() }
                .setOnDismissListener { if (!isFinishing) finish() }
                .show()
        }
    }
}
