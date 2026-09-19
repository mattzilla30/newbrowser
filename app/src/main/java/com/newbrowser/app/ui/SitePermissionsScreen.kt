package com.newbrowser.app.ui

import android.webkit.WebStorage
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.newbrowser.app.data.BrowserDatabase
import com.newbrowser.app.data.SitePermission

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

private fun permissionLabel(permission: String): String = when (permission) {
    "media" -> "Camera & microphone"
    "location" -> "Location"
    else -> permission
}

@Composable
fun SitePermissionsScreen(
    database: BrowserDatabase,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var permissions by remember { mutableStateOf(database.getAllSitePermissions()) }
    var siteData by remember { mutableStateOf<List<WebStorage.Origin>>(emptyList()) }

    fun refreshSiteData() {
        // WebStorage.getOrigins predates generics on the platform and hands back a raw Map.
        WebStorage.getInstance().getOrigins { origins ->
            siteData = origins.values.mapNotNull { it as? WebStorage.Origin }
        }
    }

    LaunchedEffect(Unit) { refreshSiteData() }

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
                    text = "Site permissions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (permissions.isEmpty() && siteData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "No sites have been granted or denied access, or stored data, yet.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (permissions.isNotEmpty()) {
                        item {
                            SectionLabel("Permissions")
                        }
                        items(permissions, key = { "perm_" + it.origin + it.permission }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.origin, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    Text(
                                        text = "${permissionLabel(entry.permission)}: " +
                                            if (entry.granted) "Allowed" else "Blocked",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                IconButton(onClick = {
                                    database.clearSitePermission(entry.origin, entry.permission)
                                    permissions = permissions.filterNot {
                                        it.origin == entry.origin && it.permission == entry.permission
                                    }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Forget this decision")
                                }
                            }
                            HorizontalDivider()
                        }
                    }

                    if (siteData.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SectionLabel("Site data", modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        WebStorage.getInstance().deleteAllData()
                                        siteData = emptyList()
                                    },
                                    modifier = Modifier.padding(end = 16.dp),
                                ) { Text("Clear all") }
                            }
                        }
                        items(siteData, key = { "data_" + it.origin }) { origin ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(origin.origin, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    Text(
                                        text = "${formatBytes(origin.usage)} stored",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                IconButton(onClick = {
                                    WebStorage.getInstance().deleteOrigin(origin.origin)
                                    siteData = siteData.filterNot { it.origin == origin.origin }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Clear this site's data")
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
