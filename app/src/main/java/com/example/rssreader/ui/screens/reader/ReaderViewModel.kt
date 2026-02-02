package com.example.rssreader.ui.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rssreader.data.repository.RssRepository
import com.example.rssreader.domain.model.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(private val repository: RssRepository) : ViewModel() {

    data class UiState(
        val article: Article? = null,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadArticle(articleId: Long) {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            try {
                val article = repository.getArticleById(articleId)
                if (article != null) {
                    repository.markArticleAsRead(articleId)
                    _uiState.value = UiState(article = article.copy(isRead = true), isLoading = false)
                } else {
                    _uiState.value = UiState(error = "Article not found", isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = UiState(error = e.message, isLoading = false)
            }
        }
    }

    fun toggleStar() {
        val article = _uiState.value.article ?: return
        viewModelScope.launch {
            repository.toggleArticleStar(article.id)
            _uiState.value = _uiState.value.copy(
                article = article.copy(isStarred = !article.isStarred)
            )
        }
    }

    fun toggleRead() {
        val article = _uiState.value.article ?: return
        viewModelScope.launch {
            repository.toggleArticleRead(article.id)
            _uiState.value = _uiState.value.copy(
                article = article.copy(isRead = !article.isRead)
            )
        }
    }
}
