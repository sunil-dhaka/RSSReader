package com.example.rssreader.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class FeedDiscovery(private val httpClient: OkHttpClient) {

    data class DiscoveredFeed(
        val url: String,
        val title: String?,
        val type: String
    )

    suspend fun discoverFeeds(url: String): List<DiscoveredFeed> = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url)
        val feeds = mutableListOf<DiscoveredFeed>()

        if (isProbablyFeedUrl(normalizedUrl)) {
            feeds.add(DiscoveredFeed(normalizedUrl, null, "direct"))
            return@withContext feeds
        }

        try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext feeds

            val contentType = response.header("Content-Type") ?: ""
            if (isFeedContentType(contentType)) {
                feeds.add(DiscoveredFeed(normalizedUrl, null, "direct"))
                return@withContext feeds
            }

            val doc = Jsoup.parse(body, normalizedUrl)

            doc.select("link[rel=alternate][type*=rss], link[rel=alternate][type*=atom]")
                .forEach { link ->
                    val href = link.attr("abs:href")
                    val title = link.attr("title").takeIf { it.isNotBlank() }
                    val type = link.attr("type")
                    if (href.isNotBlank()) {
                        feeds.add(DiscoveredFeed(href, title, type))
                    }
                }

            if (feeds.isEmpty()) {
                val commonPaths = listOf(
                    "/feed",
                    "/feed/",
                    "/rss",
                    "/rss.xml",
                    "/atom.xml",
                    "/feed.xml",
                    "/index.xml",
                    "/feeds/posts/default"
                )
                for (path in commonPaths) {
                    try {
                        val feedUrl = java.net.URL(java.net.URL(normalizedUrl), path).toString()
                        val feedRequest = Request.Builder()
                            .url(feedUrl)
                            .header("User-Agent", USER_AGENT)
                            .head()
                            .build()

                        val feedResponse = httpClient.newCall(feedRequest).execute()
                        if (feedResponse.isSuccessful) {
                            val feedContentType = feedResponse.header("Content-Type") ?: ""
                            if (isFeedContentType(feedContentType)) {
                                feeds.add(DiscoveredFeed(feedUrl, null, "discovered"))
                                break
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            if (isProbablyFeedUrl(normalizedUrl)) {
                feeds.add(DiscoveredFeed(normalizedUrl, null, "fallback"))
            }
        }

        feeds
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        return normalized
    }

    private fun isProbablyFeedUrl(url: String): Boolean {
        val lowercaseUrl = url.lowercase()
        return lowercaseUrl.contains("/feed") ||
                lowercaseUrl.contains("/rss") ||
                lowercaseUrl.contains("/atom") ||
                lowercaseUrl.endsWith(".xml") ||
                lowercaseUrl.contains("feeds/posts")
    }

    private fun isFeedContentType(contentType: String): Boolean {
        val lowerType = contentType.lowercase()
        return lowerType.contains("xml") ||
                lowerType.contains("rss") ||
                lowerType.contains("atom")
    }

    companion object {
        private const val USER_AGENT = "RSSReader/1.0 (Android)"
    }
}
