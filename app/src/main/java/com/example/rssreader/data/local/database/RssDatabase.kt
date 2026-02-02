package com.example.rssreader.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rssreader.data.local.dao.ArticleDao
import com.example.rssreader.data.local.dao.FeedDao
import com.example.rssreader.data.local.dao.FolderDao
import com.example.rssreader.data.local.entity.ArticleEntity
import com.example.rssreader.data.local.entity.FeedEntity
import com.example.rssreader.data.local.entity.FolderEntity

@Database(
    entities = [
        FeedEntity::class,
        ArticleEntity::class,
        FolderEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RssDatabase : RoomDatabase() {

    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao
    abstract fun folderDao(): FolderDao

    companion object {
        private const val DATABASE_NAME = "rss_reader.db"

        @Volatile
        private var instance: RssDatabase? = null

        fun getInstance(context: Context): RssDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): RssDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RssDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
