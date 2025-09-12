package com.aidestinymaster.app.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.aidestinymaster.data.prefs.UserPrefsRepository

// IMPORTANT: Do NOT create another DataStore instance here.
// Always delegate to :data module's UserPrefsRepository which holds a single DataStore per file.

object UserPrefs {
    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    // New preference keys
    private val KEY_LANG = stringPreferencesKey("lang")
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
    private val KEY_SYNC_ENABLED = booleanPreferencesKey("sync_enabled")

    fun onboardingDoneFlow(context: Context): Flow<Boolean> =
        UserPrefsRepository.from(context).onboardingDoneFlow

    suspend fun setOnboardingDone(context: Context, done: Boolean) {
        UserPrefsRepository.from(context).setOnboardingDone(done)
    }

    // Language (default zh-TW)
    fun langFlow(context: Context): Flow<String> =
        UserPrefsRepository.from(context).langFlow

    suspend fun setLang(context: Context, lang: String) {
        UserPrefsRepository.from(context).setLang(lang)
    }

    // Theme (default system)
    fun themeFlow(context: Context): Flow<String> =
        UserPrefsRepository.from(context).themeFlow

    suspend fun setTheme(context: Context, theme: String) {
        UserPrefsRepository.from(context).setTheme(theme)
    }

    // Notifications enabled (default false)
    fun notifEnabledFlow(context: Context): Flow<Boolean> =
        UserPrefsRepository.from(context).notifEnabledFlow

    suspend fun setNotifEnabled(context: Context, enabled: Boolean) {
        UserPrefsRepository.from(context).setNotifEnabled(enabled)
    }

    // Sync enabled (default false)
    fun syncEnabledFlow(context: Context): Flow<Boolean> =
        UserPrefsRepository.from(context).syncEnabledFlow

    suspend fun setSyncEnabled(context: Context, enabled: Boolean) {
        UserPrefsRepository.from(context).setSyncEnabled(enabled)
    }
}

