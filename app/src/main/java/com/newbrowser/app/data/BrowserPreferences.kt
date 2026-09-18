package com.newbrowser.app.data

import android.content.Context

private const val PREFS_NAME = "newbrowser_settings"
private const val KEY_HOME_URL = "home_url"
private const val KEY_JAVASCRIPT_ENABLED = "javascript_enabled"
private const val DEFAULT_HOME_URL = "https://duckduckgo.com"

class BrowserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var homeUrl: String
        get() = prefs.getString(KEY_HOME_URL, DEFAULT_HOME_URL) ?: DEFAULT_HOME_URL
        set(value) = prefs.edit().putString(KEY_HOME_URL, value).apply()

    var javaScriptEnabled: Boolean
        get() = prefs.getBoolean(KEY_JAVASCRIPT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_JAVASCRIPT_ENABLED, value).apply()
}
