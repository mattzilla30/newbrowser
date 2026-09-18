package com.newbrowser.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.newbrowser.app.ui.NewBrowserApp
import com.newbrowser.app.ui.theme.NewBrowserTheme

class MainActivity : ComponentActivity() {
    private var pendingUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingUrl = intent?.dataString
        setContent {
            NewBrowserTheme {
                NewBrowserApp(
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
}
