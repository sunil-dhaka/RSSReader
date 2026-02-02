package com.example.rssreader.domain.model

import com.example.rssreader.data.local.entity.ArticleEntity

data class Article(
    val id: Long = 0,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String? = null,
    val author: String? = null,
    val content: String? = null,
    val summary: String? = null,
    val imageUrl: String? = null,
    val publishedAt: Long? = null,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val feedTitle: String? = null
) {
    fun toEntity(): ArticleEntity = ArticleEntity(
        id = id,
        feedId = feedId,
        guid = guid,
        title = title,
        link = link,
        author = author,
        content = content,
        summary = summary,
        imageUrl = imageUrl,
        publishedAt = publishedAt,
        isRead = isRead,
        isStarred = isStarred
    )

    companion object {
        fun fromEntity(entity: ArticleEntity, feedTitle: String? = null): Article = Article(
            id = entity.id,
            feedId = entity.feedId,
            guid = entity.guid,
            title = entity.title,
            link = entity.link,
            author = entity.author,
            content = entity.content,
            summary = entity.summary,
            imageUrl = entity.imageUrl,
            publishedAt = entity.publishedAt,
            isRead = entity.isRead,
            isStarred = entity.isStarred,
            feedTitle = feedTitle
        )
    }
}
