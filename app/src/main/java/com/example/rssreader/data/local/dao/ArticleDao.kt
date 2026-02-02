package com.example.rssreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.rssreader.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("""
        SELECT * FROM articles
        ORDER BY publishedAt DESC, createdAt DESC
    """)
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("""
        SELECT * FROM articles
        WHERE isRead = 0
        ORDER BY publishedAt DESC, createdAt DESC
    """)
    fun getUnreadArticles(): Flow<List<ArticleEntity>>

    @Query("""
        SELECT * FROM articles
        WHERE isStarred = 1
        ORDER BY publishedAt DESC, createdAt DESC
    """)
    fun getStarredArticles(): Flow<List<ArticleEntity>>

    @Query("""
        SELECT * FROM articles
        WHERE feedId = :feedId
        ORDER BY publishedAt DESC, createdAt DESC
    """)
    fun getArticlesByFeed(feedId: Long): Flow<List<ArticleEntity>>

    @Query("""
        SELECT * FROM articles
        WHERE feedId = :feedId AND isRead = 0
        ORDER BY publishedAt DESC, createdAt DESC
    """)
    fun getUnreadArticlesByFeed(feedId: Long): Flow<List<ArticleEntity>>

    @Query("""
        SELECT a.* FROM articles a
        INNER JOIN feeds f ON a.feedId = f.id
        WHERE f.folderId = :folderId
        ORDER BY a.publishedAt DESC, a.createdAt DESC
    """)
    fun getArticlesByFolder(folderId: Long): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Long): ArticleEntity?

    @Query("SELECT * FROM articles WHERE guid = :guid AND feedId = :feedId")
    suspend fun getArticleByGuid(guid: String, feedId: Long): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(article: ArticleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<ArticleEntity>): List<Long>

    @Update
    suspend fun update(article: ArticleEntity)

    @Query("UPDATE articles SET isRead = :isRead WHERE id = :id")
    suspend fun updateReadStatus(id: Long, isRead: Boolean)

    @Query("UPDATE articles SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateStarredStatus(id: Long, isStarred: Boolean)

    @Query("UPDATE articles SET isRead = 1 WHERE feedId = :feedId")
    suspend fun markAllAsReadByFeed(feedId: Long)

    @Query("UPDATE articles SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("SELECT COUNT(*) FROM articles WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles WHERE feedId = :feedId AND isRead = 0")
    fun getUnreadCountByFeed(feedId: Long): Flow<Int>

    @Query("""
        DELETE FROM articles
        WHERE isStarred = 0
        AND createdAt < :olderThan
    """)
    suspend fun deleteOldArticles(olderThan: Long)

    @Query("DELETE FROM articles WHERE feedId = :feedId")
    suspend fun deleteByFeed(feedId: Long)

    @Query("""
        SELECT * FROM articles
        WHERE title LIKE '%' || :query || '%'
        OR content LIKE '%' || :query || '%'
        OR summary LIKE '%' || :query || '%'
        ORDER BY publishedAt DESC
    """)
    fun searchArticles(query: String): Flow<List<ArticleEntity>>
}
