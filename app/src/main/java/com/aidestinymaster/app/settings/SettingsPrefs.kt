package com.aidestinymaster.app.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings_prefs")

object SettingsPrefs {
    private val KEY_LANGUAGE = stringPreferencesKey("language") // system | zh | en
    private val KEY_HOUSES = stringPreferencesKey("houses")     // ASC | ARIES | PLACIDUS
    private val KEY_HIGH_LAT = stringPreferencesKey("high_lat_fallback") // ASC | REGIO
    private val KEY_DIAGNOSTICS = stringPreferencesKey("astro_diagnostics") // true | false

    data class Settings(
        val language: String = "system",
        val houses: String = "ASC",
        val highLatFallback: String = "ASC",
        val diagnostics: Boolean = false
    )

    fun flow(context: Context): Flow<Settings> = context.settingsStore.data.map { p: Preferences ->
        Settings(
            language = p[KEY_LANGUAGE] ?: "system",
            houses = p[KEY_HOUSES] ?: "ASC",
            highLatFallback = p[KEY_HIGH_LAT] ?: "ASC",
            diagnostics = (p[KEY_DIAGNOSTICS] ?: "false").toBoolean()
        )
    }

    suspend fun setLanguage(context: Context, language: String) {
        context.settingsStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setHouses(context: Context, houses: String) {
        context.settingsStore.edit { it[KEY_HOUSES] = houses }
    }

    suspend fun setHighLatFallback(context: Context, mode: String) {
        context.settingsStore.edit { it[KEY_HIGH_LAT] = mode }
    }

    suspend fun setDiagnostics(context: Context, enabled: Boolean) {
        context.settingsStore.edit { it[KEY_DIAGNOSTICS] = enabled.toString() }
    }
}
