package com.example.rssreader.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feeds",
    indices = [Index(value = ["feedUrl"], unique = true)]
)
data class FeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val feedUrl: String,
    val siteUrl: String?,
    val description: String?,
    val iconUrl: String?,
    val folderId: Long? = null,
    val lastFetchedAt: Long? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
