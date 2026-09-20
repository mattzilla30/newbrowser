package com.newbrowser.app.data

import android.content.Context

private const val PREFS_NAME = "newbrowser_settings"
private const val KEY_HOME_URL = "home_url"
private const val KEY_JAVASCRIPT_ENABLED = "javascript_enabled"
private const val KEY_SEARCH_ENGINE = "search_engine"
private const val KEY_DARK_MODE_FOR_PAGES = "dark_mode_for_pages"
private const val KEY_BLOCK_POPUPS = "block_popups"
private const val KEY_DO_NOT_TRACK = "do_not_track"
private const val KEY_REMOTE_DEBUGGING_ENABLED = "remote_debugging_enabled"
private const val KEY_PAGE_TEXT_ZOOM = "page_text_zoom"
private const val KEY_THEME_COLOR_INDEX = "theme_color_index"
private const val KEY_LOCK_PRIVATE_TABS = "lock_private_tabs_enabled"
private const val KEY_BLOCK_THIRD_PARTY_COOKIES = "block_third_party_cookies"
private const val DEFAULT_HOME_URL = "https://duckduckgo.com"
private const val DEFAULT_SEARCH_ENGINE = "duckduckgo"

/** Sentinel for [BrowserPreferences.themeColorIndex]: the app's own Cyn theme, not a preset. */
const val CYN_THEME_INDEX = -2

/** Sentinel for [BrowserPreferences.themeColorIndex]: follow the system/Material You color. */
const val SYSTEM_THEME_INDEX = -1

class BrowserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var homeUrl: String
        get() = prefs.getString(KEY_HOME_URL, DEFAULT_HOME_URL) ?: DEFAULT_HOME_URL
        set(value) = prefs.edit().putString(KEY_HOME_URL, value).apply()

    var javaScriptEnabled: Boolean
        get() = prefs.getBoolean(KEY_JAVASCRIPT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_JAVASCRIPT_ENABLED, value).apply()

    var searchEngineKey: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, DEFAULT_SEARCH_ENGINE) ?: DEFAULT_SEARCH_ENGINE
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    var darkModeForPages: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE_FOR_PAGES, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE_FOR_PAGES, value).apply()

    var blockPopups: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_POPUPS, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_POPUPS, value).apply()

    var doNotTrack: Boolean
        get() = prefs.getBoolean(KEY_DO_NOT_TRACK, false)
        set(value) = prefs.edit().putBoolean(KEY_DO_NOT_TRACK, value).apply()

    var remoteDebuggingEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMOTE_DEBUGGING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_REMOTE_DEBUGGING_ENABLED, value).apply()

    /** WebView's textZoom percentage; 100 is normal size. */
    var pageTextZoom: Int
        get() = prefs.getInt(KEY_PAGE_TEXT_ZOOM, 100)
        set(value) = prefs.edit().putInt(KEY_PAGE_TEXT_ZOOM, value).apply()

    /** Index into a preset palette, or one of the sentinels above. Defaults to the Cyn theme. */
    var themeColorIndex: Int
        get() = prefs.getInt(KEY_THEME_COLOR_INDEX, CYN_THEME_INDEX)
        set(value) = prefs.edit().putInt(KEY_THEME_COLOR_INDEX, value).apply()

    /** Requires biometric/PIN authentication before showing private tabs after the app backgrounds. */
    var lockPrivateTabsEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_PRIVATE_TABS, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_PRIVATE_TABS, value).apply()

    var blockThirdPartyCookies: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_THIRD_PARTY_COOKIES, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_THIRD_PARTY_COOKIES, value).apply()
}
