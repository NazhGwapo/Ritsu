package com.example.ritsu.ui.screens

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.ritsu.data.AppThemeMode
import com.example.ritsu.data.ColorPalette
import com.example.ritsu.data.ThemeConfig
import com.example.ritsu.data.ThemeRepository
import kotlinx.coroutines.launch

@Composable
fun ThemeScreen(themeRepository: ThemeRepository) {
    val themeConfig by themeRepository.themeConfig.collectAsState(initial = ThemeConfig())
    val scope = rememberCoroutineScope()

    ThemeScreenContent(
        themeConfig = themeConfig,
        onThemeModeSelected = { mode ->
            scope.launch { themeRepository.updateThemeMode(mode) }
        },
        onColorPaletteSelected = { palette ->
            scope.launch { themeRepository.updateColorPalette(palette) }
        },
        onCustomLightColorSelected = { color ->
            scope.launch { themeRepository.updateCustomLightColor(color) }
        },
        onCustomDarkColorSelected = { color ->
            scope.launch { themeRepository.updateCustomDarkColor(color) }
        }
    )
}

@Composable
fun ThemeScreenContent(
    themeConfig: ThemeConfig,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    onColorPaletteSelected: (ColorPalette) -> Unit,
    onCustomLightColorSelected: (Int) -> Unit,
    onCustomDarkColorSelected: (Int) -> Unit
) {
    val customColors = listOf(
        Color(0xFF6650a4), // Purple
        Color(0xFF6750A4), // Deep Purple
        Color(0xFF914331), // Deep Orange
        Color(0xFF386a20), // Green
        Color(0xFF006a6a), // Teal
        Color(0xFF006780), // Blue
        Color(0xFF7d5260), // Pink
        Color(0xFF625b71), // Purple Grey
        Color(0xFFbf360c), // Red
        Color(0xFF0d47a1), // Navy Blue
        Color(0xFF1b5e20), // Dark Green
        Color(0xFFfbc02d)  // Yellow
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
            Column {
                AppThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeModeSelected(mode)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeConfig.themeMode == mode,
                            onClick = {
                                onThemeModeSelected(mode)
                            }
                        )
                        Text(mode.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            Text("Color Palette", style = MaterialTheme.typography.titleMedium)
            Column {
                val palettes = mutableListOf<ColorPalette>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    palettes.add(ColorPalette.Dynamic)
                }
                palettes.add(ColorPalette.Default)
                palettes.add(ColorPalette.Custom)

                palettes.forEach { palette ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onColorPaletteSelected(palette)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeConfig.colorPalette == palette,
                            onClick = {
                                onColorPaletteSelected(palette)
                            }
                        )
                        val label = when (palette) {
                            ColorPalette.Dynamic -> "Dynamic Color (Material You)"
                            ColorPalette.Default -> "Default App Theme"
                            ColorPalette.Custom -> "Custom Color Scheme"
                        }
                        Text(label, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        
                        // Theme Preview
                        val isPreviewDark = when (themeConfig.themeMode) {
                            AppThemeMode.System -> isSystemInDarkTheme()
                            AppThemeMode.Light -> false
                            AppThemeMode.Dark -> true
                        }
                        
                        val previewColors = when (palette) {
                            ColorPalette.Default -> if (isPreviewDark) {
                                Triple(
                                    Color(0xFFD0BCFF), // Purple80
                                    Color(0xFFCCC2DC), // PurpleGrey80
                                    Color(0xFF1C1B1F)  // Dark background
                                )
                            } else {
                                Triple(
                                    Color(0xFF6650a4), // Purple40
                                    Color(0xFF625b71), // PurpleGrey40
                                    Color(0xFFFFFBFE)  // Light background
                                )
                            }
                            ColorPalette.Custom -> {
                                val seed = if (isPreviewDark) Color(themeConfig.customDarkColor) else Color(themeConfig.customLightColor)
                                if (isPreviewDark) {
                                    Triple(
                                        seed,
                                        lerp(seed, Color.White, 0.4f),
                                        lerp(Color(0xFF121212), seed, 0.04f)
                                    )
                                } else {
                                    Triple(
                                        seed,
                                        lerp(seed, Color.Black, 0.3f),
                                        lerp(Color(0xFFFFFBFE), seed, 0.02f)
                                    )
                                }
                            }
                            else -> null // Dynamic or other
                        }
                        
                        if (previewColors != null) {
                            ThemePreview(
                                primary = previewColors.first,
                                secondary = previewColors.second,
                                background = previewColors.third
                            )
                        } else if (palette == ColorPalette.Dynamic) {
                            // Placeholder for Dynamic
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Auto", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (themeConfig.colorPalette == ColorPalette.Custom) {
            item {
                Text("Custom Color (Light Mode)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                ColorGrid(
                    colors = customColors,
                    selectedColor = Color(themeConfig.customLightColor),
                    onColorSelected = { color ->
                        onCustomLightColorSelected(color.toArgb())
                    }
                )
            }

            item {
                Text("Custom Color (Dark Mode)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                ColorGrid(
                    colors = customColors,
                    selectedColor = Color(themeConfig.customDarkColor),
                    onColorSelected = { color ->
                        onCustomDarkColorSelected(color.toArgb())
                    }
                )
            }
        }
    }
}

@Composable
fun ThemePreview(
    primary: Color,
    secondary: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(48.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(primary)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(4.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(secondary)
        )
    }
}

@Composable
fun ColorGrid(
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Box(modifier = Modifier.height(120.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors.size) { index ->
                val color = colors[index]
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == color) 3.dp else 0.dp,
                            color = if (selectedColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ThemeScreenPreview() {
    com.example.ritsu.ui.theme.RitsuTheme {
        ThemeScreenContent(
            themeConfig = ThemeConfig(),
            onThemeModeSelected = {},
            onColorPaletteSelected = {},
            onCustomLightColorSelected = {},
            onCustomDarkColorSelected = {}
        )
    }
}
