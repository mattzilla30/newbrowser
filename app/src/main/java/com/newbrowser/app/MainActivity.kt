package com.newbrowser.app

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.newbrowser.app.data.BrowserPreferences
import com.newbrowser.app.data.BrowserSettings
import com.newbrowser.app.ui.NewBrowserApp
import com.newbrowser.app.ui.PipController
import com.newbrowser.app.ui.theme.NewBrowserTheme

class MainActivity : ComponentActivity() {
    private var pendingUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingUrl = intent?.dataString
        // Created here rather than inside NewBrowserApp so the theme wrapper - which has to sit
        // outside NewBrowserApp to color its own background - can read the chosen theme color too.
        val appSettings = BrowserSettings(BrowserPreferences(this))
        setContent {
            NewBrowserTheme(themeColorIndex = appSettings.themeColorIndex) {
                NewBrowserApp(
                    appSettings = appSettings,
                    pendingUrl = pendingUrl,
                    onPendingUrlConsumed = { pendingUrl = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingUrl = intent.dataString
    }

    // Fires when the user leaves via Home/recents/another app, not on every pause (e.g. not
    // for a permission dialog or the share sheet) - exactly when a playing fullscreen video
    // should follow into a floating window instead of just stopping off-screen.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (PipController.isFullscreenVideoActive) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }
}
