package com.newbrowser.app.ui

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.newbrowser.app.data.BrowserDatabase
import com.newbrowser.app.data.BrowserSettings
import com.newbrowser.app.ui.tabs.TabManager

@Composable
fun NewBrowserApp(
    appSettings: BrowserSettings,
    pendingUrl: String? = null,
    onPendingUrlConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val database = remember { BrowserDatabase(context) }
    val tabManager = remember { TabManager(database) }

    var screen by remember { mutableStateOf(Screen.Browser) }

    LaunchedEffect(appSettings.remoteDebuggingEnabled) {
        WebView.setWebContentsDebuggingEnabled(appSettings.remoteDebuggingEnabled)
    }

    LaunchedEffect(pendingUrl) {
        if (pendingUrl != null) {
            tabManager.newTab(url = pendingUrl)
            screen = Screen.Browser
            onPendingUrlConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BrowserScreen(
            appSettings = appSettings,
            tabManager = tabManager,
            database = database,
            onNavigate = { screen = it },
        )

        when (screen) {
            Screen.Browser -> Unit

            Screen.Settings -> SettingsScreen(
                appSettings = appSettings,
                onClearBrowsingData = {
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData()
                    database.clearHistory()
                },
                onBack = { screen = Screen.Browser },
            )

            Screen.Tabs -> TabSwitcherScreen(
                tabManager = tabManager,
                onBack = { screen = Screen.Browser },
            )

            Screen.Bookmarks -> BookmarksScreen(
                database = database,
                onOpen = { url ->
                    tabManager.onNavigate?.invoke(url)
                    screen = Screen.Browser
                },
                onBack = { screen = Screen.Browser },
            )

            Screen.ReadingList -> ReadingListScreen(
                database = database,
                onOpen = { url ->
                    tabManager.onNavigate?.invoke(url)
                    screen = Screen.Browser
                },
                onBack = { screen = Screen.Browser },
            )

            Screen.History -> HistoryScreen(
                database = database,
                onOpen = { url ->
                    tabManager.onNavigate?.invoke(url)
                    screen = Screen.Browser
                },
                onBack = { screen = Screen.Browser },
            )

            Screen.Downloads -> DownloadsScreen(
                database = database,
                onOpenSavedPage = { url ->
                    tabManager.onNavigate?.invoke(url)
                    screen = Screen.Browser
                },
                onBack = { screen = Screen.Browser },
            )

            Screen.SitePermissions -> SitePermissionsScreen(
                database = database,
                onBack = { screen = Screen.Browser },
            )
        }
    }
}
