package com.example.rssreader.ui.navigation

sealed class NavRoutes(val route: String) {
    data object Feeds : NavRoutes("feeds")
    data object AllArticles : NavRoutes("articles/all")
    data object UnreadArticles : NavRoutes("articles/unread")
    data object StarredArticles : NavRoutes("articles/starred")
    data object FeedArticles : NavRoutes("feed/{feedId}/articles") {
        fun createRoute(feedId: Long) = "feed/$feedId/articles"
    }
    data object FolderArticles : NavRoutes("folder/{folderId}/articles") {
        fun createRoute(folderId: Long) = "folder/$folderId/articles"
    }
    data object ArticleReader : NavRoutes("article/{articleId}") {
        fun createRoute(articleId: Long) = "article/$articleId"
    }
    data object AddFeed : NavRoutes("add_feed")
    data object Settings : NavRoutes("settings")
    data object Search : NavRoutes("search")
}
