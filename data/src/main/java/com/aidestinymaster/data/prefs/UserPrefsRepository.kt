package com.aidestinymaster.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UserPrefsRepository
 *
 * A thin repository wrapper around DataStore Preferences so that upper layers (e.g., :app)
 * do not need to touch DataStore APIs directly. This improves testability and reuse.
 *
 * Keys aligned with:
 *  - PREF_LANG -> "lang"
 *  - PREF_THEME -> "theme"
 *  - PREF_NOTIF_ENABLED -> "notif_enabled"
 *  - PREF_SYNC_ENABLED -> "sync_enabled"
 *  - onboarding_done (internal)
 */
class UserPrefsRepository private constructor(private val appContext: Context) {
    private val Context.dataStore by preferencesDataStore(name = "user_prefs")

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LANG = stringPreferencesKey("lang")
        val THEME = stringPreferencesKey("theme")
        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    }

    // Flows
    val onboardingDoneFlow: Flow<Boolean> =
        appContext.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    val langFlow: Flow<String> =
        appContext.dataStore.data.map { it[Keys.LANG] ?: "zh-TW" }

    val themeFlow: Flow<String> =
        appContext.dataStore.data.map { it[Keys.THEME] ?: "system" }

    val notifEnabledFlow: Flow<Boolean> =
        appContext.dataStore.data.map { it[Keys.NOTIF_ENABLED] ?: false }

    val syncEnabledFlow: Flow<Boolean> =
        appContext.dataStore.data.map { it[Keys.SYNC_ENABLED] ?: false }

    // Setters
    suspend fun setOnboardingDone(done: Boolean) {
        appContext.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setLang(lang: String) {
        appContext.dataStore.edit { it[Keys.LANG] = lang }
    }

    suspend fun setTheme(theme: String) {
        appContext.dataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun setNotifEnabled(enabled: Boolean) {
        appContext.dataStore.edit { it[Keys.NOTIF_ENABLED] = enabled }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        appContext.dataStore.edit { it[Keys.SYNC_ENABLED] = enabled }
    }

    companion object {
        fun from(context: Context): UserPrefsRepository =
            UserPrefsRepository(context.applicationContext)
    }
}
