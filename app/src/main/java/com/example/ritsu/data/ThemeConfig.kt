package com.example.ritsu.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class AppThemeMode {
    System,
    Light,
    Dark
}

enum class ColorPalette {
    Dynamic,
    Default,
    Custom
}

data class ThemeConfig(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val colorPalette: ColorPalette = ColorPalette.Default,
    val customLightColor: Int = Color(0xFF6650a4).toArgb(), // Default Purple40
    val customDarkColor: Int = Color(0xFFD0BCFF).toArgb()   // Default Purple80
)
