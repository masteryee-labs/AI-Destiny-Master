package com.aidestinymaster.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// IMPORTANT: Keep a single DataStore instance per file by declaring the delegate at top-level.
private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

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

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LANG = stringPreferencesKey("lang")
        val THEME = stringPreferencesKey("theme")
        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        // Accessibility
        val FONT_SCALE = stringPreferencesKey("font_scale") // small/normal/large/extra
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        // In-app review counter
        val REVIEW_SUCCESS_COUNT = intPreferencesKey("review_success_count")
        val REVIEW_PROMPTED = booleanPreferencesKey("review_prompted")
    }

    // Flows
    val onboardingDoneFlow: Flow<Boolean> =
        appContext.userPrefsDataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    val langFlow: Flow<String> =
        appContext.userPrefsDataStore.data.map { it[Keys.LANG] ?: "zh-TW" }

    val themeFlow: Flow<String> =
        appContext.userPrefsDataStore.data.map { it[Keys.THEME] ?: "system" }

    val notifEnabledFlow: Flow<Boolean> =
        appContext.userPrefsDataStore.data.map { it[Keys.NOTIF_ENABLED] ?: false }

    val syncEnabledFlow: Flow<Boolean> =
        appContext.userPrefsDataStore.data.map { it[Keys.SYNC_ENABLED] ?: false }

    // Accessibility flows
    val fontScaleFlow: Flow<String> =
        appContext.userPrefsDataStore.data.map { it[Keys.FONT_SCALE] ?: "normal" }

    val reduceMotionFlow: Flow<Boolean> =
        appContext.userPrefsDataStore.data.map { it[Keys.REDUCE_MOTION] ?: false }

    // Review flows
    val reviewSuccessCountFlow: Flow<Int> =
        appContext.userPrefsDataStore.data.map { it[Keys.REVIEW_SUCCESS_COUNT] ?: 0 }

    val reviewPromptedFlow: Flow<Boolean> =
        appContext.userPrefsDataStore.data.map { it[Keys.REVIEW_PROMPTED] ?: false }

    // Setters
    suspend fun setOnboardingDone(done: Boolean) {
        appContext.userPrefsDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setLang(lang: String) {
        appContext.userPrefsDataStore.edit { it[Keys.LANG] = lang }
    }

    suspend fun setTheme(theme: String) {
        appContext.userPrefsDataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun setNotifEnabled(enabled: Boolean) {
        appContext.userPrefsDataStore.edit { it[Keys.NOTIF_ENABLED] = enabled }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        appContext.userPrefsDataStore.edit { it[Keys.SYNC_ENABLED] = enabled }
    }

    // Accessibility setters
    suspend fun setFontScale(scale: String) {
        appContext.userPrefsDataStore.edit { it[Keys.FONT_SCALE] = scale }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        appContext.userPrefsDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    // Review setters
    suspend fun incrementReviewSuccessCount() {
        appContext.userPrefsDataStore.edit { prefs ->
            val cur = prefs[Keys.REVIEW_SUCCESS_COUNT] ?: 0
            prefs[Keys.REVIEW_SUCCESS_COUNT] = cur + 1
        }
    }

    suspend fun setReviewPrompted(prompted: Boolean) {
        appContext.userPrefsDataStore.edit { it[Keys.REVIEW_PROMPTED] = prompted }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPrefsRepository? = null

        fun from(context: Context): UserPrefsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPrefsRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}

