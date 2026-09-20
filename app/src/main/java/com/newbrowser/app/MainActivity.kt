package com.newbrowser.app

import android.app.PictureInPictureParams
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.newbrowser.app.data.BrowserPreferences
import com.newbrowser.app.data.BrowserSettings
import com.newbrowser.app.data.CYN_THEME_INDEX
import com.newbrowser.app.ui.NewBrowserApp
import com.newbrowser.app.ui.PipController
import com.newbrowser.app.ui.PrivacyLockController
import com.newbrowser.app.ui.theme.NewBrowserTheme

class MainActivity : FragmentActivity() {
    private var pendingUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Created here rather than inside NewBrowserApp so the theme wrapper - which has to sit
        // outside NewBrowserApp to color its own background - can read the chosen theme color too.
        val appSettings = BrowserSettings(BrowserPreferences(this))
        // Cyn is always dark regardless of the system setting, so its status/nav bar icons need
        // to be forced light too; every other theme choice still follows the system light/dark
        // detection enableEdgeToEdge() does on its own.
        if (appSettings.themeColorIndex == CYN_THEME_INDEX) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
        } else {
            enableEdgeToEdge()
        }
        pendingUrl = intent?.dataString
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

    // Leaving the app (Home, recents, another app, screen off) is the moment private tabs
    // need to be hidden behind a lock screen - not onPause(), which also fires for a
    // permission dialog or the share sheet sitting on top of us.
    override fun onStop() {
        super.onStop()
        PrivacyLockController.onAppBackgrounded()
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
