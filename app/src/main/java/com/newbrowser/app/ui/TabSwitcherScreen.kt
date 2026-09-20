package com.newbrowser.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.newbrowser.app.data.PersistedTab
import com.newbrowser.app.ui.tabs.BrowserTab
import com.newbrowser.app.ui.tabs.TabGroup
import com.newbrowser.app.ui.tabs.TabManager
import com.newbrowser.app.ui.tabs.NEW_TAB_URL

private val GROUP_COLORS = listOf(
    Color(0xFFE53935), // red
    Color(0xFFFB8C00), // orange
    Color(0xFFFDD835), // yellow
    Color(0xFF43A047), // green
    Color(0xFF1E88E5), // blue
    Color(0xFF8E24AA), // purple
)

@Composable
fun TabSwitcherScreen(
    tabManager: TabManager,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var overflowExpanded by remember { mutableStateOf(false) }
    var groupDialogTab by remember { mutableStateOf<BrowserTab?>(null) }
    var recentlyClosedDialogVisible by remember { mutableStateOf(false) }

    val filteredTabs = tabManager.tabs.filter { tab ->
        searchQuery.isBlank() ||
            tab.title.contains(searchQuery, ignoreCase = true) ||
            tab.url.contains(searchQuery, ignoreCase = true)
    }
    val ungrouped = filteredTabs.filter { it.groupId == null }
    val byGroup = filteredTabs.filter { it.groupId != null }.groupBy {
        TabGroup(it.groupId!!, it.groupName ?: "Group", it.groupColorIndex)
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
                    text = "Tabs",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(onClick = {
                    searchVisible = !searchVisible
                    if (!searchVisible) searchQuery = ""
                }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search tabs")
                }
                IconButton(onClick = {
                    tabManager.newTab()
                    onBack()
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "New tab")
                }
                Box {
                    IconButton(onClick = { overflowExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = overflowExpanded, onDismissRequest = { overflowExpanded = false }) {
                        DropdownMenuItem(text = { Text("Close all tabs") }, onClick = {
                            overflowExpanded = false
                            tabManager.closeAllTabs()
                        })
                        DropdownMenuItem(
                            text = { Text("Recently closed (${tabManager.recentlyClosed.size})") },
                            enabled = tabManager.recentlyClosed.isNotEmpty(),
                            onClick = {
                                overflowExpanded = false
                                recentlyClosedDialogVisible = true
                            },
                        )
                    }
                }
            }
            CynAccentLine()

            if (searchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true,
                    placeholder = { Text("Search open tabs") },
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                byGroup.forEach { (group, tabsInGroup) ->
                    item(key = "group_${group.id}") {
                        GroupHeader(group)
                    }
                    items(tabsInGroup, key = { it.id }) { tab ->
                        TabRow(
                            tab = tab,
                            isActive = tab.id == tabManager.activeTabId,
                            group = group,
                            onSelect = {
                                tabManager.selectTab(tab)
                                onBack()
                            },
                            onClose = { tabManager.closeTab(tab) },
                            onCloseOthers = { tabManager.closeOtherTabs(tab) },
                            onGroupAction = {
                                if (tab.groupId != null) tabManager.removeFromGroup(tab) else groupDialogTab = tab
                            },
                        )
                        HorizontalDivider()
                    }
                }
                items(ungrouped, key = { it.id }) { tab ->
                    TabRow(
                        tab = tab,
                        isActive = tab.id == tabManager.activeTabId,
                        group = null,
                        onSelect = {
                            tabManager.selectTab(tab)
                            onBack()
                        },
                        onClose = { tabManager.closeTab(tab) },
                        onCloseOthers = { tabManager.closeOtherTabs(tab) },
                        onGroupAction = { groupDialogTab = tab },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    groupDialogTab?.let { tab ->
        GroupPickerDialog(
            existingGroups = tabManager.groups,
            onPickExisting = { group ->
                tabManager.addToGroup(tab, group)
                groupDialogTab = null
            },
            onCreateNew = { name ->
                val colorIndex = tabManager.groups.size % GROUP_COLORS.size
                tabManager.createGroup(listOf(tab), name, colorIndex)
                groupDialogTab = null
            },
            onDismiss = { groupDialogTab = null },
        )
    }

    if (recentlyClosedDialogVisible) {
        RecentlyClosedDialog(
            entries = tabManager.recentlyClosed,
            onReopen = { entry ->
                tabManager.reopenRecentlyClosed(entry)
                recentlyClosedDialogVisible = false
                onBack()
            },
            onDismiss = { recentlyClosedDialogVisible = false },
        )
    }
}

@Composable
private fun RecentlyClosedDialog(
    entries: List<PersistedTab>,
    onReopen: (PersistedTab) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recently closed") },
        text = {
            Column {
                entries.forEach { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReopen(entry) }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(entry.title, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                        Text(entry.url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun GroupHeader(group: TabGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(GROUP_COLORS[group.colorIndex % GROUP_COLORS.size], CircleShape),
        )
        Text(
            text = group.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun TabRow(
    tab: BrowserTab,
    isActive: Boolean,
    group: TabGroup?,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onCloseOthers: () -> Unit,
    onGroupAction: () -> Unit,
) {
    var kebabExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (group != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(GROUP_COLORS[group.colorIndex % GROUP_COLORS.size], CircleShape),
            )
            Box(modifier = Modifier.size(8.dp))
        }
        tab.favicon?.let { fav ->
            Image(
                bitmap = fav.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (tab.isIncognito) "(Private) ${tab.title}" else tab.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
            Text(
                text = if (tab.url == NEW_TAB_URL) "New Tab" else tab.url,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
        Box {
            IconButton(onClick = { kebabExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Tab options")
            }
            DropdownMenu(expanded = kebabExpanded, onDismissRequest = { kebabExpanded = false }) {
                DropdownMenuItem(text = { Text("Close other tabs") }, onClick = {
                    kebabExpanded = false
                    onCloseOthers()
                })
                DropdownMenuItem(
                    text = { Text(if (group != null) "Remove from group" else "Add to group") },
                    onClick = {
                        kebabExpanded = false
                        onGroupAction()
                    },
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close tab")
        }
    }
}

@Composable
private fun GroupPickerDialog(
    existingGroups: List<TabGroup>,
    onPickExisting: (TabGroup) -> Unit,
    onCreateNew: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newGroupName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to group") },
        text = {
            Column {
                existingGroups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickExisting(group) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(GROUP_COLORS[group.colorIndex % GROUP_COLORS.size], CircleShape),
                        )
                        Text(group.name, modifier = Modifier.padding(start = 12.dp))
                    }
                }
                if (existingGroups.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    singleLine = true,
                    placeholder = { Text("New group name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreateNew(newGroupName.ifBlank { "Group" }) },
                enabled = newGroupName.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
