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

    var remoteDebuggingEnabled by mutableStateOf(preferences.remoteDebuggingEnabled)
        private set

    var pageTextZoom by mutableStateOf(preferences.pageTextZoom)
        private set

    var themeColorIndex by mutableStateOf(preferences.themeColorIndex)
        private set

    var lockPrivateTabsEnabled by mutableStateOf(preferences.lockPrivateTabsEnabled)
        private set

    var blockThirdPartyCookies by mutableStateOf(preferences.blockThirdPartyCookies)
        private set

    var webHeadsEnabled by mutableStateOf(preferences.webHeadsEnabled)
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

    fun updateRemoteDebuggingEnabled(value: Boolean) {
        remoteDebuggingEnabled = value
        preferences.remoteDebuggingEnabled = value
    }

    fun updatePageTextZoom(value: Int) {
        pageTextZoom = value
        preferences.pageTextZoom = value
    }

    fun updateThemeColorIndex(value: Int) {
        themeColorIndex = value
        preferences.themeColorIndex = value
    }

    fun updateLockPrivateTabsEnabled(value: Boolean) {
        lockPrivateTabsEnabled = value
        preferences.lockPrivateTabsEnabled = value
    }

    fun updateBlockThirdPartyCookies(value: Boolean) {
        blockThirdPartyCookies = value
        preferences.blockThirdPartyCookies = value
    }

    fun updateWebHeadsEnabled(value: Boolean) {
        webHeadsEnabled = value
        preferences.webHeadsEnabled = value
    }
}
