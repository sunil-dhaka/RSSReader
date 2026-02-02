package com.example.rssreader.ui.screens.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rssreader.data.repository.RssRepository
import com.example.rssreader.domain.model.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArticlesViewModel(private val repository: RssRepository) : ViewModel() {

    enum class FilterType {
        ALL, UNREAD, STARRED, FEED, FOLDER, SEARCH
    }

    data class UiState(
        val articles: List<Article> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val filterType: FilterType = FilterType.ALL,
        val feedId: Long? = null,
        val folderId: Long? = null,
        val searchQuery: String = "",
        val feedTitle: String? = null
    )

    private val _filterType = MutableStateFlow(FilterType.ALL)
    private val _feedId = MutableStateFlow<Long?>(null)
    private val _folderId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _feedTitle = MutableStateFlow<String?>(null)

    private val articles = combine(
        _filterType,
        _feedId,
        _folderId,
        _searchQuery
    ) { filterType, feedId, folderId, searchQuery ->
        FilterParams(filterType, feedId, folderId, searchQuery)
    }.flatMapLatest { params ->
        when (params.filterType) {
            FilterType.ALL -> repository.getAllArticles()
            FilterType.UNREAD -> repository.getUnreadArticles()
            FilterType.STARRED -> repository.getStarredArticles()
            FilterType.FEED -> params.feedId?.let { repository.getArticlesByFeed(it) } ?: flowOf(emptyList())
            FilterType.FOLDER -> params.folderId?.let { repository.getArticlesByFolder(it) } ?: flowOf(emptyList())
            FilterType.SEARCH -> repository.searchArticles(params.searchQuery)
        }
    }

    val uiState: StateFlow<UiState> = combine(
        articles,
        _isRefreshing,
        _error,
        _filterType,
        _feedId,
        _folderId,
        _searchQuery,
        _feedTitle
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        UiState(
            articles = values[0] as List<Article>,
            isLoading = false,
            isRefreshing = values[1] as Boolean,
            error = values[2] as String?,
            filterType = values[3] as FilterType,
            feedId = values[4] as Long?,
            folderId = values[5] as Long?,
            searchQuery = values[6] as String,
            feedTitle = values[7] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    fun setFilterAll() {
        _filterType.value = FilterType.ALL
        _feedId.value = null
        _folderId.value = null
        _feedTitle.value = null
    }

    fun setFilterUnread() {
        _filterType.value = FilterType.UNREAD
        _feedId.value = null
        _folderId.value = null
        _feedTitle.value = null
    }

    fun setFilterStarred() {
        _filterType.value = FilterType.STARRED
        _feedId.value = null
        _folderId.value = null
        _feedTitle.value = null
    }

    fun setFilterByFeed(feedId: Long) {
        _filterType.value = FilterType.FEED
        _feedId.value = feedId
        _folderId.value = null
        viewModelScope.launch {
            val feed = repository.getFeedById(feedId)
            _feedTitle.value = feed?.title
        }
    }

    fun setFilterByFolder(folderId: Long) {
        _filterType.value = FilterType.FOLDER
        _feedId.value = null
        _folderId.value = folderId
        _feedTitle.value = null
    }

    fun setSearchQuery(query: String) {
        _filterType.value = FilterType.SEARCH
        _searchQuery.value = query
        _feedId.value = null
        _folderId.value = null
        _feedTitle.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            try {
                val feedId = _feedId.value
                if (feedId != null) {
                    repository.refreshFeed(feedId)
                } else {
                    repository.refreshAllFeeds()
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleRead(articleId: Long) {
        viewModelScope.launch {
            repository.toggleArticleRead(articleId)
        }
    }

    fun toggleStar(articleId: Long) {
        viewModelScope.launch {
            repository.toggleArticleStar(articleId)
        }
    }

    fun markAsRead(articleId: Long) {
        viewModelScope.launch {
            repository.markArticleAsRead(articleId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val feedId = _feedId.value
            if (feedId != null) {
                repository.markAllAsReadByFeed(feedId)
            } else {
                repository.markAllAsRead()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private data class FilterParams(
        val filterType: FilterType,
        val feedId: Long?,
        val folderId: Long?,
        val searchQuery: String
    )
}
