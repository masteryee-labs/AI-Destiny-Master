package com.aidestinymaster.app.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aidestinymaster.data.repository.UserRepository
import com.aidestinymaster.sync.GoogleAuthManager
import com.aidestinymaster.sync.SyncBatchScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val userRepo by lazy { UserRepository.from(app) }

    private val _syncEnabled = MutableStateFlow(false)
    val syncEnabled = _syncEnabled.asStateFlow()

    private val _email = MutableStateFlow<String?>(null)
    val email = _email.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        val u = userRepo.get() ?: userRepo.ensure()
        _syncEnabled.value = u.syncEnabled
        _email.value = GoogleAuthManager.signIn(getApplication())?.email
    }

    suspend fun setSyncEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        try {
            userRepo.toggleSync(enabled)
            _syncEnabled.value = enabled
            if (enabled) SyncBatchScheduler.schedule(getApplication()) else SyncBatchScheduler.cancel(getApplication())
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    fun onSignedIn(email: String?) {
        _email.value = email
    }

    fun clearError() { _error.value = null }
}

