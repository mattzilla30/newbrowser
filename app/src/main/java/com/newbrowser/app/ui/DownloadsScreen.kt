package com.newbrowser.app.ui

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.newbrowser.app.data.BrowserDatabase
import com.newbrowser.app.data.DownloadRecord
import com.newbrowser.app.data.SavedPage
import java.io.File

@Composable
fun DownloadsScreen(
    database: BrowserDatabase,
    onOpenSavedPage: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var downloads by remember { mutableStateOf(database.getDownloads()) }
    var savedPages by remember { mutableStateOf(database.getSavedPages()) }

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
                    text = "Downloads",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            CynAccentLine()

            if (downloads.isEmpty() && savedPages.isEmpty()) {
                CynEmptyState("No downloads yet")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (savedPages.isNotEmpty()) {
                        item { SectionLabel("Saved pages") }
                        items(savedPages, key = { "saved_" + it.id }) { page ->
                            SavedPageRow(
                                page = page,
                                onOpen = { onOpenSavedPage("file://${page.filePath}") },
                                onShare = { shareSavedPage(context, page) },
                                onDelete = {
                                    database.removeSavedPage(page.id)
                                    File(page.filePath).delete()
                                    savedPages = savedPages.filterNot { it.id == page.id }
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                    if (downloads.isNotEmpty()) {
                        item { SectionLabel("Downloads") }
                        items(downloads, key = { "dl_" + it.id }) { record ->
                            DownloadRow(
                                record = record,
                                onOpen = { openDownload(context, record.downloadManagerId) },
                                onShare = { shareDownload(context, record.downloadManagerId) },
                                onDelete = {
                                    val downloadManager =
                                        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    downloadManager.remove(record.downloadManagerId)
                                    database.removeDownload(record.id)
                                    downloads = downloads.filterNot { it.id == record.id }
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun DownloadRow(
    record: DownloadRecord,
    onOpen: () -> Unit,
    onShare: () -> Unit,
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
            Text(record.fileName, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(record.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = "Share download")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete download")
        }
    }
}

@Composable
private fun SavedPageRow(
    page: SavedPage,
    onOpen: () -> Unit,
    onShare: () -> Unit,
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
            Text(page.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(page.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = "Share saved page")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete saved page")
        }
    }
}

private fun openDownload(context: Context, downloadManagerId: Long) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val uri = try {
        downloadManager.getUriForDownloadedFile(downloadManagerId)
    } catch (e: Exception) {
        null
    }

    if (uri == null) {
        Toast.makeText(context, "Download not available", Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, downloadManager.getMimeTypeForDownloadedFile(downloadManagerId))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}

private fun shareDownload(context: Context, downloadManagerId: Long) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val uri = try {
        downloadManager.getUriForDownloadedFile(downloadManagerId)
    } catch (e: Exception) {
        null
    }
    if (uri == null) {
        Toast.makeText(context, "Download not available", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = downloadManager.getMimeTypeForDownloadedFile(downloadManagerId) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share download"))
}

private fun shareSavedPage(context: Context, page: SavedPage) {
    val file = File(page.filePath)
    if (!file.exists()) {
        Toast.makeText(context, "Saved page file is missing", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "multipart/related"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share saved page"))
}
