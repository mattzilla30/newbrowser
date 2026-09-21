package com.newbrowser.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.newbrowser.app.data.BrowserSettings
import com.newbrowser.app.data.CYN_THEME_INDEX
import com.newbrowser.app.data.SEARCH_ENGINES
import com.newbrowser.app.data.SYSTEM_THEME_INDEX
import com.newbrowser.app.data.searchEngineFor
import com.newbrowser.app.ui.theme.CynPrimary
import com.newbrowser.app.ui.theme.CynSecondary
import com.newbrowser.app.ui.theme.THEME_COLOR_PRESETS

@Composable
fun SettingsScreen(
    appSettings: BrowserSettings,
    onClearBrowsingData: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var confirmClearDataVisible by remember { mutableStateOf(false) }

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
            CynAccentLine()

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column {
                    SectionHeader("General")
                    GroupedCard {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text("Home page", style = MaterialTheme.typography.labelLarge)
                            OutlinedTextField(
                                value = appSettings.homeUrl,
                                onValueChange = appSettings::updateHomeUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                singleLine = true,
                            )
                        }
                        CardRow(
                            icon = Icons.Filled.Javascript,
                            title = "Enable JavaScript",
                        ) {
                            Switch(
                                checked = appSettings.javaScriptEnabled,
                                onCheckedChange = appSettings::updateJavaScriptEnabled,
                            )
                        }
                        SearchEngineRow(
                            selectedKey = appSettings.searchEngineKey,
                            onSelect = appSettings::updateSearchEngineKey,
                        )
                        CardRow(
                            icon = Icons.Filled.DarkMode,
                            title = "Dark mode for web pages",
                        ) {
                            Switch(
                                checked = appSettings.darkModeForPages,
                                onCheckedChange = appSettings::updateDarkModeForPages,
                            )
                        }
                    }
                    Button(
                        onClick = { confirmClearDataVisible = true },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text("Clear browsing data")
                    }
                }

                Column {
                    SectionHeader("Appearance")
                    GroupedCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Theme color", modifier = Modifier.padding(bottom = 12.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CynColorSwatch(
                                    selected = appSettings.themeColorIndex == CYN_THEME_INDEX,
                                    onClick = { appSettings.updateThemeColorIndex(CYN_THEME_INDEX) },
                                )
                                ColorSwatch(
                                    color = null,
                                    selected = appSettings.themeColorIndex == SYSTEM_THEME_INDEX,
                                    onClick = { appSettings.updateThemeColorIndex(SYSTEM_THEME_INDEX) },
                                )
                                THEME_COLOR_PRESETS.forEachIndexed { index, color ->
                                    ColorSwatch(
                                        color = color,
                                        selected = appSettings.themeColorIndex == index,
                                        onClick = { appSettings.updateThemeColorIndex(index) },
                                    )
                                }
                            }

                            Text("Page text size", modifier = Modifier.padding(top = 20.dp, bottom = 12.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(75, 100, 125, 150, 200).forEach { percent ->
                                    val selected = appSettings.pageTextZoom == percent
                                    if (selected) {
                                        Button(onClick = {}) { Text("$percent%") }
                                    } else {
                                        TextButton(onClick = { appSettings.updatePageTextZoom(percent) }) {
                                            Text("$percent%")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column {
                    SectionHeader("Privacy")
                    GroupedCard {
                        CardRow(
                            icon = Icons.Filled.OpenInNew,
                            title = "Block pop-ups",
                            subtitle = "Pop-ups from a direct tap still open as a new tab",
                        ) {
                            Switch(checked = appSettings.blockPopups, onCheckedChange = appSettings::updateBlockPopups)
                        }
                        CardRow(
                            icon = Icons.Filled.VisibilityOff,
                            title = "Send Do Not Track requests",
                            subtitle = "Most sites ignore this, but it costs nothing to ask",
                        ) {
                            Switch(checked = appSettings.doNotTrack, onCheckedChange = appSettings::updateDoNotTrack)
                        }
                        CardRow(
                            icon = Icons.Filled.Cookie,
                            title = "Block third-party cookies",
                            subtitle = "Stops embedded trackers from reading or setting cookies",
                        ) {
                            Switch(
                                checked = appSettings.blockThirdPartyCookies,
                                onCheckedChange = appSettings::updateBlockThirdPartyCookies,
                            )
                        }
                        CardRow(
                            icon = Icons.Filled.Lock,
                            title = "Lock private tabs",
                            subtitle = "Requires fingerprint, face, or PIN to reopen them",
                        ) {
                            Switch(
                                checked = appSettings.lockPrivateTabsEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        val canAuthenticate = BiometricManager.from(context).canAuthenticate(
                                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                                        )
                                        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                                            appSettings.updateLockPrivateTabsEnabled(true)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Set up a screen lock (PIN, pattern, or fingerprint) to use this",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    } else {
                                        appSettings.updateLockPrivateTabsEnabled(false)
                                    }
                                },
                            )
                        }
                    }
                }

                Column {
                    SectionHeader("Developer")
                    GroupedCard {
                        CardRow(
                            icon = Icons.Filled.Code,
                            title = "Remote debugging",
                            subtitle = "Connect over USB, then open chrome://inspect in Chrome",
                        ) {
                            Switch(
                                checked = appSettings.remoteDebuggingEnabled,
                                onCheckedChange = appSettings::updateRemoteDebuggingEnabled,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClearDataVisible) {
        AlertDialog(
            onDismissRequest = { confirmClearDataVisible = false },
            title = { Text("Clear browsing data?") },
            text = { Text("This removes cookies, site storage, and history. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearBrowsingData()
                    confirmClearDataVisible = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDataVisible = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
    )
}

@Composable
private fun SearchEngineRow(
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        CardRow(
            icon = Icons.Filled.Language,
            title = "Search engine",
            modifier = Modifier.clickable { expanded = true },
        ) {
            Text(searchEngineFor(selectedKey).label, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun CynColorSwatch(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(CynPrimary, CynSecondary)))
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color?, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color ?: MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = if (color != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
