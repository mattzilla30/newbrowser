package com.newbrowser.app.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.newbrowser.app.data.BrowserDatabase
import com.newbrowser.app.data.PersistedTab

/**
 * Owns the list of open tabs and which one is active. Holds no WebView itself: the
 * screen that owns the single shared WebView sets [onActivate] once, and every tab
 * switch/creation/close routes through it so that screen can save/restore WebView
 * state without this class needing to know anything about WebView.
 *
 * Non-incognito tabs are persisted to [database] on every change so they survive the
 * app process being killed and restarted.
 */
class TabManager(
    private val database: BrowserDatabase,
    private val defaultUrl: () -> String,
) {
    val tabs = mutableStateListOf<BrowserTab>()

    var activeTabId by mutableStateOf<Long?>(null)
        private set

    private var nextId = 0L

    /** Set by the screen that owns the shared WebView; invoked whenever the active tab changes. */
    var onActivate: ((target: BrowserTab, previous: BrowserTab?) -> Unit)? = null

    /** Set by the screen that owns the shared WebView; lets other screens request a navigation. */
    var onNavigate: ((url: String) -> Unit)? = null

    init {
        val restored = database.getOpenTabs()
        if (restored.isEmpty()) {
            newTab()
        } else {
            restored.forEach { persisted ->
                val tab = BrowserTab(id = nextId++, isIncognito = false, initialUrl = persisted.url)
                tab.title = persisted.title
                tabs.add(tab)
                if (persisted.isActive) activeTabId = tab.id
            }
            if (activeTabId == null) activeTabId = tabs.first().id
        }
    }

    val activeTab: BrowserTab?
        get() = tabs.find { it.id == activeTabId }

    fun newTab(url: String = defaultUrl(), incognito: Boolean = false): BrowserTab {
        val previous = activeTab
        val tab = BrowserTab(id = nextId++, isIncognito = incognito, initialUrl = url)
        tabs.add(tab)
        activeTabId = tab.id
        onActivate?.invoke(tab, previous)
        persistTabs()
        return tab
    }

    fun selectTab(tab: BrowserTab) {
        if (tab.id == activeTabId) return
        val previous = activeTab
        activeTabId = tab.id
        onActivate?.invoke(tab, previous)
        persistTabs()
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
        persistTabs()
    }

    /** Call after a tab's url/title change (e.g. on page load) so the restore point stays current. */
    fun persistTabs() {
        database.saveOpenTabs(
            tabs.filterNot { it.isIncognito }.map { PersistedTab(it.url, it.title, it.id == activeTabId) },
        )
    }
}
