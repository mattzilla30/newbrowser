package com.newbrowser.app.ui

import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.newbrowser.app.data.BrowserDatabase
import com.newbrowser.app.data.BrowserPreferences
import com.newbrowser.app.data.BrowserSettings
import com.newbrowser.app.ui.tabs.TabManager

@Composable
fun NewBrowserApp() {
    val context = LocalContext.current
    val preferences = remember { BrowserPreferences(context) }
    val appSettings = remember { BrowserSettings(preferences) }
    val database = remember { BrowserDatabase(context) }
    val tabManager = remember { TabManager { appSettings.homeUrl } }

    var screen by remember { mutableStateOf(Screen.Browser) }

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
                onBack = { screen = Screen.Browser },
            )
        }
    }
}
