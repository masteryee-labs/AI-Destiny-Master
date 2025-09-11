package com.aidestinymaster.app.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aidestinymaster.app.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(app: Application) : AndroidViewModel(app) {
    private val _themeState = MutableStateFlow("system")
    val themeState = _themeState.asStateFlow()

    init {
        // Observe persisted theme and push into state
        viewModelScope.launch {
            UserPrefs.themeFlow(app).collect { t ->
                _themeState.value = t
            }
        }
    }

    fun setTheme(theme: String) {
        val app = getApplication<Application>()
        _themeState.value = theme
        viewModelScope.launch { UserPrefs.setTheme(app, theme) }
    }
}
