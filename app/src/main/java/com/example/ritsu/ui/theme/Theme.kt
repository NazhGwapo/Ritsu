package com.example.ritsu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.ritsu.data.AppThemeMode
import com.example.ritsu.data.ColorPalette
import com.example.ritsu.data.ThemeConfig

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = PurpleGrey80,
    onSecondary = Color(0xFF332D41),
    tertiary = Pink80,
    onTertiary = Color(0xFF492532),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    tertiary = Pink40,
    onTertiary = Color.White,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

@Composable
fun RitsuTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeConfig.themeMode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }

    val colorScheme = when (themeConfig.colorPalette) {
        ColorPalette.Dynamic -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        ColorPalette.Custom -> {
            val seed = if (darkTheme) Color(themeConfig.customDarkColor) else Color(themeConfig.customLightColor)
            val onSeed = if (seed.luminance() > 0.5) Color.Black else Color.White
            
            if (darkTheme) {
                darkColorScheme(
                    primary = seed,
                    onPrimary = onSeed,
                    primaryContainer = seed.copy(alpha = 0.2f),
                    onPrimaryContainer = seed,
                    secondary = lerp(seed, Color.White, 0.4f),
                    onSecondary = Color.Black,
                    background = lerp(Color(0xFF121212), seed, 0.04f),
                    surface = lerp(Color(0xFF141218), seed, 0.08f),
                    onBackground = Color(0xFFE6E1E5),
                    onSurface = Color(0xFFE6E1E5),
                    surfaceVariant = lerp(Color(0xFF1D1B20), seed, 0.12f),
                    onSurfaceVariant = Color(0xFFCAC4D0),
                    outline = Color(0xFF938F99)
                )
            } else {
                lightColorScheme(
                    primary = seed,
                    onPrimary = onSeed,
                    primaryContainer = seed.copy(alpha = 0.15f),
                    onPrimaryContainer = seed,
                    secondary = lerp(seed, Color.Black, 0.3f),
                    onSecondary = Color.White,
                    background = lerp(Color(0xFFFFFBFE), seed, 0.02f),
                    surface = lerp(Color(0xFFFFFBFE), seed, 0.05f),
                    onBackground = Color(0xFF1C1B1F),
                    onSurface = Color(0xFF1C1B1F),
                    surfaceVariant = lerp(Color(0xFFE7E0EC), seed, 0.1f),
                    onSurfaceVariant = Color(0xFF49454F),
                    outline = Color(0xFF79747E)
                )
            }
        }
        ColorPalette.Default -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
