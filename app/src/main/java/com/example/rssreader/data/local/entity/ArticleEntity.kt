package com.example.rssreader.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feedId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["feedId"]),
        Index(value = ["guid", "feedId"], unique = true),
        Index(value = ["isRead"]),
        Index(value = ["isStarred"]),
        Index(value = ["publishedAt"])
    ]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String?,
    val author: String?,
    val content: String?,
    val summary: String?,
    val imageUrl: String?,
    val publishedAt: Long?,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
