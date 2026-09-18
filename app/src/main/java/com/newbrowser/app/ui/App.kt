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
import com.newbrowser.app.data.BrowserPreferences

private enum class Screen { Browser, Settings }

@Composable
fun NewBrowserApp() {
    val context = LocalContext.current
    val preferences = remember { BrowserPreferences(context) }

    var screen by remember { mutableStateOf(Screen.Browser) }
    var homeUrl by remember { mutableStateOf(preferences.homeUrl) }
    var javaScriptEnabled by remember { mutableStateOf(preferences.javaScriptEnabled) }

    Box(modifier = Modifier.fillMaxSize()) {
        BrowserScreen(
            homeUrl = homeUrl,
            javaScriptEnabled = javaScriptEnabled,
            onOpenSettings = { screen = Screen.Settings },
        )

        if (screen == Screen.Settings) {
            SettingsScreen(
                homeUrl = homeUrl,
                javaScriptEnabled = javaScriptEnabled,
                onHomeUrlChange = {
                    homeUrl = it
                    preferences.homeUrl = it
                },
                onJavaScriptEnabledChange = {
                    javaScriptEnabled = it
                    preferences.javaScriptEnabled = it
                },
                onClearBrowsingData = {
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData()
                },
                onBack = { screen = Screen.Browser },
            )
        }
    }
}
