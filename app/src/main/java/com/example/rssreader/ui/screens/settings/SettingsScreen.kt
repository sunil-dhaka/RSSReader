package com.example.rssreader.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onExportOpml: () -> Unit,
    onImportOpml: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRefreshIntervalDialog by remember { mutableStateOf(false) }
    var showKeepDaysDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "Sync") {
                SettingsItem(
                    title = "Refresh interval",
                    subtitle = "${uiState.refreshIntervalMinutes} minutes",
                    onClick = { showRefreshIntervalDialog = true }
                )
            }

            SettingsSection(title = "Articles") {
                SettingsItem(
                    title = "Keep articles for",
                    subtitle = "${uiState.keepArticlesDays} days",
                    onClick = { showKeepDaysDialog = true }
                )

                SwitchSettingsItem(
                    title = "Show images",
                    subtitle = "Display article images in list and reader",
                    checked = uiState.showImages,
                    onCheckedChange = { viewModel.setShowImages(it) }
                )

                SwitchSettingsItem(
                    title = "Open in browser",
                    subtitle = "Open articles in external browser by default",
                    checked = uiState.openInBrowser,
                    onCheckedChange = { viewModel.setOpenInBrowser(it) }
                )
            }

            SettingsSection(title = "Reader") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Font size: ${uiState.fontSize}sp",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Slider(
                        value = uiState.fontSize.toFloat(),
                        onValueChange = { viewModel.setFontSize(it.toInt()) },
                        valueRange = 12f..24f,
                        steps = 11
                    )
                }
            }

            SettingsSection(title = "Appearance") {
                SettingsItem(
                    title = "Theme",
                    subtitle = when (uiState.darkMode) {
                        "light" -> "Light"
                        "dark" -> "Dark"
                        else -> "System default"
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            SettingsSection(title = "Data") {
                SettingsItem(
                    title = "Import feeds (OPML)",
                    subtitle = "Import feeds from an OPML file",
                    onClick = onImportOpml
                )

                SettingsItem(
                    title = "Export feeds (OPML)",
                    subtitle = "Export all feeds to an OPML file",
                    onClick = onExportOpml
                )

                SettingsItem(
                    title = "Clean up old articles",
                    subtitle = "Remove articles older than ${uiState.keepArticlesDays} days",
                    onClick = { viewModel.cleanupOldArticles() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "RSS Reader v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showRefreshIntervalDialog) {
        val intervals = listOf(15, 30, 60, 120, 240, 480, 720, 1440)
        ListDialog(
            title = "Refresh interval",
            options = intervals.map {
                when {
                    it < 60 -> "$it minutes"
                    it == 60 -> "1 hour"
                    it < 1440 -> "${it / 60} hours"
                    else -> "24 hours"
                }
            },
            selectedIndex = intervals.indexOf(uiState.refreshIntervalMinutes).coerceAtLeast(0),
            onSelect = { index ->
                viewModel.setRefreshInterval(intervals[index])
                showRefreshIntervalDialog = false
            },
            onDismiss = { showRefreshIntervalDialog = false }
        )
    }

    if (showKeepDaysDialog) {
        val days = listOf(7, 14, 30, 60, 90, 180, 365)
        ListDialog(
            title = "Keep articles for",
            options = days.map { "$it days" },
            selectedIndex = days.indexOf(uiState.keepArticlesDays).coerceAtLeast(0),
            onSelect = { index ->
                viewModel.setKeepArticlesDays(days[index])
                showKeepDaysDialog = false
            },
            onDismiss = { showKeepDaysDialog = false }
        )
    }

    if (showThemeDialog) {
        val themes = listOf("system", "light", "dark")
        val themeLabels = listOf("System default", "Light", "Dark")
        ListDialog(
            title = "Theme",
            options = themeLabels,
            selectedIndex = themes.indexOf(uiState.darkMode).coerceAtLeast(0),
            onSelect = { index ->
                viewModel.setDarkMode(themes[index])
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwitchSettingsItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ListDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
