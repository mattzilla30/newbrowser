package com.newbrowser.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.newbrowser.app.data.BrowserDatabase
import com.newbrowser.app.data.HistoryEntry

@Composable
fun HistoryScreen(
    database: BrowserDatabase,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var history by remember { mutableStateOf(database.getHistory()) }
    var query by remember { mutableStateOf("") }
    var confirmClearVisible by remember { mutableStateOf(false) }

    val filtered = remember(history, query) {
        if (query.isBlank()) {
            history
        } else {
            history.filter { it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true) }
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
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(
                    onClick = { confirmClearVisible = true },
                    enabled = history.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear history")
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
                placeholder = { Text("Search history") },
            )

            if (filtered.isEmpty()) {
                CynEmptyState(if (history.isEmpty()) "No history yet" else "No matches")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { entry ->
                        HistoryRow(entry = entry, onOpen = { onOpen(entry.url) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (confirmClearVisible) {
        AlertDialog(
            onDismissRequest = { confirmClearVisible = false },
            title = { Text("Clear history?") },
            text = { Text("This removes all ${history.size} entries from your browsing history. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    database.clearHistory()
                    history = emptyList()
                    confirmClearVisible = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearVisible = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(entry.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        Text(entry.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
