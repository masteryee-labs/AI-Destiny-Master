package com.aidestinymaster.app.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aidestinymaster.data.prefs.UserPrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo by lazy { UserPrefsRepository.from(getApplication()) }
    private val _themeState = MutableStateFlow("system")
    val themeState = _themeState.asStateFlow()

    init {
        // Observe persisted theme and push into state
        viewModelScope.launch {
            repo.themeFlow.collect { t ->
                _themeState.value = t
            }
        }
    }

    fun setTheme(theme: String) {
        _themeState.value = theme
        viewModelScope.launch { repo.setTheme(theme) }
    }
}
