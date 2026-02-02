package com.example.rssreader.ui.screens.reader

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rssreader.domain.model.Article
import com.example.rssreader.ui.theme.StarColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    articleId: Long,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    uiState.article?.feedTitle?.let { feedTitle ->
                        Text(
                            text = feedTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    uiState.article?.let { article ->
                        IconButton(onClick = { viewModel.toggleStar() }) {
                            Icon(
                                imageVector = if (article.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (article.isStarred) "Unstar" else "Star",
                                tint = if (article.isStarred) StarColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        article.link?.let { link ->
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    Icons.Default.OpenInBrowser,
                                    contentDescription = "Open in browser"
                                )
                            }

                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, article.title)
                                        putExtra(Intent.EXTRA_TEXT, link)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share article")
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.article != null -> {
                    val article = uiState.article!!
                    val htmlContent = remember(article, isDarkTheme) {
                        buildArticleHtml(
                            article = article,
                            backgroundColor = backgroundColor,
                            textColor = textColor,
                            linkColor = linkColor,
                            mutedColor = mutedColor
                        )
                    }

                    ArticleWebView(
                        htmlContent = htmlContent,
                        onLinkClick = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleWebView(
    htmlContent: String,
    onLinkClick: (String) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = object : WebViewClient() {
                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        url?.let { onLinkClick(it) }
                        return true
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                null,
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun buildArticleHtml(
    article: Article,
    backgroundColor: Int,
    textColor: Int,
    linkColor: Int,
    mutedColor: Int
): String {
    val bgColor = String.format("#%06X", 0xFFFFFF and backgroundColor)
    val txtColor = String.format("#%06X", 0xFFFFFF and textColor)
    val lnkColor = String.format("#%06X", 0xFFFFFF and linkColor)
    val mtdColor = String.format("#%06X", 0xFFFFFF and mutedColor)

    val metaInfo = buildString {
        article.author?.let {
            append(escapeHtml(it))
        }
        article.publishedAt?.let { timestamp ->
            if (isNotEmpty()) append(" &middot; ")
            append(formatDate(timestamp))
        }
    }

    val content = article.content ?: article.summary ?: ""

    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

        * {
            box-sizing: border-box;
            -webkit-tap-highlight-color: transparent;
        }

        html {
            font-size: 18px;
            -webkit-text-size-adjust: 100%;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            font-size: 1rem;
            line-height: 1.75;
            color: $txtColor;
            background-color: $bgColor;
            margin: 0;
            padding: 20px 24px 48px 24px;
            word-wrap: break-word;
            overflow-wrap: break-word;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }

        .article-header {
            margin-bottom: 28px;
            padding-bottom: 20px;
            border-bottom: 1px solid ${mtdColor}33;
        }

        .article-title {
            font-size: 1.65rem;
            font-weight: 700;
            line-height: 1.3;
            margin: 0 0 12px 0;
            color: $txtColor;
            letter-spacing: -0.02em;
        }

        .article-meta {
            font-size: 0.875rem;
            color: $mtdColor;
            margin: 0;
            font-weight: 500;
        }

        .article-content {
            font-weight: 400;
        }

        .article-content p {
            margin: 0 0 1.5em 0;
        }

        .article-content h1,
        .article-content h2,
        .article-content h3,
        .article-content h4,
        .article-content h5,
        .article-content h6 {
            font-weight: 600;
            line-height: 1.3;
            margin: 2em 0 0.75em 0;
            letter-spacing: -0.01em;
        }

        .article-content h1 { font-size: 1.5rem; }
        .article-content h2 { font-size: 1.35rem; }
        .article-content h3 { font-size: 1.2rem; }
        .article-content h4 { font-size: 1.1rem; }

        .article-content a {
            color: $lnkColor;
            text-decoration: none;
            border-bottom: 1px solid ${lnkColor}66;
            transition: border-color 0.2s;
        }

        .article-content a:hover,
        .article-content a:active {
            border-bottom-color: $lnkColor;
        }

        .article-content img {
            max-width: 100%;
            height: auto;
            border-radius: 8px;
            margin: 1.5em 0;
            display: block;
        }

        .article-content figure {
            margin: 1.5em 0;
            padding: 0;
        }

        .article-content figcaption {
            font-size: 0.875rem;
            color: $mtdColor;
            text-align: center;
            margin-top: 8px;
            font-style: italic;
        }

        .article-content blockquote {
            margin: 1.5em 0;
            padding: 0 0 0 20px;
            border-left: 3px solid $lnkColor;
            color: $mtdColor;
            font-style: italic;
        }

        .article-content blockquote p {
            margin: 0;
        }

        .article-content pre,
        .article-content code {
            font-family: 'SF Mono', 'Menlo', 'Monaco', 'Courier New', monospace;
            font-size: 0.875rem;
            background-color: ${mtdColor}15;
            border-radius: 4px;
        }

        .article-content code {
            padding: 2px 6px;
        }

        .article-content pre {
            padding: 16px;
            overflow-x: auto;
            margin: 1.5em 0;
        }

        .article-content pre code {
            padding: 0;
            background: none;
        }

        .article-content ul,
        .article-content ol {
            margin: 1em 0;
            padding-left: 1.5em;
        }

        .article-content li {
            margin: 0.5em 0;
        }

        .article-content hr {
            border: none;
            border-top: 1px solid ${mtdColor}33;
            margin: 2em 0;
        }

        .article-content table {
            width: 100%;
            border-collapse: collapse;
            margin: 1.5em 0;
            font-size: 0.9rem;
        }

        .article-content th,
        .article-content td {
            padding: 12px;
            border: 1px solid ${mtdColor}33;
            text-align: left;
        }

        .article-content th {
            font-weight: 600;
            background-color: ${mtdColor}10;
        }

        .article-content video,
        .article-content iframe {
            max-width: 100%;
            border-radius: 8px;
            margin: 1.5em 0;
        }

        .lead-image {
            width: calc(100% + 48px);
            margin: 0 -24px 24px -24px;
            border-radius: 0;
        }
    </style>
</head>
<body>
    <article>
        <header class="article-header">
            <h1 class="article-title">${escapeHtml(article.title)}</h1>
            ${if (metaInfo.isNotEmpty()) """<p class="article-meta">$metaInfo</p>""" else ""}
        </header>
        <div class="article-content">
            $content
        </div>
    </article>
</body>
</html>
""".trimIndent()
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
