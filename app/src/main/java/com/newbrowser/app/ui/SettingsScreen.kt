package com.newbrowser.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.newbrowser.app.data.BrowserSettings
import com.newbrowser.app.data.SEARCH_ENGINES
import com.newbrowser.app.data.searchEngineFor

@Composable
fun SettingsScreen(
    appSettings: BrowserSettings,
    onClearBrowsingData: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

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
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                SectionHeader("General")

                Text("Home page")
                OutlinedTextField(
                    value = appSettings.homeUrl,
                    onValueChange = appSettings::updateHomeUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                    singleLine = true,
                )

                SettingSwitchRow(
                    label = "Enable JavaScript",
                    checked = appSettings.javaScriptEnabled,
                    onCheckedChange = appSettings::updateJavaScriptEnabled,
                )

                SearchEngineRow(
                    selectedKey = appSettings.searchEngineKey,
                    onSelect = appSettings::updateSearchEngineKey,
                )

                SettingSwitchRow(
                    label = "Dark mode for web pages",
                    checked = appSettings.darkModeForPages,
                    onCheckedChange = appSettings::updateDarkModeForPages,
                )

                Button(
                    onClick = onClearBrowsingData,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                ) {
                    Text("Clear browsing data")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                SectionHeader("Shields")

                SettingSwitchRow(
                    label = "Block pop-ups",
                    checked = appSettings.blockPopups,
                    onCheckedChange = appSettings::updateBlockPopups,
                )
                Text(
                    text = "Blocks windows a page opens on its own; pop-ups from a direct tap still open as a new tab.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )

                SettingSwitchRow(
                    label = "Send Do Not Track requests",
                    checked = appSettings.doNotTrack,
                    onCheckedChange = appSettings::updateDoNotTrack,
                )
                Text(
                    text = "Asks sites not to track you. Most sites ignore this, but it costs nothing to ask.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SearchEngineRow(
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Search engine")
            Text(searchEngineFor(selectedKey).label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SEARCH_ENGINES.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(engine.label) },
                    onClick = {
                        expanded = false
                        onSelect(engine.key)
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
