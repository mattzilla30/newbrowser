package com.newbrowser.app.data

import android.content.Context

private const val PREFS_NAME = "newbrowser_settings"
private const val KEY_HOME_URL = "home_url"
private const val KEY_JAVASCRIPT_ENABLED = "javascript_enabled"
private const val KEY_AD_BLOCK_ENABLED = "ad_block_enabled"
private const val KEY_TOTAL_BLOCKED_COUNT = "total_blocked_count"
private const val KEY_SEARCH_ENGINE = "search_engine"
private const val KEY_DARK_MODE_FOR_PAGES = "dark_mode_for_pages"
private const val DEFAULT_HOME_URL = "https://duckduckgo.com"
private const val DEFAULT_SEARCH_ENGINE = "duckduckgo"

class BrowserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var homeUrl: String
        get() = prefs.getString(KEY_HOME_URL, DEFAULT_HOME_URL) ?: DEFAULT_HOME_URL
        set(value) = prefs.edit().putString(KEY_HOME_URL, value).apply()

    var javaScriptEnabled: Boolean
        get() = prefs.getBoolean(KEY_JAVASCRIPT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_JAVASCRIPT_ENABLED, value).apply()

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AD_BLOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AD_BLOCK_ENABLED, value).apply()

    var totalBlockedCount: Long
        get() = prefs.getLong(KEY_TOTAL_BLOCKED_COUNT, 0L)
        set(value) = prefs.edit().putLong(KEY_TOTAL_BLOCKED_COUNT, value).apply()

    var searchEngineKey: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, DEFAULT_SEARCH_ENGINE) ?: DEFAULT_SEARCH_ENGINE
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    var darkModeForPages: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE_FOR_PAGES, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE_FOR_PAGES, value).apply()
}
