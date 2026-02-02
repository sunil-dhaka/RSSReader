package com.example.rssreader.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class RssParser(private val httpClient: OkHttpClient) {

    data class ParsedFeed(
        val title: String,
        val siteUrl: String?,
        val description: String?,
        val iconUrl: String?,
        val articles: List<ParsedArticle>
    )

    data class ParsedArticle(
        val guid: String,
        val title: String,
        val link: String?,
        val author: String?,
        val content: String?,
        val summary: String?,
        val imageUrl: String?,
        val publishedAt: Long?
    )

    suspend fun parseFeed(url: String): ParsedFeed = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch feed: ${response.code}")
        }

        val body = response.body?.string() ?: throw Exception("Empty response")
        val doc = Jsoup.parse(body, "", Parser.xmlParser())

        val rssChannel = doc.selectFirst("rss > channel")
        val atomFeed = doc.selectFirst("feed")

        when {
            rssChannel != null -> parseRss(rssChannel, url)
            atomFeed != null -> parseAtom(atomFeed, url)
            else -> throw Exception("Unknown feed format")
        }
    }

    private fun parseRss(channel: org.jsoup.nodes.Element, feedUrl: String): ParsedFeed {
        val title = channel.selectFirst("title")?.text() ?: "Untitled Feed"
        val siteUrl = channel.selectFirst("link")?.text()
        val description = channel.selectFirst("description")?.text()

        val iconUrl = channel.selectFirst("image > url")?.text()
            ?: siteUrl?.let { extractFavicon(it) }

        val articles = channel.select("item").map { item ->
            val itemTitle = item.selectFirst("title")?.text() ?: "Untitled"
            val itemLink = item.selectFirst("link")?.text()
            val guid = item.selectFirst("guid")?.text()
                ?: itemLink
                ?: itemTitle.hashCode().toString()

            val author = item.selectFirst("author")?.text()
                ?: item.selectFirst("dc|creator")?.text()

            val contentEncoded = item.selectFirst("content|encoded")?.text()
            val itemDescription = item.selectFirst("description")?.text()

            val content = contentEncoded ?: itemDescription
            val summary = if (contentEncoded != null) itemDescription else null

            val imageUrl = extractImageFromContent(content)
                ?: item.selectFirst("enclosure[type^=image]")?.attr("url")
                ?: item.selectFirst("media|content[type^=image]")?.attr("url")
                ?: item.selectFirst("media|thumbnail")?.attr("url")

            val pubDate = item.selectFirst("pubDate")?.text()
            val dcDate = item.selectFirst("dc|date")?.text()
            val publishedAt = parseDate(pubDate ?: dcDate)

            ParsedArticle(
                guid = guid,
                title = itemTitle,
                link = itemLink,
                author = author,
                content = content,
                summary = summary,
                imageUrl = imageUrl,
                publishedAt = publishedAt
            )
        }

        return ParsedFeed(
            title = title,
            siteUrl = siteUrl,
            description = description,
            iconUrl = iconUrl,
            articles = articles
        )
    }

    private fun parseAtom(feed: org.jsoup.nodes.Element, feedUrl: String): ParsedFeed {
        val title = feed.selectFirst("title")?.text() ?: "Untitled Feed"
        val siteUrl = feed.selectFirst("link[rel=alternate]")?.attr("href")
            ?: feed.selectFirst("link:not([rel=self])")?.attr("href")
        val description = feed.selectFirst("subtitle")?.text()

        val iconUrl = feed.selectFirst("icon")?.text()
            ?: feed.selectFirst("logo")?.text()
            ?: siteUrl?.let { extractFavicon(it) }

        val articles = feed.select("entry").map { entry ->
            val entryTitle = entry.selectFirst("title")?.text() ?: "Untitled"
            val entryLink = entry.selectFirst("link[rel=alternate]")?.attr("href")
                ?: entry.selectFirst("link")?.attr("href")

            val guid = entry.selectFirst("id")?.text()
                ?: entryLink
                ?: entryTitle.hashCode().toString()

            val author = entry.selectFirst("author > name")?.text()

            val contentElement = entry.selectFirst("content")
            val summaryElement = entry.selectFirst("summary")

            val content = contentElement?.text() ?: contentElement?.html()
            val summary = summaryElement?.text() ?: summaryElement?.html()

            val imageUrl = extractImageFromContent(content ?: summary)
                ?: entry.selectFirst("media|content[type^=image]")?.attr("url")
                ?: entry.selectFirst("media|thumbnail")?.attr("url")

            val published = entry.selectFirst("published")?.text()
            val updated = entry.selectFirst("updated")?.text()
            val publishedAt = parseDate(published ?: updated)

            ParsedArticle(
                guid = guid,
                title = entryTitle,
                link = entryLink,
                author = author,
                content = content,
                summary = summary,
                imageUrl = imageUrl,
                publishedAt = publishedAt
            )
        }

        return ParsedFeed(
            title = title,
            siteUrl = siteUrl,
            description = description,
            iconUrl = iconUrl,
            articles = articles
        )
    }

    private fun extractImageFromContent(content: String?): String? {
        if (content == null) return null
        return try {
            val doc = Jsoup.parse(content)
            doc.selectFirst("img")?.attr("src")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractFavicon(siteUrl: String): String? {
        return try {
            val url = java.net.URL(siteUrl)
            "${url.protocol}://${url.host}/favicon.ico"
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr == null) return null

        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(dateStr)?.time
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    companion object {
        private const val USER_AGENT = "RSSReader/1.0 (Android)"
    }
}
