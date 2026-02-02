package com.example.rssreader.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.rssreader.data.local.entity.FeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {

    @Query("SELECT * FROM feeds ORDER BY title COLLATE NOCASE ASC")
    fun getAllFeeds(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feeds WHERE folderId IS NULL ORDER BY title COLLATE NOCASE ASC")
    fun getFeedsWithoutFolder(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feeds WHERE folderId = :folderId ORDER BY title COLLATE NOCASE ASC")
    fun getFeedsByFolder(folderId: Long): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun getFeedById(id: Long): FeedEntity?

    @Query("SELECT * FROM feeds WHERE feedUrl = :url")
    suspend fun getFeedByUrl(url: String): FeedEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(feed: FeedEntity): Long

    @Update
    suspend fun update(feed: FeedEntity)

    @Delete
    suspend fun delete(feed: FeedEntity)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE feeds SET lastFetchedAt = :timestamp, errorMessage = NULL WHERE id = :feedId")
    suspend fun updateLastFetched(feedId: Long, timestamp: Long)

    @Query("UPDATE feeds SET errorMessage = :error WHERE id = :feedId")
    suspend fun updateError(feedId: Long, error: String?)

    @Query("UPDATE feeds SET folderId = :folderId WHERE id = :feedId")
    suspend fun updateFolder(feedId: Long, folderId: Long?)
}
