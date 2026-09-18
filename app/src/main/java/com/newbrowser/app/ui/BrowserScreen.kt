package com.newbrowser.app.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.PopupProperties
import com.newbrowser.app.data.AdBlocker
import com.newbrowser.app.data.BrowserDatabase
import com.newbrowser.app.data.BrowserSettings
import com.newbrowser.app.data.Suggestion
import com.newbrowser.app.data.searchEngineFor
import com.newbrowser.app.ui.tabs.TabManager
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.net.URLEncoder

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/125.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    appSettings: BrowserSettings,
    tabManager: TabManager,
    database: BrowserDatabase,
    onNavigate: (Screen) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val activeTab = tabManager.activeTab!!

    var isAddressFocused by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var menuExpanded by remember { mutableStateOf(false) }

    var findBarVisible by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findActiveOrdinal by remember { mutableIntStateOf(0) }
    var findTotal by remember { mutableIntStateOf(0) }

    var isBookmarked by remember(activeTab.url) { mutableStateOf(database.isBookmarked(activeTab.url)) }

    var longPressLinkUrl by remember { mutableStateOf<String?>(null) }
    var readerContent by remember { mutableStateOf<ReaderContent?>(null) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = appSettings.javaScriptEnabled
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, appSettings.darkModeForPages)
            }

            setOnLongClickListener {
                val result = hitTestResult
                val url = result.extra
                val isLink = result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                    result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                if (url != null && isLink) {
                    longPressLinkUrl = url
                    true
                } else {
                    false
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    val tab = tabManager.activeTab ?: return
                    tab.isLoading = true
                    tab.blockedOnPage = 0
                    url?.let { tab.url = it }
                    tab.canGoBack = view?.canGoBack() ?: false
                    tab.canGoForward = view?.canGoForward() ?: false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val tab = tabManager.activeTab ?: return
                    tab.isLoading = false
                    url?.let { tab.url = it }
                    tab.canGoBack = view?.canGoBack() ?: false
                    tab.canGoForward = view?.canGoForward() ?: false
                    if (tab.blockedOnPage > 0) {
                        appSettings.addBlockedCount(tab.blockedOnPage)
                    }
                    if (!tab.isIncognito && url != null) {
                        database.addHistoryEntry(url, tab.title.ifBlank { url })
                        tabManager.persistTabs()
                    }
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): WebResourceResponse? {
                    if (request != null &&
                        !request.isForMainFrame &&
                        appSettings.adBlockEnabled &&
                        AdBlocker.isBlockedHost(request.url.host)
                    ) {
                        tabManager.activeTab?.let { it.blockedOnPage++ }
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    tabManager.activeTab?.let { it.isLoading = newProgress in 1..99 }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    val tab = tabManager.activeTab ?: return
                    if (!title.isNullOrBlank()) {
                        tab.title = title
                        if (!tab.isIncognito) tabManager.persistTabs()
                    }
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    addRequestHeader("User-Agent", userAgent)
                    setMimeType(mimeType)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    setTitle(fileName)
                }
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)
                database.addDownload(downloadId, url, fileName)
            }

            setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                findActiveOrdinal = activeMatchOrdinal
                findTotal = numberOfMatches
            }

            tabManager.onActivate = { target, previous ->
                if (previous != null && previous.id != target.id) {
                    previous.savedState = Bundle().also { saveState(it) }
                }
                settings.userAgentString = if (target.requestDesktopSite) DESKTOP_USER_AGENT else null
                settings.cacheMode = if (target.isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                val state = target.savedState
                if (state != null) restoreState(state) else loadUrl(target.url)
            }

            loadUrl(tabManager.activeTab!!.url)
        }
    }

    // SwipeRefreshLayout uses classic touch interception rather than Compose's nested-scroll
    // protocol, which a plain embedded WebView doesn't participate in - that's why this wraps
    // the WebView natively instead of using Material3's PullToRefreshBox.
    val swipeRefreshLayout = remember {
        SwipeRefreshLayout(context).apply {
            addView(
                webView,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            setOnRefreshListener { webView.reload() }
        }
    }

    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    LaunchedEffect(activeTab.isLoading) {
        swipeRefreshLayout.isRefreshing = activeTab.isLoading
    }

    LaunchedEffect(appSettings.javaScriptEnabled) {
        webView.settings.javaScriptEnabled = appSettings.javaScriptEnabled
    }

    LaunchedEffect(appSettings.darkModeForPages) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, appSettings.darkModeForPages)
        }
    }

    LaunchedEffect(tabManager.activeTabId) {
        findBarVisible = false
        findQuery = ""
        findTotal = 0
    }

    fun navigateTo(input: String) {
        val target = normalizeUrl(input, appSettings.homeUrl, appSettings.searchEngineKey)
        activeTab.url = target
        webView.loadUrl(target)
        suggestions = emptyList()
        focusManager.clearFocus()
    }

    SideEffect {
        tabManager.onNavigate = { url -> navigateTo(url) }
    }

    fun toggleDesktopSite() {
        activeTab.requestDesktopSite = !activeTab.requestDesktopSite
        webView.settings.userAgentString = if (activeTab.requestDesktopSite) DESKTOP_USER_AGENT else null
        webView.reload()
    }

    fun sharePage() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, activeTab.url)
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    fun openReaderMode() {
        webView.evaluateJavascript(READER_EXTRACTION_JS) { rawResult ->
            val jsonText = try {
                JSONTokener(rawResult).nextValue() as? String
            } catch (e: Exception) {
                null
            }
            val parsed = jsonText?.let {
                try {
                    JSONObject(it)
                } catch (e: Exception) {
                    null
                }
            }
            if (parsed != null) {
                readerContent = ReaderContent(
                    title = parsed.optString("title").ifBlank { activeTab.title },
                    content = parsed.optString("content"),
                )
            }
        }
    }

    BackHandler(enabled = activeTab.canGoBack) {
        webView.goBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
            tonalElevation = 2.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = activeTab.url,
                            onValueChange = {
                                activeTab.url = it
                                suggestions = database.searchSuggestions(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .onFocusChanged { isAddressFocused = it.isFocused },
                            singleLine = true,
                            placeholder = { Text("Search or enter address") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { navigateTo(activeTab.url) }),
                        )

                        DropdownMenu(
                            expanded = isAddressFocused && suggestions.isNotEmpty(),
                            onDismissRequest = { suggestions = emptyList() },
                            properties = PopupProperties(focusable = false),
                        ) {
                            suggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(suggestion.title, maxLines = 1)
                                            Text(
                                                suggestion.url,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                            )
                                        }
                                    },
                                    onClick = { navigateTo(suggestion.url) },
                                )
                            }
                        }
                    }

                    IconButton(onClick = {
                        if (isBookmarked) {
                            database.removeBookmark(activeTab.url)
                        } else {
                            database.addBookmark(activeTab.url, activeTab.title)
                        }
                        isBookmarked = !isBookmarked
                    }) {
                        Icon(
                            if (isBookmarked) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Bookmark this page",
                        )
                    }
                }

                if (activeTab.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (findBarVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = {
                                findQuery = it
                                if (it.isNotEmpty()) webView.findAllAsync(it) else webView.clearMatches()
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Find in page") },
                        )
                        Text(
                            text = if (findTotal > 0) "${findActiveOrdinal + 1}/$findTotal" else "0/0",
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        IconButton(onClick = { webView.findNext(false) }) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous match")
                        }
                        IconButton(onClick = { webView.findNext(true) }) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next match")
                        }
                        IconButton(onClick = {
                            findBarVisible = false
                            findQuery = ""
                            webView.clearMatches()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close find bar")
                        }
                    }
                }
            }
        }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { swipeRefreshLayout },
        )

        BackHandler(enabled = findBarVisible) {
            findBarVisible = false
            findQuery = ""
            webView.clearMatches()
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { webView.goBack() }, enabled = activeTab.canGoBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                IconButton(onClick = { webView.goForward() }, enabled = activeTab.canGoForward) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                }
                IconButton(onClick = {
                    if (activeTab.isLoading) webView.stopLoading() else webView.reload()
                }) {
                    Icon(
                        if (activeTab.isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                        contentDescription = if (activeTab.isLoading) "Stop" else "Reload",
                    )
                }
                IconButton(onClick = { onNavigate(Screen.Tabs) }) {
                    BadgedBox(badge = {
                        if (tabManager.tabs.size > 1) {
                            Badge { Text(tabManager.tabs.size.toString()) }
                        }
                    }) {
                        Icon(Icons.Filled.Tab, contentDescription = "Tabs")
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Home") }, onClick = {
                            menuExpanded = false
                            navigateTo(appSettings.homeUrl)
                        })
                        DropdownMenuItem(text = { Text("New tab") }, onClick = {
                            menuExpanded = false
                            tabManager.newTab()
                        })
                        DropdownMenuItem(text = { Text("New private tab") }, onClick = {
                            menuExpanded = false
                            tabManager.newTab(incognito = true)
                        })
                        DropdownMenuItem(text = { Text("Bookmarks") }, onClick = {
                            menuExpanded = false
                            onNavigate(Screen.Bookmarks)
                        })
                        DropdownMenuItem(text = { Text("History") }, onClick = {
                            menuExpanded = false
                            onNavigate(Screen.History)
                        })
                        DropdownMenuItem(text = { Text("Downloads") }, onClick = {
                            menuExpanded = false
                            onNavigate(Screen.Downloads)
                        })
                        DropdownMenuItem(text = { Text("Find in page") }, onClick = {
                            menuExpanded = false
                            findBarVisible = true
                        })
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (activeTab.requestDesktopSite) "Request mobile site" else "Request desktop site",
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                toggleDesktopSite()
                            },
                        )
                        DropdownMenuItem(text = { Text("Reader mode") }, onClick = {
                            menuExpanded = false
                            openReaderMode()
                        })
                        DropdownMenuItem(text = { Text("Share page") }, onClick = {
                            menuExpanded = false
                            sharePage()
                        })
                    }
                }
                IconButton(onClick = { onNavigate(Screen.Settings) }) {
                    BadgedBox(badge = {
                        if (appSettings.adBlockEnabled && activeTab.blockedOnPage > 0) {
                            Badge { Text(activeTab.blockedOnPage.toString()) }
                        }
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }
        }
    }

    readerContent?.let { reader ->
        ReaderScreen(
            title = reader.title,
            content = reader.content,
            onClose = { readerContent = null },
        )
    }
    }

    longPressLinkUrl?.let { url ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        AlertDialog(
            onDismissRequest = { longPressLinkUrl = null },
            title = { Text(url, maxLines = 2) },
            text = {
                Column {
                    TextButton(onClick = {
                        tabManager.newTab(url = url, incognito = activeTab.isIncognito)
                        longPressLinkUrl = null
                    }) { Text("Open in new tab") }
                    TextButton(onClick = {
                        clipboard.setPrimaryClip(ClipData.newPlainText("link", url))
                        longPressLinkUrl = null
                    }) { Text("Copy link") }
                    TextButton(onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                        longPressLinkUrl = null
                    }) { Text("Share link") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { longPressLinkUrl = null }) { Text("Cancel") }
            },
        )
    }
}

private fun normalizeUrl(input: String, homeUrl: String, searchEngineKey: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return homeUrl

    val looksLikeUrl = trimmed.contains("://") ||
        (!trimmed.contains(" ") && trimmed.contains("."))

    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        looksLikeUrl -> "https://$trimmed"
        else -> {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            searchEngineFor(searchEngineKey).urlTemplate.replace("%s", encoded)
        }
    }
}

private const val READER_EXTRACTION_JS = """
(function() {
    function bestContent() {
        var candidates = document.querySelectorAll('article, main, [role="main"]');
        var best = null;
        var bestLen = 0;
        for (var i = 0; i < candidates.length; i++) {
            var len = candidates[i].innerText.length;
            if (len > bestLen) { bestLen = len; best = candidates[i]; }
        }
        if (best && bestLen > 200) return best.innerText;
        return document.body.innerText;
    }
    return JSON.stringify({ title: document.title, content: bestContent() });
})();
"""

data class ReaderContent(val title: String, val content: String)
