package com.newbrowser.app.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Owns the list of open tabs and which one is active. Holds no WebView itself: the
 * screen that owns the single shared WebView sets [onActivate] once, and every tab
 * switch/creation/close routes through it so that screen can save/restore WebView
 * state without this class needing to know anything about WebView.
 */
class TabManager(private val defaultUrl: () -> String) {
    val tabs = mutableStateListOf<BrowserTab>()

    var activeTabId by mutableStateOf<Long?>(null)
        private set

    private var nextId = 0L

    /** Set by the screen that owns the shared WebView; invoked whenever the active tab changes. */
    var onActivate: ((target: BrowserTab, previous: BrowserTab?) -> Unit)? = null

    /** Set by the screen that owns the shared WebView; lets other screens request a navigation. */
    var onNavigate: ((url: String) -> Unit)? = null

    init {
        newTab()
    }

    val activeTab: BrowserTab?
        get() = tabs.find { it.id == activeTabId }

    fun newTab(url: String = defaultUrl(), incognito: Boolean = false): BrowserTab {
        val previous = activeTab
        val tab = BrowserTab(id = nextId++, isIncognito = incognito, initialUrl = url)
        tabs.add(tab)
        activeTabId = tab.id
        onActivate?.invoke(tab, previous)
        return tab
    }

    fun selectTab(tab: BrowserTab) {
        if (tab.id == activeTabId) return
        val previous = activeTab
        activeTabId = tab.id
        onActivate?.invoke(tab, previous)
    }

    fun closeTab(tab: BrowserTab) {
        val index = tabs.indexOf(tab)
        if (index == -1) return
        val wasActive = activeTabId == tab.id
        tabs.removeAt(index)

        if (tabs.isEmpty()) {
            newTab()
            return
        }

        if (wasActive) {
            val next = tabs.getOrNull(index) ?: tabs.getOrNull(index - 1)!!
            activeTabId = next.id
            onActivate?.invoke(next, null)
        }
    }
}
