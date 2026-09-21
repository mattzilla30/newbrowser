package com.newbrowser.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.newbrowser.app.data.BrowserSettings

/**
 * CynBrowse's answer to Omni Browser's Extensions screen. CynBrowse is a WebView-based
 * shell, not an extension-capable engine like GeckoView, so there's no add-on store to
 * browse here - just the browser's own built-in privacy tools, toggled directly.
 */
@Composable
fun ExtensionsScreen(
    appSettings: BrowserSettings,
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
                    text = "Extensions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            CynAccentLine()

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(modifier = Modifier.padding(bottom = 16.dp)) {
                    Icon(
                        Icons.Filled.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp, top = 2.dp),
                    )
                    Text(
                        text = "CynBrowse runs on Android's WebView, which can't load Chrome or " +
                            "Firefox add-ons - there's no store to browse here. These are the " +
                            "browser's own built-in privacy tools instead.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = "Built-in tools",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                GroupedCard {
                    CardRow(
                        icon = Icons.Filled.OpenInNew,
                        title = "Popup Blocker",
                        subtitle = "Blocks windows a page opens on its own",
                    ) {
                        Switch(
                            checked = appSettings.blockPopups,
                            onCheckedChange = appSettings::updateBlockPopups,
                        )
                    }
                    CardRow(
                        icon = Icons.Filled.Cookie,
                        title = "Block Third-Party Cookies",
                        subtitle = "Stops embedded trackers from reading or setting cookies",
                    ) {
                        Switch(
                            checked = appSettings.blockThirdPartyCookies,
                            onCheckedChange = appSettings::updateBlockThirdPartyCookies,
                        )
                    }
                    CardRow(
                        icon = Icons.Filled.VisibilityOff,
                        title = "Do Not Track",
                        subtitle = "Asks sites not to track you",
                    ) {
                        Switch(
                            checked = appSettings.doNotTrack,
                            onCheckedChange = appSettings::updateDoNotTrack,
                        )
                    }
                    CardRow(
                        icon = Icons.Filled.Lock,
                        title = "Lock Private Tabs",
                        subtitle = if (appSettings.lockPrivateTabsEnabled) {
                            "On - turn off here, or enable from Settings"
                        } else {
                            "Turn on from Settings (checks for a screen lock first)"
                        },
                    ) {
                        // Enabling needs the BiometricManager.canAuthenticate() gate in
                        // SettingsScreen; this switch can only turn the lock back off.
                        Switch(
                            checked = appSettings.lockPrivateTabsEnabled,
                            onCheckedChange = { if (!it) appSettings.updateLockPrivateTabsEnabled(false) },
                        )
                    }
                }
            }
        }
    }
}
