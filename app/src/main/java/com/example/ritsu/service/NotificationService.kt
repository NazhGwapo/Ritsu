package com.example.ritsu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.ritsu.MainActivity
import com.example.ritsu.R
import com.example.ritsu.data.AccuracyCalculator
import com.example.ritsu.data.GameConfig
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.GenericScore
import com.example.ritsu.data.OCRManager
import com.example.ritsu.data.RitsuDatabase
import com.example.ritsu.data.ScoreDetail
import com.example.ritsu.data.ScoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class NotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "RitsuCaptureChannel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START_PROJECTION = "com.example.ritsu.START_PROJECTION"
        const val ACTION_UPDATE_CONFIG = "com.example.ritsu.UPDATE_CONFIG"
        const val ACTION_CAPTURE = "com.example.ritsu.CAPTURE"
        const val ACTION_EXIT = "com.example.ritsu.EXIT"

        const val EXTRA_PROJECTION_RESULT_CODE = "projection_result_code"
        const val EXTRA_PROJECTION_DATA = "projection_data"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private var activeConfig: GameConfig? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val ocrManager = OCRManager()
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadInitialConfig()
    }

    private fun loadInitialConfig() {
        serviceScope.launch {
            val database = RitsuDatabase.getDatabase(this@NotificationService)
            activeConfig = database.scoreDao().getAllConfigs().first().firstOrNull()
            updateNotification()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_EXIT) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Show notification and promote to foreground service immediately
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        when (intent?.action) {
            ACTION_START_PROJECTION -> {
                val resultCode = intent.getIntExtra(EXTRA_PROJECTION_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
                }
                if (resultCode != 0 && data != null) {
                    startProjection(resultCode, data)
                }
            }
            ACTION_UPDATE_CONFIG -> {
                val configId = intent.getLongExtra(ServiceControlActivity.EXTRA_CONFIG_ID, -1L)
                if (configId != -1L) {
                    serviceScope.launch {
                        val database = RitsuDatabase.getDatabase(this@NotificationService)
                        activeConfig = database.scoreDao().getAllConfigs().first().find { it.id == configId }
                        updateNotification()
                        Toast.makeText(this@NotificationService, "Config updated: ${activeConfig?.gameName}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ACTION_CAPTURE -> {
                captureScreen()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startProjection(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, data)
        
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                virtualDisplay?.release()
                imageReader?.close()
                mediaProjection = null
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun captureScreen() {
        if (mediaProjection == null) {
            Toast.makeText(this, "MediaProjection not started", Toast.LENGTH_SHORT).show()
            return
        }
        if (activeConfig == null) {
            Toast.makeText(this, "No active config selected", Toast.LENGTH_SHORT).show()
            return
        }

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val width: Int
        val height: Int
        val density: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
            density = resources.displayMetrics.densityDpi
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
            density = metrics.densityDpi
        }

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "RitsuCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            imageReader?.surface,
            null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = try { reader.acquireLatestImage() } catch (_: Exception) { null }
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                
                val actualWidth = rowStride / pixelStride
                val bitmap = Bitmap.createBitmap(
                    actualWidth,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                image.close()
                
                // Process the captured bitmap
                processCapturedBitmap(finalBitmap)
                
                // Cleanup
                virtualDisplay?.release()
                virtualDisplay = null
                reader.close()
                imageReader = null
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun processCapturedBitmap(bitmap: Bitmap) {
        val config = activeConfig ?: return
        serviceScope.launch {
            try {
                val configData = json.decodeFromString<GameConfigData>(config.configData)
                val extractedData = ocrManager.processImage(bitmap, configData)

                // Same logic as in NavigationComponent
                var accuracyVal = if (configData.useAccuracyOcr) extractedData["accuracy"]?.toDoubleOrNull() else null
                if (accuracyVal == null) {
                    accuracyVal = AccuracyCalculator.calculate(extractedData, configData) ?: 0.0
                }

                val score = GenericScore(
                    configId = config.id,
                    songTitle = extractedData["songTitle"] ?: "Unknown",
                    difficultyName = extractedData["difficultyName"] ?: "Unknown",
                    difficultyVal = extractedData["difficultyVal"] ?: "0.0",
                    difficultySortValue = GenericScore.parseDifficulty(extractedData["difficultyVal"] ?: "0.0"),
                    totalScore = extractedData["totalScore"]?.filter { it.isDigit() }?.toLongOrNull() ?: 0L,
                    maxCombo = extractedData["maxCombo"]?.filter { it.isDigit() }?.toIntOrNull() ?: 0,
                    accuracy = accuracyVal,
                    playRank = if (configData.useRankOcr) (extractedData["playRank"] ?: "N/A") else "",
                    playTimestamp = System.currentTimeMillis(),
                    importTimestamp = System.currentTimeMillis()
                )

                val details = configData.allFieldsWithCategory.map { (field, category) ->
                    ScoreDetail(
                        scoreId = 0,
                        key = field.key,
                        value = extractedData[field.key] ?: "",
                        category = category
                    )
                }

                val database = RitsuDatabase.getDatabase(this@NotificationService)
                val repository = ScoreRepository(database.scoreDao())
                repository.saveFullScore(score, details)
                
                Toast.makeText(this@NotificationService, "Score Captured: ${score.songTitle}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@NotificationService, "Capture failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNotification() {
        val notification = createNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val changeConfigIntent = Intent(this, ServiceControlActivity::class.java).apply {
            putExtra(ServiceControlActivity.EXTRA_COMMAND, ServiceControlActivity.COMMAND_CHANGE_CONFIG)
        }
        val changeConfigPendingIntent = PendingIntent.getActivity(
            this, 1, changeConfigIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val captureIntent = Intent(this, ServiceControlActivity::class.java).apply {
            putExtra(ServiceControlActivity.EXTRA_COMMAND, ServiceControlActivity.COMMAND_CAPTURE)
        }
        val capturePendingIntent = PendingIntent.getActivity(
            this, 2, captureIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val exitIntent = Intent(this, NotificationService::class.java).apply {
            action = ACTION_EXIT
        }
        val exitPendingIntent = PendingIntent.getService(
            this, 3, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val configName = activeConfig?.gameName ?: "No Config Selected"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ritsu Capture Service")
            .setContentText("Active Config: $configName")
            .setSmallIcon(R.drawable.ohnoes)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .addAction(0, "Change Config", changeConfigPendingIntent)
            .addAction(0, "Capture", capturePendingIntent)
            .addAction(0, "Exit", exitPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Ritsu Capture Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        serviceScope.launch { Job().cancel() }
    }
}
