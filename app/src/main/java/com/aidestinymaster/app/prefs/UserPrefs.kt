package com.aidestinymaster.app.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPrefs {
    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    // New preference keys
    private val KEY_LANG = stringPreferencesKey("lang")
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
    private val KEY_SYNC_ENABLED = booleanPreferencesKey("sync_enabled")

    fun onboardingDoneFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(context: Context, done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    // Language (default zh-TW)
    fun langFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_LANG] ?: "zh-TW" }

    suspend fun setLang(context: Context, lang: String) {
        context.dataStore.edit { it[KEY_LANG] = lang }
    }

    // Theme (default system)
    fun themeFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_THEME] ?: "system" }

    suspend fun setTheme(context: Context, theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    // Notifications enabled (default false)
    fun notifEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_NOTIF_ENABLED] ?: false }

    suspend fun setNotifEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_ENABLED] = enabled }
    }

    // Sync enabled (default false)
    fun syncEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SYNC_ENABLED] ?: false }

    suspend fun setSyncEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_SYNC_ENABLED] = enabled }
    }
}

