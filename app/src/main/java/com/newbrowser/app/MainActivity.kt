package com.newbrowser.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.newbrowser.app.ui.BrowserScreen
import com.newbrowser.app.ui.theme.NewBrowserTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewBrowserTheme {
                BrowserScreen()
            }
        }
    }
}
