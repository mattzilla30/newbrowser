package com.newbrowser.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Single shared, live source of truth for user-configurable settings.
 *
 * Both the Settings UI and the tab/WebView plumbing read this object directly, so a
 * change made in Settings is visible immediately to every tab. Every setter also
 * persists to [preferences], so callers never have to remember to do that themselves.
 */
class BrowserSettings(private val preferences: BrowserPreferences) {
    var homeUrl by mutableStateOf(preferences.homeUrl)
        private set

    var javaScriptEnabled by mutableStateOf(preferences.javaScriptEnabled)
        private set

    var searchEngineKey by mutableStateOf(preferences.searchEngineKey)
        private set

    var darkModeForPages by mutableStateOf(preferences.darkModeForPages)
        private set

    var blockPopups by mutableStateOf(preferences.blockPopups)
        private set

    var doNotTrack by mutableStateOf(preferences.doNotTrack)
        private set

    fun updateHomeUrl(value: String) {
        homeUrl = value
        preferences.homeUrl = value
    }

    fun updateJavaScriptEnabled(value: Boolean) {
        javaScriptEnabled = value
        preferences.javaScriptEnabled = value
    }

    fun updateSearchEngineKey(value: String) {
        searchEngineKey = value
        preferences.searchEngineKey = value
    }

    fun updateDarkModeForPages(value: Boolean) {
        darkModeForPages = value
        preferences.darkModeForPages = value
    }

    fun updateBlockPopups(value: Boolean) {
        blockPopups = value
        preferences.blockPopups = value
    }

    fun updateDoNotTrack(value: Boolean) {
        doNotTrack = value
        preferences.doNotTrack = value
    }
}
