package com.newbrowser.app.ui.tabs

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Per-tab state. There is a single shared WebView (owned by BrowserScreen); switching
 * tabs saves the outgoing tab's navigation state into [savedState] and restores the
 * incoming tab's state into that same WebView, rather than keeping one WebView alive
 * per tab.
 */
class BrowserTab(
    val id: Long,
    val isIncognito: Boolean,
    initialUrl: String,
) {
    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf(initialUrl)
    var isLoading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var requestDesktopSite by mutableStateOf(false)
    var groupId by mutableStateOf<String?>(null)
    var groupName by mutableStateOf<String?>(null)
    var groupColorIndex by mutableStateOf(0)
    var favicon by mutableStateOf<Bitmap?>(null)
    var savedState: Bundle? = null
}
