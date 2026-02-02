package com.example.rssreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.rssreader.ui.navigation.RssNavGraph
import com.example.rssreader.ui.theme.RSSReaderTheme
import com.example.rssreader.util.OpmlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val opmlManager by lazy {
        OpmlManager((application as RssReaderApp).repository)
    }

    private val exportOpmlLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/xml")
    ) { uri ->
        uri?.let { exportOpml(it) }
    }

    private val importOpmlLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importOpml(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RSSReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    RssNavGraph(
                        navController = navController,
                        onExportOpml = { launchExportOpml() },
                        onImportOpml = { launchImportOpml() }
                    )
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.toString()?.let { url ->
                    if (url.contains("feed") || url.endsWith(".xml") || url.contains("rss")) {
                        addFeedFromUrl(url)
                    }
                }
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                    val url = extractUrl(text)
                    if (url != null) {
                        addFeedFromUrl(url)
                    }
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        return urlPattern.find(text)?.value
    }

    private fun addFeedFromUrl(url: String) {
        val app = application as RssReaderApp
        lifecycleScope.launch {
            try {
                val result = app.repository.addFeed(url)
                result.fold(
                    onSuccess = { feed ->
                        Toast.makeText(
                            this@MainActivity,
                            "Added: ${feed.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            this@MainActivity,
                            "Failed: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun launchExportOpml() {
        exportOpmlLauncher.launch("feeds.opml")
    }

    private fun launchImportOpml() {
        importOpmlLauncher.launch(arrayOf("text/xml", "application/xml", "*/*"))
    }

    private fun exportOpml(uri: Uri) {
        lifecycleScope.launch {
            try {
                val opml = withContext(Dispatchers.IO) {
                    opmlManager.exportToOpml()
                }
                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(opml.toByteArray())
                }
                Toast.makeText(
                    this@MainActivity,
                    "Feeds exported successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Export failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun importOpml(uri: Uri) {
        lifecycleScope.launch {
            try {
                val opmlContent = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    }
                } ?: throw Exception("Could not read file")

                val count = withContext(Dispatchers.IO) {
                    opmlManager.importFromOpml(opmlContent)
                }
                Toast.makeText(
                    this@MainActivity,
                    "Imported $count feeds",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Import failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
