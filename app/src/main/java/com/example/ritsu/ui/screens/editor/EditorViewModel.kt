package com.example.ritsu.ui.screens.editor

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import com.example.ritsu.data.ConfigField
import com.example.ritsu.data.GameConfigData
import com.example.ritsu.data.OcrRect
import com.example.ritsu.ui.screens.EditorMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditorViewModel : ViewModel() {
    private val _configData = MutableStateFlow<GameConfigData?>(null)
    val configData: StateFlow<GameConfigData?> = _configData.asStateFlow()

    private val _selectedKey = MutableStateFlow<String?>(null)
    val selectedKey: StateFlow<String?> = _selectedKey.asStateFlow()

    private val _mode = MutableStateFlow(EditorMode.EDIT)
    val mode: StateFlow<EditorMode> = _mode.asStateFlow()

    // Transform State
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    
    // UI State
    var containerSize by mutableStateOf(Size.Zero)
    var imageSize by mutableStateOf(Size.Zero)
    var bitmap: Bitmap? by mutableStateOf(null)

    fun setConfigData(data: GameConfigData?) {
        _configData.value = data
    }

    fun setSelectedKey(key: String?) {
        _selectedKey.value = key
    }

    fun setMode(newMode: EditorMode) {
        _mode.value = newMode
    }

    fun updateConfigData(newData: GameConfigData) {
        _configData.value = newData
    }

    fun updateRect(key: String, rect: OcrRect?, targetColor: Int? = null) {
        _configData.value = _configData.value?.updateRect(key, rect, targetColor)
    }

    fun updateField(category: String, index: Int, field: ConfigField) {
        _configData.value = _configData.value?.updateField(category, index, field)
    }

    fun addField(category: String) {
        _configData.value = _configData.value?.addField(category)
        _mode.value = EditorMode.EDIT
    }

    fun removeField(key: String) {
        _configData.value = _configData.value?.removeFieldByKey(key)
        _selectedKey.value = null
    }

    // Helper to convert screen coordinates to normalized coordinates (0.0 - 1.0)
    fun screenToNormalized(screenOffset: Offset): Offset {
        if ((imageSize.width == 0f) || (imageSize.height == 0f)) return Offset.Zero
        
        // Remove scale and translation
        val canvasX = (screenOffset.x - offset.x) / scale
        val canvasY = (screenOffset.y - offset.y) / scale
        
        // Center the image within the container coordinate system
        val centeredX = canvasX - ((containerSize.width - imageSize.width) / 2)
        val centeredY = canvasY - ((containerSize.height - imageSize.height) / 2)
        
        return Offset(
            x = (centeredX / imageSize.width).coerceIn(0f, 1f),
            y = (centeredY / imageSize.height).coerceIn(0f, 1f),
        )
    }

    // Helper to convert normalized coordinates to screen coordinates
    fun normalizedToScreen(normOffset: Offset): Offset {
        val centeredX = normOffset.x * imageSize.width
        val centeredY = normOffset.y * imageSize.height
        
        val canvasX = centeredX + ((containerSize.width - imageSize.width) / 2)
        val canvasY = centeredY + ((containerSize.height - imageSize.height) / 2)
        
        return Offset(
            x = canvasX * scale + offset.x,
            y = canvasY * scale + offset.y,
        )
    }
}
