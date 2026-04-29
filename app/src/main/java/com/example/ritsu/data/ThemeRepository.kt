package com.example.ritsu.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
        val CUSTOM_LIGHT_COLOR = intPreferencesKey("custom_light_color")
        val CUSTOM_DARK_COLOR = intPreferencesKey("custom_dark_color")
    }

    val themeConfig: Flow<ThemeConfig> = context.dataStore.data.map { preferences ->
        val themeMode = AppThemeMode.valueOf(
            preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.System.name,
        )
        val colorPalette = ColorPalette.valueOf(
            preferences[PreferencesKeys.COLOR_PALETTE] ?: ColorPalette.Default.name,
        )
        val customLightColor = preferences[PreferencesKeys.CUSTOM_LIGHT_COLOR] ?: ThemeConfig().customLightColor
        val customDarkColor = preferences[PreferencesKeys.CUSTOM_DARK_COLOR] ?: ThemeConfig().customDarkColor

        ThemeConfig(themeMode, colorPalette, customLightColor, customDarkColor,)
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateColorPalette(palette: ColorPalette) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLOR_PALETTE] = palette.name
        }
    }

    suspend fun updateCustomLightColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_LIGHT_COLOR] = color
        }
    }

    suspend fun updateCustomDarkColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_DARK_COLOR] = color
        }
    }
}
