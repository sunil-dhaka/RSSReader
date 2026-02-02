package com.example.rssreader.data.repository

import com.example.rssreader.data.local.dao.ArticleDao
import com.example.rssreader.data.local.dao.FeedDao
import com.example.rssreader.data.local.dao.FolderDao
import com.example.rssreader.data.local.entity.ArticleEntity
import com.example.rssreader.data.local.entity.FeedEntity
import com.example.rssreader.data.local.entity.FolderEntity
import com.example.rssreader.domain.model.Article
import com.example.rssreader.domain.model.Feed
import com.example.rssreader.domain.model.Folder
import com.example.rssreader.network.RssParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RssRepository(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val folderDao: FolderDao,
    private val rssParser: RssParser
) {

    fun getAllFeeds(): Flow<List<Feed>> {
        return feedDao.getAllFeeds().map { feeds ->
            feeds.map { feed ->
                val unreadCount = articleDao.getUnreadCountByFeed(feed.id).first()
                Feed.fromEntity(feed, unreadCount)
            }
        }
    }

    fun getFeedsWithoutFolder(): Flow<List<Feed>> {
        return feedDao.getFeedsWithoutFolder().map { feeds ->
            feeds.map { feed ->
                val unreadCount = articleDao.getUnreadCountByFeed(feed.id).first()
                Feed.fromEntity(feed, unreadCount)
            }
        }
    }

    fun getFeedsByFolder(folderId: Long): Flow<List<Feed>> {
        return feedDao.getFeedsByFolder(folderId).map { feeds ->
            feeds.map { feed ->
                val unreadCount = articleDao.getUnreadCountByFeed(feed.id).first()
                Feed.fromEntity(feed, unreadCount)
            }
        }
    }

    suspend fun getFeedById(id: Long): Feed? {
        return feedDao.getFeedById(id)?.let { Feed.fromEntity(it) }
    }

    suspend fun addFeed(url: String): Result<Feed> {
        return try {
            val existingFeed = feedDao.getFeedByUrl(url)
            if (existingFeed != null) {
                return Result.failure(Exception("Feed already exists"))
            }

            val parsed = rssParser.parseFeed(url)
            val feedEntity = FeedEntity(
                title = parsed.title,
                feedUrl = url,
                siteUrl = parsed.siteUrl,
                description = parsed.description,
                iconUrl = parsed.iconUrl,
                lastFetchedAt = System.currentTimeMillis()
            )

            val feedId = feedDao.insert(feedEntity)
            if (feedId == -1L) {
                return Result.failure(Exception("Failed to insert feed"))
            }

            val articles = parsed.articles.map { article ->
                ArticleEntity(
                    feedId = feedId,
                    guid = article.guid,
                    title = article.title,
                    link = article.link,
                    author = article.author,
                    content = article.content,
                    summary = article.summary,
                    imageUrl = article.imageUrl,
                    publishedAt = article.publishedAt
                )
            }
            articleDao.insertAll(articles)

            Result.success(Feed.fromEntity(feedEntity.copy(id = feedId), articles.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshFeed(feedId: Long): Result<Int> {
        return try {
            val feed = feedDao.getFeedById(feedId)
                ?: return Result.failure(Exception("Feed not found"))

            val parsed = rssParser.parseFeed(feed.feedUrl)

            val newArticles = parsed.articles.mapNotNull { article ->
                val existing = articleDao.getArticleByGuid(article.guid, feedId)
                if (existing == null) {
                    ArticleEntity(
                        feedId = feedId,
                        guid = article.guid,
                        title = article.title,
                        link = article.link,
                        author = article.author,
                        content = article.content,
                        summary = article.summary,
                        imageUrl = article.imageUrl,
                        publishedAt = article.publishedAt
                    )
                } else null
            }

            if (newArticles.isNotEmpty()) {
                articleDao.insertAll(newArticles)
            }

            feedDao.updateLastFetched(feedId, System.currentTimeMillis())
            feedDao.updateError(feedId, null)

            Result.success(newArticles.size)
        } catch (e: Exception) {
            feedDao.updateError(feedId, e.message)
            Result.failure(e)
        }
    }

    suspend fun refreshAllFeeds(): Map<Long, Result<Int>> {
        val feeds = feedDao.getAllFeeds().first()
        return feeds.associate { feed ->
            feed.id to refreshFeed(feed.id)
        }
    }

    suspend fun deleteFeed(feedId: Long) {
        feedDao.deleteById(feedId)
    }

    suspend fun updateFeedFolder(feedId: Long, folderId: Long?) {
        feedDao.updateFolder(feedId, folderId)
    }

    fun getAllArticles(): Flow<List<Article>> {
        return combine(
            articleDao.getAllArticles(),
            feedDao.getAllFeeds()
        ) { articles, feeds ->
            val feedMap = feeds.associateBy { it.id }
            articles.map { article ->
                Article.fromEntity(article, feedMap[article.feedId]?.title)
            }
        }
    }

    fun getUnreadArticles(): Flow<List<Article>> {
        return combine(
            articleDao.getUnreadArticles(),
            feedDao.getAllFeeds()
        ) { articles, feeds ->
            val feedMap = feeds.associateBy { it.id }
            articles.map { article ->
                Article.fromEntity(article, feedMap[article.feedId]?.title)
            }
        }
    }

    fun getStarredArticles(): Flow<List<Article>> {
        return combine(
            articleDao.getStarredArticles(),
            feedDao.getAllFeeds()
        ) { articles, feeds ->
            val feedMap = feeds.associateBy { it.id }
            articles.map { article ->
                Article.fromEntity(article, feedMap[article.feedId]?.title)
            }
        }
    }

    fun getArticlesByFeed(feedId: Long): Flow<List<Article>> {
        return combine(
            articleDao.getArticlesByFeed(feedId),
            feedDao.getAllFeeds()
        ) { articles, feeds ->
            val feedMap = feeds.associateBy { it.id }
            articles.map { article ->
                Article.fromEntity(article, feedMap[article.feedId]?.title)
            }
        }
    }

    fun getArticlesByFolder(folderId: Long): Flow<List<Article>> {
        return combine(
            articleDao.getArticlesByFolder(folderId),
            feedDao.getAllFeeds()
        ) { articles, feeds ->
            val feedMap = feeds.associateBy { it.id }
            articles.map { article ->
                Article.fromEntity(article, feedMap[article.feedId]?.title)
            }
        }
    }

    suspend fun getArticleById(id: Long): Article? {
        return articleDao.getArticleById(id)?.let { entity ->
            val feed = feedDao.getFeedById(entity.feedId)
            Article.fromEntity(entity, feed?.title)
        }
    }

    suspend fun toggleArticleRead(articleId: Long) {
        val article = articleDao.getArticleById(articleId) ?: return
        articleDao.updateReadStatus(articleId, !article.isRead)
    }

    suspend fun markArticleAsRead(articleId: Long) {
        articleDao.updateReadStatus(articleId, true)
    }

    suspend fun toggleArticleStar(articleId: Long) {
        val article = articleDao.getArticleById(articleId) ?: return
        articleDao.updateStarredStatus(articleId, !article.isStarred)
    }

    suspend fun markAllAsReadByFeed(feedId: Long) {
        articleDao.markAllAsReadByFeed(feedId)
    }

    suspend fun markAllAsRead() {
        articleDao.markAllAsRead()
    }

    fun getUnreadCount(): Flow<Int> = articleDao.getUnreadCount()

    fun searchArticles(query: String): Flow<List<Article>> {
        return combine(
            articleDao.searchArticles(query),
            feedDao.getAllFeeds()
        ) { articles, feeds ->
            val feedMap = feeds.associateBy { it.id }
            articles.map { article ->
                Article.fromEntity(article, feedMap[article.feedId]?.title)
            }
        }
    }

    suspend fun cleanupOldArticles(daysToKeep: Int = 30) {
        val cutoff = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        articleDao.deleteOldArticles(cutoff)
    }

    fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { folders ->
            folders.map { Folder.fromEntity(it) }
        }
    }

    suspend fun createFolder(name: String): Result<Folder> {
        return try {
            val existing = folderDao.getFolderByName(name)
            if (existing != null) {
                return Result.failure(Exception("Folder already exists"))
            }
            val entity = FolderEntity(name = name)
            val id = folderDao.insert(entity)
            Result.success(Folder.fromEntity(entity.copy(id = id)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFolder(folderId: Long) {
        feedDao.getFeedsByFolder(folderId).first().forEach { feed ->
            feedDao.updateFolder(feed.id, null)
        }
        folderDao.deleteById(folderId)
    }

    suspend fun renameFolder(folderId: Long, newName: String): Result<Unit> {
        return try {
            val folder = folderDao.getFolderById(folderId)
                ?: return Result.failure(Exception("Folder not found"))
            folderDao.update(folder.copy(name = newName))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
