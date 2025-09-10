package com.aidestinymaster.app.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPrefs {
    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

    fun onboardingDoneFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(context: Context, done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }
}

