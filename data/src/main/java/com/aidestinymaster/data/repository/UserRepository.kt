package com.aidestinymaster.data.repository

import android.content.Context
import com.aidestinymaster.data.db.DatabaseProvider
import com.aidestinymaster.data.db.UserProfileDao
import com.aidestinymaster.data.db.UserProfileEntity

class UserRepository private constructor(private val dao: UserProfileDao) {
    suspend fun ensure(): UserProfileEntity {
        val cur = dao.get()
        if (cur != null) return cur
        val def = UserProfileEntity(name = null, lang = "zh-TW", theme = "system", syncEnabled = false)
        dao.upsert(def)
        return def
    }

    suspend fun toggleSync(enabled: Boolean) {
        val p = ensure().copy(syncEnabled = enabled)
        dao.upsert(p)
    }

    suspend fun setLanguage(lang: String) {
        val p = ensure().copy(lang = lang)
        dao.upsert(p)
    }

    suspend fun updateProfile(name: String?, theme: String) {
        val p = ensure().copy(name = name, theme = theme)
        dao.upsert(p)
    }

    suspend fun get(): UserProfileEntity? = dao.get()

    companion object {
        fun from(context: Context): UserRepository = UserRepository(DatabaseProvider.get(context).userProfileDao())
    }
}

