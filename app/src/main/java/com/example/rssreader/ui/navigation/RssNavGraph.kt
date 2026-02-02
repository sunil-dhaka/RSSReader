package com.example.rssreader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.rssreader.RssReaderApp
import com.example.rssreader.ui.screens.addfeed.AddFeedScreen
import com.example.rssreader.ui.screens.addfeed.AddFeedViewModel
import com.example.rssreader.ui.screens.articles.ArticlesScreen
import com.example.rssreader.ui.screens.articles.ArticlesViewModel
import com.example.rssreader.ui.screens.feeds.FeedsScreen
import com.example.rssreader.ui.screens.feeds.FeedsViewModel
import com.example.rssreader.ui.screens.reader.ReaderScreen
import com.example.rssreader.ui.screens.reader.ReaderViewModel
import com.example.rssreader.ui.screens.search.SearchScreen
import com.example.rssreader.ui.screens.settings.SettingsScreen
import com.example.rssreader.ui.screens.settings.SettingsViewModel

@Composable
fun RssNavGraph(
    navController: NavHostController,
    onExportOpml: () -> Unit,
    onImportOpml: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RssReaderApp

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Feeds.route
    ) {
        composable(NavRoutes.Feeds.route) {
            val viewModel = remember { FeedsViewModel(app.repository) }
            FeedsScreen(
                viewModel = viewModel,
                onNavigateToAllArticles = {
                    navController.navigate(NavRoutes.AllArticles.route)
                },
                onNavigateToUnreadArticles = {
                    navController.navigate(NavRoutes.UnreadArticles.route)
                },
                onNavigateToStarredArticles = {
                    navController.navigate(NavRoutes.StarredArticles.route)
                },
                onNavigateToFeedArticles = { feedId ->
                    navController.navigate(NavRoutes.FeedArticles.createRoute(feedId))
                },
                onNavigateToFolderArticles = { folderId ->
                    navController.navigate(NavRoutes.FolderArticles.createRoute(folderId))
                },
                onNavigateToAddFeed = {
                    navController.navigate(NavRoutes.AddFeed.route)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                },
                onNavigateToSearch = {
                    navController.navigate(NavRoutes.Search.route)
                }
            )
        }

        composable(NavRoutes.AllArticles.route) {
            val viewModel = remember { ArticlesViewModel(app.repository) }
            viewModel.setFilterAll()
            ArticlesScreen(
                viewModel = viewModel,
                title = "All Articles",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToArticle = { articleId ->
                    navController.navigate(NavRoutes.ArticleReader.createRoute(articleId))
                }
            )
        }

        composable(NavRoutes.UnreadArticles.route) {
            val viewModel = remember { ArticlesViewModel(app.repository) }
            viewModel.setFilterUnread()
            ArticlesScreen(
                viewModel = viewModel,
                title = "Unread",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToArticle = { articleId ->
                    navController.navigate(NavRoutes.ArticleReader.createRoute(articleId))
                }
            )
        }

        composable(NavRoutes.StarredArticles.route) {
            val viewModel = remember { ArticlesViewModel(app.repository) }
            viewModel.setFilterStarred()
            ArticlesScreen(
                viewModel = viewModel,
                title = "Starred",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToArticle = { articleId ->
                    navController.navigate(NavRoutes.ArticleReader.createRoute(articleId))
                }
            )
        }

        composable(
            route = NavRoutes.FeedArticles.route,
            arguments = listOf(navArgument("feedId") { type = NavType.LongType })
        ) { backStackEntry ->
            val feedId = backStackEntry.arguments?.getLong("feedId") ?: return@composable
            val viewModel = remember { ArticlesViewModel(app.repository) }
            viewModel.setFilterByFeed(feedId)
            ArticlesScreen(
                viewModel = viewModel,
                title = "Feed",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToArticle = { articleId ->
                    navController.navigate(NavRoutes.ArticleReader.createRoute(articleId))
                }
            )
        }

        composable(
            route = NavRoutes.FolderArticles.route,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
            val viewModel = remember { ArticlesViewModel(app.repository) }
            viewModel.setFilterByFolder(folderId)
            ArticlesScreen(
                viewModel = viewModel,
                title = "Folder",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToArticle = { articleId ->
                    navController.navigate(NavRoutes.ArticleReader.createRoute(articleId))
                }
            )
        }

        composable(
            route = NavRoutes.ArticleReader.route,
            arguments = listOf(navArgument("articleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: return@composable
            val viewModel = remember { ReaderViewModel(app.repository) }
            ReaderScreen(
                viewModel = viewModel,
                articleId = articleId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.AddFeed.route) {
            val viewModel = remember { AddFeedViewModel(app.repository, app.feedDiscovery) }
            AddFeedScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Settings.route) {
            val viewModel = remember { SettingsViewModel(context, app.repository) }
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onExportOpml = onExportOpml,
                onImportOpml = onImportOpml
            )
        }

        composable(NavRoutes.Search.route) {
            val viewModel = remember { ArticlesViewModel(app.repository) }
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToArticle = { articleId ->
                    navController.navigate(NavRoutes.ArticleReader.createRoute(articleId))
                }
            )
        }
    }
}
