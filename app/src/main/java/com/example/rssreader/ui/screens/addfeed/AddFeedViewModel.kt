package com.example.rssreader.ui.screens.addfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rssreader.data.repository.RssRepository
import com.example.rssreader.domain.model.Folder
import com.example.rssreader.network.FeedDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddFeedViewModel(
    private val repository: RssRepository,
    private val feedDiscovery: FeedDiscovery
) : ViewModel() {

    data class UiState(
        val url: String = "",
        val selectedFolderId: Long? = null,
        val isLoading: Boolean = false,
        val isSuccess: Boolean = false,
        val error: String? = null,
        val discoveredFeeds: List<FeedDiscovery.DiscoveredFeed> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val folders: StateFlow<List<Folder>> = repository.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(url = url, error = null, discoveredFeeds = emptyList())
    }

    fun setSelectedFolder(folderId: Long?) {
        _uiState.value = _uiState.value.copy(selectedFolderId = folderId)
    }

    fun discoverFeeds() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val feeds = feedDiscovery.discoverFeeds(url)
                if (feeds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No feeds found at this URL"
                    )
                } else if (feeds.size == 1) {
                    addFeed(feeds.first().url)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        discoveredFeeds = feeds
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to discover feeds"
                )
            }
        }
    }

    fun addFeed(feedUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repository.addFeed(feedUrl)
                result.fold(
                    onSuccess = { feed ->
                        val folderId = _uiState.value.selectedFolderId
                        if (folderId != null) {
                            repository.updateFeedFolder(feed.id, folderId)
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to add feed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add feed"
                )
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun reset() {
        _uiState.value = UiState()
    }
}
