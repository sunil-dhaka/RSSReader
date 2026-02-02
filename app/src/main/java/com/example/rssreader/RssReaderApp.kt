package com.example.rssreader

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.rssreader.data.local.database.RssDatabase
import com.example.rssreader.data.repository.RssRepository
import com.example.rssreader.network.FeedDiscovery
import com.example.rssreader.network.RssParser
import com.example.rssreader.worker.SyncWorker
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class RssReaderApp : Application() {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    private val database: RssDatabase by lazy {
        RssDatabase.getInstance(this)
    }

    val rssParser: RssParser by lazy {
        RssParser(httpClient)
    }

    val feedDiscovery: FeedDiscovery by lazy {
        FeedDiscovery(httpClient)
    }

    val repository: RssRepository by lazy {
        RssRepository(
            feedDao = database.feedDao(),
            articleDao = database.articleDao(),
            folderDao = database.folderDao(),
            rssParser = rssParser
        )
    }

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicSync()
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        const val SYNC_WORK_NAME = "rss_sync_work"
    }
}
