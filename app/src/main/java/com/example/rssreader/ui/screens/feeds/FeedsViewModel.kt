package com.example.rssreader.ui.screens.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rssreader.data.repository.RssRepository
import com.example.rssreader.domain.model.Feed
import com.example.rssreader.domain.model.Folder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedsViewModel(private val repository: RssRepository) : ViewModel() {

    data class UiState(
        val folders: List<Folder> = emptyList(),
        val feedsWithoutFolder: List<Feed> = emptyList(),
        val feedsByFolder: Map<Long, List<Feed>> = emptyMap(),
        val totalUnreadCount: Int = 0,
        val isRefreshing: Boolean = false,
        val error: String? = null
    )

    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UiState> = combine(
        repository.getAllFolders(),
        repository.getFeedsWithoutFolder(),
        repository.getAllFeeds(),
        repository.getUnreadCount(),
        _isRefreshing,
        _error
    ) { flows ->
        val folders = flows[0] as List<Folder>
        val feedsWithoutFolder = flows[1] as List<Feed>
        val allFeeds = flows[2] as List<Feed>
        val unreadCount = flows[3] as Int
        val isRefreshing = flows[4] as Boolean
        val error = flows[5] as String?

        val feedsByFolder = folders.associate { folder ->
            folder.id to allFeeds.filter { it.folderId == folder.id }
        }

        UiState(
            folders = folders,
            feedsWithoutFolder = feedsWithoutFolder,
            feedsByFolder = feedsByFolder,
            totalUnreadCount = unreadCount,
            isRefreshing = isRefreshing,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    private val _expandedFolders = MutableStateFlow<Set<Long>>(emptySet())
    val expandedFolders: StateFlow<Set<Long>> = _expandedFolders.asStateFlow()

    fun toggleFolderExpanded(folderId: Long) {
        _expandedFolders.value = if (folderId in _expandedFolders.value) {
            _expandedFolders.value - folderId
        } else {
            _expandedFolders.value + folderId
        }
    }

    fun refreshAllFeeds() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            try {
                repository.refreshAllFeeds()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteFeed(feedId: Long) {
        viewModelScope.launch {
            repository.deleteFeed(feedId)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name)
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
        }
    }

    fun moveFeedToFolder(feedId: Long, folderId: Long?) {
        viewModelScope.launch {
            repository.updateFeedFolder(feedId, folderId)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
