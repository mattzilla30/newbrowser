package com.newbrowser.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.newbrowser.app.MainActivity
import com.newbrowser.app.R
import com.newbrowser.app.data.BrowserDatabase
import com.newbrowser.app.data.BrowserSettings
import com.newbrowser.app.data.Suggestion
import com.newbrowser.app.data.searchEngineFor
import com.newbrowser.app.ui.tabs.TabManager
import org.json.JSONObject
import org.json.JSONTokener
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

    var jsDialogRequest by remember { mutableStateOf<JsDialogRequest?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeoRequest by remember { mutableStateOf<GeoRequest?>(null) }
    val inFlightPermissionRequest = remember { mutableStateOf<PermissionRequest?>(null) }
    val inFlightGeoRequest = remember { mutableStateOf<GeoRequest?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = filePathCallback
        filePathCallback = null
        val data = result.data
        val uris = if (result.resultCode == Activity.RESULT_OK && data != null) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, data) ?: emptyArray()
        } else {
            emptyArray()
        }
        callback?.onReceiveValue(uris)
    }

    val cameraMicPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val request = inFlightPermissionRequest.value
        inFlightPermissionRequest.value = null
        if (request == null) return@rememberLauncherForActivityResult
        val granted = request.resources.filter { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> grants[Manifest.permission.CAMERA] == true
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> grants[Manifest.permission.RECORD_AUDIO] == true
                else -> false
            }
        }
        database.setSitePermission(originOf(request.origin.toString()), "media", granted.isNotEmpty())
        if (granted.isNotEmpty()) request.grant(granted.toTypedArray()) else request.deny()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val request = inFlightGeoRequest.value
        inFlightGeoRequest.value = null
        request?.let {
            database.setSitePermission(originOf(it.origin), "location", granted)
            it.callback.invoke(it.origin, granted, false)
        }
    }

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
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)

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
                private var lastDntReissuedUrl: String? = null

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (appSettings.doNotTrack && request.isForMainFrame) {
                        val url = request.url.toString()
                        if (url != lastDntReissuedUrl) {
                            lastDntReissuedUrl = url
                            view.loadUrl(url, mapOf("DNT" to "1"))
                            return true
                        }
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    val tab = tabManager.activeTab ?: return
                    tab.isLoading = true
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
                    if (!tab.isIncognito && url != null) {
                        database.addHistoryEntry(url, tab.title.ifBlank { url })
                        tabManager.persistTabs()
                    }
                    lastDntReissuedUrl = null
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        view?.loadUrl(errorPageDataUrl(request.url.toString()))
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                private var fullscreenContainer: ViewGroup? = null
                private var fullscreenCallback: CustomViewCallback? = null

                // isLoading is owned by onPageStarted/onPageFinished only, deliberately: WebView
                // doesn't strictly guarantee a final onProgressChanged(100) always fires (e.g.
                // for cached or instant loads), and a stray onProgressChanged callback with
                // newProgress < 100 arriving after onPageFinished could otherwise leave
                // isLoading stuck true - which, mirrored into SwipeRefreshLayout.isRefreshing,
                // blocks all touch input on the page.

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    val tab = tabManager.activeTab ?: return
                    if (!title.isNullOrBlank()) {
                        tab.title = title
                        if (!tab.isIncognito) tabManager.persistTabs()
                    }
                }

                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    jsDialogRequest = JsDialogRequest.Alert(message.orEmpty(), result)
                    return true
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                    jsDialogRequest = JsDialogRequest.Confirm(message.orEmpty(), result)
                    return true
                }

                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult,
                ): Boolean {
                    jsDialogRequest = JsDialogRequest.Prompt(message.orEmpty(), defaultValue.orEmpty(), result)
                    return true
                }

                override fun onShowFileChooser(
                    view: WebView?,
                    filePathCallbackParam: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?,
                ): Boolean {
                    val intent = fileChooserParams?.createIntent() ?: return false
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = filePathCallbackParam
                    return try {
                        fileChooserLauncher.launch(intent)
                        true
                    } catch (e: Exception) {
                        filePathCallback = null
                        false
                    }
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null) return
                    val activity = context.findActivity() ?: return
                    val decorView = activity.window.decorView as? ViewGroup ?: return
                    val container = FrameLayout(activity).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        addView(view)
                    }
                    decorView.addView(container)
                    fullscreenContainer = container
                    fullscreenCallback = callback
                    WindowInsetsControllerCompat(activity.window, decorView).apply {
                        hide(WindowInsetsCompat.Type.systemBars())
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }

                override fun onHideCustomView() {
                    val activity = context.findActivity() ?: return
                    val decorView = activity.window.decorView as? ViewGroup ?: return
                    fullscreenContainer?.let { decorView.removeView(it) }
                    fullscreenContainer = null
                    fullscreenCallback?.onCustomViewHidden()
                    fullscreenCallback = null
                    WindowInsetsControllerCompat(activity.window, decorView).show(WindowInsetsCompat.Type.systemBars())
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (request == null) return
                    val origin = originOf(request.origin.toString())
                    when (database.getSitePermission(origin, "media")) {
                        true -> {
                            val needed = buildList {
                                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) add(Manifest.permission.CAMERA)
                                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) add(Manifest.permission.RECORD_AUDIO)
                            }
                            val allOsGranted = needed.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            if (allOsGranted) request.grant(request.resources) else pendingPermissionRequest = request
                        }
                        false -> request.deny()
                        null -> pendingPermissionRequest = request
                    }
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?,
                ) {
                    if (origin == null || callback == null) return
                    val cleanOrigin = originOf(origin)
                    when (database.getSitePermission(cleanOrigin, "location")) {
                        true -> {
                            val osGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (osGranted) callback.invoke(origin, true, false) else pendingGeoRequest = GeoRequest(origin, callback)
                        }
                        false -> callback.invoke(origin, false, false)
                        null -> pendingGeoRequest = GeoRequest(origin, callback)
                    }
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?,
                ): Boolean {
                    if (appSettings.blockPopups && !isUserGesture) return false
                    val transport = WebView(context)
                    transport.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            tabManager.newTab(url = request.url.toString(), incognito = activeTab.isIncognito)
                            transport.destroy()
                            return true
                        }
                    }
                    val transportObject = resultMsg?.obj as? WebView.WebViewTransport ?: run {
                        transport.destroy()
                        return false
                    }
                    transportObject.webView = transport
                    resultMsg.sendToTarget()
                    return true
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
    // the WebView natively instead of using Material3's PullToRefreshBox. canChildScrollUp is
    // overridden to check the WebView's own scrollY directly, rather than relying on the
    // default View.canScrollVertically() path, so a downward drag partway down a page always
    // scrolls the page instead of being claimed for the refresh gesture.
    val swipeRefreshLayout = remember {
        object : SwipeRefreshLayout(context) {
            override fun canChildScrollUp(): Boolean = webView.scrollY > 0
        }.apply {
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

    fun printPage() {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = activeTab.title.ifBlank { "Web page" }
        val adapter = webView.createPrintDocumentAdapter(jobName)
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    fun addToHomeScreen() {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(activeTab.url)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, "site_${System.currentTimeMillis()}")
            .setShortLabel(activeTab.title.take(30).ifBlank { activeTab.url })
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(shortcutIntent)
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
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
                        DropdownMenuItem(text = { Text("Print") }, onClick = {
                            menuExpanded = false
                            printPage()
                        })
                        DropdownMenuItem(text = { Text("Add to Home screen") }, onClick = {
                            menuExpanded = false
                            addToHomeScreen()
                        })
                        DropdownMenuItem(text = { Text("Site permissions") }, onClick = {
                            menuExpanded = false
                            onNavigate(Screen.SitePermissions)
                        })
                    }
                }
                IconButton(onClick = { onNavigate(Screen.Settings) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
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

    jsDialogRequest?.let { request ->
        when (request) {
            is JsDialogRequest.Alert -> AlertDialog(
                onDismissRequest = {
                    request.result.cancel()
                    jsDialogRequest = null
                },
                title = { Text(activeTab.url) },
                text = { Text(request.message) },
                confirmButton = {
                    TextButton(onClick = {
                        request.result.confirm()
                        jsDialogRequest = null
                    }) { Text("OK") }
                },
            )

            is JsDialogRequest.Confirm -> AlertDialog(
                onDismissRequest = {
                    request.result.cancel()
                    jsDialogRequest = null
                },
                title = { Text(activeTab.url) },
                text = { Text(request.message) },
                confirmButton = {
                    TextButton(onClick = {
                        request.result.confirm()
                        jsDialogRequest = null
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        request.result.cancel()
                        jsDialogRequest = null
                    }) { Text("Cancel") }
                },
            )

            is JsDialogRequest.Prompt -> {
                var promptValue by remember(request) { mutableStateOf(request.defaultValue) }
                AlertDialog(
                    onDismissRequest = {
                        request.result.cancel()
                        jsDialogRequest = null
                    },
                    title = { Text(activeTab.url) },
                    text = {
                        Column {
                            Text(request.message)
                            OutlinedTextField(
                                value = promptValue,
                                onValueChange = { promptValue = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            request.result.confirm(promptValue)
                            jsDialogRequest = null
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            request.result.cancel()
                            jsDialogRequest = null
                        }) { Text("Cancel") }
                    },
                )
            }
        }
    }

    pendingPermissionRequest?.let { request ->
        val label = if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) {
            "camera and microphone"
        } else {
            "microphone"
        }
        AlertDialog(
            onDismissRequest = {
                database.setSitePermission(originOf(request.origin.toString()), "media", false)
                request.deny()
                pendingPermissionRequest = null
            },
            title = { Text("Permission request") },
            text = { Text("${request.origin} wants to use your $label.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingPermissionRequest = null
                    val androidPermissions = buildList {
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) add(Manifest.permission.CAMERA)
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (androidPermissions.isEmpty()) {
                        request.deny()
                    } else {
                        inFlightPermissionRequest.value = request
                        cameraMicPermissionLauncher.launch(androidPermissions.toTypedArray())
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    database.setSitePermission(originOf(request.origin.toString()), "media", false)
                    request.deny()
                    pendingPermissionRequest = null
                }) { Text("Deny") }
            },
        )
    }

    pendingGeoRequest?.let { request ->
        AlertDialog(
            onDismissRequest = {
                database.setSitePermission(originOf(request.origin), "location", false)
                request.callback.invoke(request.origin, false, false)
                pendingGeoRequest = null
            },
            title = { Text("Location request") },
            text = { Text("${request.origin} wants to use your location.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingGeoRequest = null
                    inFlightGeoRequest.value = request
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    database.setSitePermission(originOf(request.origin), "location", false)
                    request.callback.invoke(request.origin, false, false)
                    pendingGeoRequest = null
                }) { Text("Deny") }
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

private data class GeoRequest(val origin: String, val callback: GeolocationPermissions.Callback)

private sealed class JsDialogRequest {
    data class Alert(val message: String, val result: JsResult) : JsDialogRequest()
    data class Confirm(val message: String, val result: JsResult) : JsDialogRequest()
    data class Prompt(val message: String, val defaultValue: String, val result: JsPromptResult) : JsDialogRequest()
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun errorPageDataUrl(failedUrl: String): String {
    val html = """
        <html><body style="font-family:sans-serif;text-align:center;padding-top:80px;color:#666;">
        <h2>This page couldn't load</h2>
        <p style="word-break:break-all;">$failedUrl</p>
        </body></html>
    """.trimIndent()
    return "data:text/html;charset=utf-8," + Uri.encode(html)
}

/** Normalizes a WebView-supplied origin/URL string down to a host, for use as a permission-memory key. */
private fun originOf(originOrUrl: String): String = Uri.parse(originOrUrl).host ?: originOrUrl
