package com.example.rssreader.ui.screens.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rssreader.data.repository.RssRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(
    private val context: Context,
    private val repository: RssRepository
) : ViewModel() {

    data class UiState(
        val refreshIntervalMinutes: Int = 60,
        val keepArticlesDays: Int = 30,
        val openInBrowser: Boolean = false,
        val darkMode: String = "system",
        val showImages: Boolean = true,
        val fontSize: Int = 16,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            _uiState.value = UiState(
                refreshIntervalMinutes = prefs[KEY_REFRESH_INTERVAL] ?: 60,
                keepArticlesDays = prefs[KEY_KEEP_DAYS] ?: 30,
                openInBrowser = prefs[KEY_OPEN_IN_BROWSER] ?: false,
                darkMode = prefs[KEY_DARK_MODE] ?: "system",
                showImages = prefs[KEY_SHOW_IMAGES] ?: true,
                fontSize = prefs[KEY_FONT_SIZE] ?: 16
            )
        }
    }

    fun setRefreshInterval(minutes: Int) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_REFRESH_INTERVAL] = minutes }
            _uiState.value = _uiState.value.copy(refreshIntervalMinutes = minutes)
        }
    }

    fun setKeepArticlesDays(days: Int) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_KEEP_DAYS] = days }
            _uiState.value = _uiState.value.copy(keepArticlesDays = days)
        }
    }

    fun setOpenInBrowser(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_OPEN_IN_BROWSER] = enabled }
            _uiState.value = _uiState.value.copy(openInBrowser = enabled)
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_DARK_MODE] = mode }
            _uiState.value = _uiState.value.copy(darkMode = mode)
        }
    }

    fun setShowImages(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_SHOW_IMAGES] = enabled }
            _uiState.value = _uiState.value.copy(showImages = enabled)
        }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_FONT_SIZE] = size }
            _uiState.value = _uiState.value.copy(fontSize = size)
        }
    }

    fun cleanupOldArticles() {
        viewModelScope.launch {
            repository.cleanupOldArticles(_uiState.value.keepArticlesDays)
            _uiState.value = _uiState.value.copy(message = "Old articles cleaned up")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    companion object {
        private val KEY_REFRESH_INTERVAL = intPreferencesKey("refresh_interval")
        private val KEY_KEEP_DAYS = intPreferencesKey("keep_days")
        private val KEY_OPEN_IN_BROWSER = booleanPreferencesKey("open_in_browser")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_SHOW_IMAGES = booleanPreferencesKey("show_images")
        private val KEY_FONT_SIZE = intPreferencesKey("font_size")
    }
}
