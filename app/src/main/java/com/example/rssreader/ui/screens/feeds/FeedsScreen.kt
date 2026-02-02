package com.example.rssreader.ui.screens.feeds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rssreader.ui.components.FeedItem
import com.example.rssreader.ui.components.FolderItem
import com.example.rssreader.ui.components.SmartFeedItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsScreen(
    viewModel: FeedsViewModel,
    onNavigateToAllArticles: () -> Unit,
    onNavigateToUnreadArticles: () -> Unit,
    onNavigateToStarredArticles: () -> Unit,
    onNavigateToFeedArticles: (Long) -> Unit,
    onNavigateToFolderArticles: (Long) -> Unit,
    onNavigateToAddFeed: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val expandedFolders by viewModel.expandedFolders.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RSS Reader") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { viewModel.refreshAllFeeds() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddFeed) {
                Icon(Icons.Default.Add, contentDescription = "Add Feed")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshAllFeeds() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.feedsWithoutFolder.isEmpty() && uiState.folders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator()
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No feeds yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap + to add your first feed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SmartFeedItem(
                            title = "All Articles",
                            icon = {
                                Icon(
                                    Icons.Default.AllInbox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            unreadCount = uiState.totalUnreadCount,
                            onClick = onNavigateToAllArticles
                        )
                    }

                    item {
                        SmartFeedItem(
                            title = "Unread",
                            icon = {
                                Icon(
                                    Icons.Default.Inbox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            unreadCount = uiState.totalUnreadCount,
                            onClick = onNavigateToUnreadArticles
                        )
                    }

                    item {
                        SmartFeedItem(
                            title = "Starred",
                            icon = {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            unreadCount = 0,
                            onClick = onNavigateToStarredArticles
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(uiState.folders) { folder ->
                        val feeds = uiState.feedsByFolder[folder.id] ?: emptyList()
                        FolderItem(
                            folder = folder,
                            feeds = feeds,
                            isExpanded = folder.id in expandedFolders,
                            onFolderClick = { onNavigateToFolderArticles(folder.id) },
                            onFolderToggle = { viewModel.toggleFolderExpanded(folder.id) },
                            onFeedClick = { feed -> onNavigateToFeedArticles(feed.id) },
                            onFeedDelete = { feed -> viewModel.deleteFeed(feed.id) },
                            onFolderDelete = { viewModel.deleteFolder(folder.id) }
                        )
                    }

                    items(uiState.feedsWithoutFolder) { feed ->
                        FeedItem(
                            feed = feed,
                            onClick = { onNavigateToFeedArticles(feed.id) },
                            onDelete = { viewModel.deleteFeed(feed.id) }
                        )
                    }

                    item {
                        TextButton(
                            onClick = { showNewFolderDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Create Folder")
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewFolderDialog = false
                newFolderName = ""
            },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            showNewFolderDialog = false
                            newFolderName = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNewFolderDialog = false
                        newFolderName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
