package com.example.rssreader.domain.model

import com.example.rssreader.data.local.entity.FolderEntity

data class Folder(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val feedCount: Int = 0,
    val unreadCount: Int = 0
) {
    fun toEntity(): FolderEntity = FolderEntity(
        id = id,
        name = name,
        sortOrder = sortOrder
    )

    companion object {
        fun fromEntity(entity: FolderEntity): Folder = Folder(
            id = entity.id,
            name = entity.name,
            sortOrder = entity.sortOrder
        )
    }
}
