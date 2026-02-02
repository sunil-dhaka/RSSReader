package com.example.rssreader.domain.model

import com.example.rssreader.data.local.entity.FeedEntity

data class Feed(
    val id: Long = 0,
    val title: String,
    val feedUrl: String,
    val siteUrl: String? = null,
    val description: String? = null,
    val iconUrl: String? = null,
    val folderId: Long? = null,
    val lastFetchedAt: Long? = null,
    val errorMessage: String? = null,
    val unreadCount: Int = 0
) {
    fun toEntity(): FeedEntity = FeedEntity(
        id = id,
        title = title,
        feedUrl = feedUrl,
        siteUrl = siteUrl,
        description = description,
        iconUrl = iconUrl,
        folderId = folderId,
        lastFetchedAt = lastFetchedAt,
        errorMessage = errorMessage
    )

    companion object {
        fun fromEntity(entity: FeedEntity, unreadCount: Int = 0): Feed = Feed(
            id = entity.id,
            title = entity.title,
            feedUrl = entity.feedUrl,
            siteUrl = entity.siteUrl,
            description = entity.description,
            iconUrl = entity.iconUrl,
            folderId = entity.folderId,
            lastFetchedAt = entity.lastFetchedAt,
            errorMessage = entity.errorMessage,
            unreadCount = unreadCount
        )
    }
}
