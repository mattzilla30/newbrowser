package com.newbrowser.app.ui

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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

    SideEffect {
        PrivacyLockController.lockEnabled = appSettings.lockPrivateTabsEnabled
        PrivacyLockController.hasIncognitoTabs = tabManager.tabs.any { it.isIncognito }
    }

    LaunchedEffect(pendingUrl) {
        if (pendingUrl != null) {
            // A Web Head bubble targets an already-open background tab by URL; reuse it
            // instead of opening a duplicate. Any other external VIEW intent (the common
            // case) just opens a fresh tab as before, since nothing will match.
            val existing = tabManager.tabs.find { it.url == pendingUrl }
            if (existing != null) tabManager.selectTab(existing) else tabManager.newTab(url = pendingUrl)
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

            Screen.SafeLocker -> SafeLockerScreen(
                onBack = { screen = Screen.Browser },
            )
        }

        if (PrivacyLockController.isLocked) {
            LockScreen(
                onUnlocked = { PrivacyLockController.unlock() },
                onCloseTabsInstead = {
                    tabManager.tabs.filter { it.isIncognito }.forEach(tabManager::closeTab)
                    PrivacyLockController.unlock()
                },
            )
        }
    }
}
