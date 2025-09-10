package com.aidestinymaster.app.report

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson

private val Context.reportStore by preferencesDataStore(name = "report_prefs")

object ReportPrefs {
    private val FAVS = preferencesKey<String>("fav_ids")
    private val NOTES = preferencesKey<String>("notes_json")
    private val gson = Gson()

    fun favsFlow(context: Context): Flow<Set<String>> = context.reportStore.data.map { prefs ->
        val raw = prefs[FAVS] ?: "[]"
        runCatching { gson.fromJson(raw, Array<String>::class.java).toSet() }.getOrDefault(emptySet())
    }

    fun notesFlow(context: Context): Flow<Map<String, String>> = context.reportStore.data.map { prefs ->
        val raw = prefs[NOTES] ?: "{}"
        runCatching { gson.fromJson(raw, Map::class.java) as Map<String, String> }.getOrDefault(emptyMap())
    }

    suspend fun toggleFav(context: Context, id: String) {
        context.reportStore.edit { prefs ->
            val cur = gson.fromJson(prefs[FAVS] ?: "[]", Array<String>::class.java)?.toMutableSet() ?: mutableSetOf()
            if (!cur.add(id)) cur.remove(id)
            prefs[FAVS] = gson.toJson(cur.toTypedArray())
        }
    }

    suspend fun setNote(context: Context, id: String, note: String) {
        context.reportStore.edit { prefs ->
            val cur = runCatching { gson.fromJson(prefs[NOTES] ?: "{}", Map::class.java) as Map<String, String> }.getOrDefault(emptyMap())
            val next = cur.toMutableMap()
            if (note.isBlank()) next.remove(id) else next[id] = note
            prefs[NOTES] = gson.toJson(next)
        }
    }
}

