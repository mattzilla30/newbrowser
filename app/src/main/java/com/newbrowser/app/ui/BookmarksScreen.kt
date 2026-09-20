package com.newbrowser.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.newbrowser.app.data.Bookmark
import com.newbrowser.app.data.BrowserDatabase

private val BOOKMARK_HTML_ENTRY_RE = Regex(
    """<A[^>]*HREF="([^"]+)"[^>]*>([^<]*)</A>""",
    RegexOption.IGNORE_CASE,
)

/** Netscape bookmark file format: what Chrome and Firefox both import and export. */
private fun buildBookmarksHtml(bookmarks: List<Bookmark>): String {
    val entries = bookmarks.joinToString("\n") { bookmark ->
        val addDate = bookmark.createdAt / 1000
        "    <DT><A HREF=\"${bookmark.url}\" ADD_DATE=\"$addDate\">${bookmark.title}</A>"
    }
    return """
        <!DOCTYPE NETSCAPE-Bookmark-file-1>
        <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
        <TITLE>Bookmarks</TITLE>
        <H1>Bookmarks</H1>
        <DL><p>
        $entries
        </DL><p>
    """.trimIndent()
}

@Composable
fun BookmarksScreen(
    database: BrowserDatabase,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var bookmarks by remember { mutableStateOf(database.getBookmarks()) }
    var query by remember { mutableStateOf("") }
    var sortAlphabetically by remember { mutableStateOf(false) }

    val filtered = remember(bookmarks, query, sortAlphabetically) {
        val matches = if (query.isBlank()) {
            bookmarks
        } else {
            bookmarks.filter { it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true) }
        }
        if (sortAlphabetically) matches.sortedBy { it.title.lowercase() } else matches.sortedByDescending { it.createdAt }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val html = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: return@rememberLauncherForActivityResult
        val entries = BOOKMARK_HTML_ENTRY_RE.findAll(html)
            .map { it.groupValues[1] to it.groupValues[2].trim() }
            .filter { (url, _) -> url.isNotBlank() }
            .toList()
        if (entries.isNotEmpty()) {
            database.importBookmarks(entries)
            bookmarks = database.getBookmarks()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(buildBookmarksHtml(bookmarks))
            }
        } catch (e: Exception) {
            // Nothing to recover into; the picker already reported success to the user.
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Bookmarks",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(onClick = { sortAlphabetically = !sortAlphabetically }) {
                    Icon(
                        if (sortAlphabetically) Icons.Filled.Schedule else Icons.Filled.SortByAlpha,
                        contentDescription = if (sortAlphabetically) {
                            "Sort by date added"
                        } else {
                            "Sort alphabetically"
                        },
                    )
                }
                IconButton(onClick = { exportLauncher.launch("bookmarks.html") }) {
                    Icon(Icons.Filled.Save, contentDescription = "Export bookmarks")
                }
                IconButton(onClick = { importLauncher.launch("text/html") }) {
                    Icon(Icons.Filled.FileOpen, contentDescription = "Import bookmarks")
                }
            }
            CynAccentLine()

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Search bookmarks") },
            )

            if (filtered.isEmpty()) {
                CynEmptyState(if (bookmarks.isEmpty()) "No bookmarks yet" else "No matches")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { bookmark ->
                        BookmarkRow(
                            bookmark = bookmark,
                            onOpen = { onOpen(bookmark.url) },
                            onDelete = {
                                database.removeBookmark(bookmark.url)
                                bookmarks = bookmarks.filterNot { it.id == bookmark.id }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(bookmark.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(bookmark.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove bookmark")
        }
    }
}
