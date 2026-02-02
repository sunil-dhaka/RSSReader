package com.example.rssreader.util

import com.example.rssreader.data.repository.RssRepository
import kotlinx.coroutines.flow.first
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OpmlManager(private val repository: RssRepository) {

    suspend fun exportToOpml(): String {
        val feeds = repository.getAllFeeds().first()
        val folders = repository.getAllFolders().first()

        val feedsByFolder = feeds.groupBy { it.folderId }
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<opml version="2.0">""")
            appendLine("  <head>")
            appendLine("    <title>RSS Reader Subscriptions</title>")
            appendLine("    <dateCreated>${dateFormat.format(Date())}</dateCreated>")
            appendLine("  </head>")
            appendLine("  <body>")

            folders.forEach { folder ->
                val folderFeeds = feedsByFolder[folder.id] ?: emptyList()
                if (folderFeeds.isNotEmpty()) {
                    appendLine("""    <outline text="${escapeXml(folder.name)}" title="${escapeXml(folder.name)}">""")
                    folderFeeds.forEach { feed ->
                        appendLine(buildOutlineElement(feed.title, feed.feedUrl, feed.siteUrl, "      "))
                    }
                    appendLine("    </outline>")
                }
            }

            feedsByFolder[null]?.forEach { feed ->
                appendLine(buildOutlineElement(feed.title, feed.feedUrl, feed.siteUrl, "    "))
            }

            appendLine("  </body>")
            appendLine("</opml>")
        }
    }

    private fun buildOutlineElement(
        title: String,
        feedUrl: String,
        siteUrl: String?,
        indent: String
    ): String {
        val htmlUrl = siteUrl?.let { """ htmlUrl="${escapeXml(it)}"""" } ?: ""
        return """$indent<outline type="rss" text="${escapeXml(title)}" title="${escapeXml(title)}" xmlUrl="${escapeXml(feedUrl)}"$htmlUrl/>"""
    }

    suspend fun importFromOpml(opmlContent: String): Int {
        val doc = Jsoup.parse(opmlContent, "", Parser.xmlParser())
        var importedCount = 0

        val outlines = doc.select("outline")

        for (outline in outlines) {
            val xmlUrl = outline.attr("xmlUrl")
            if (xmlUrl.isNotBlank()) {
                try {
                    val result = repository.addFeed(xmlUrl)
                    if (result.isSuccess) {
                        importedCount++
                    }
                } catch (e: Exception) {
                    continue
                }
            } else {
                val folderName = outline.attr("text").takeIf { it.isNotBlank() }
                    ?: outline.attr("title")

                if (folderName.isNotBlank()) {
                    val folderResult = repository.createFolder(folderName)
                    val folderId = folderResult.getOrNull()?.id

                    for (childOutline in outline.children()) {
                        val childXmlUrl = childOutline.attr("xmlUrl")
                        if (childXmlUrl.isNotBlank()) {
                            try {
                                val result = repository.addFeed(childXmlUrl)
                                result.getOrNull()?.let { feed ->
                                    importedCount++
                                    if (folderId != null) {
                                        repository.updateFeedFolder(feed.id, folderId)
                                    }
                                }
                            } catch (e: Exception) {
                                continue
                            }
                        }
                    }
                }
            }
        }

        return importedCount
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
