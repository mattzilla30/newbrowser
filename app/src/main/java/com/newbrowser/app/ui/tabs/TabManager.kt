package com.newbrowser.app.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.newbrowser.app.data.BrowserDatabase
import com.newbrowser.app.data.PersistedTab

/** Sentinel URL for a tab showing the new-tab speed dial instead of a real page. */
const val NEW_TAB_URL = "cynbrowse://newtab"

/** A named, color-coded collection of tabs, derived from what's currently assigned to it. */
data class TabGroup(val id: String, val name: String, val colorIndex: Int)

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
) {
    val tabs = mutableStateListOf<BrowserTab>()

    /** Non-incognito tabs closed recently, most recent first, for a "reopen" action. */
    val recentlyClosed = mutableStateListOf<PersistedTab>()

    var activeTabId by mutableStateOf<Long?>(null)
        private set

    private var nextId = 0L

    /** Set by the screen that owns the shared WebView; invoked whenever the active tab changes. */
    var onActivate: ((target: BrowserTab, previous: BrowserTab?) -> Unit)? = null

    /** Set by the screen that owns the shared WebView; lets other screens request a navigation. */
    var onNavigate: ((url: String) -> Unit)? = null

    /** Every group with at least one tab in it, in first-seen order. */
    val groups: List<TabGroup>
        get() = tabs.mapNotNull { tab ->
            tab.groupId?.let { TabGroup(it, tab.groupName ?: "Group", tab.groupColorIndex) }
        }.distinctBy { it.id }

    init {
        val restored = database.getOpenTabs()
        if (restored.isEmpty()) {
            newTab()
        } else {
            restored.forEach { persisted ->
                val tab = BrowserTab(id = nextId++, isIncognito = false, initialUrl = persisted.url)
                tab.title = if (persisted.url == NEW_TAB_URL) "New Tab" else persisted.title
                tab.groupId = persisted.groupId
                tab.groupName = persisted.groupName
                tab.groupColorIndex = persisted.groupColorIndex
                tabs.add(tab)
                if (persisted.isActive) activeTabId = tab.id
            }
            if (activeTabId == null) activeTabId = tabs.first().id
        }
    }

    val activeTab: BrowserTab?
        get() = tabs.find { it.id == activeTabId }

    fun newTab(url: String = NEW_TAB_URL, incognito: Boolean = false): BrowserTab {
        val previous = activeTab
        val tab = BrowserTab(id = nextId++, isIncognito = incognito, initialUrl = url)
        if (url == NEW_TAB_URL) tab.title = "New Tab"
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
        recordClosed(tab)
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

    /** Closes every tab except [keep], leaving the tab list with just that one active tab. */
    fun closeOtherTabs(keep: BrowserTab) {
        val previous = activeTab
        tabs.filterNot { it.id == keep.id }.forEach { recordClosed(it) }
        tabs.retainAll { it.id == keep.id }
        if (activeTabId != keep.id) {
            activeTabId = keep.id
            onActivate?.invoke(keep, previous)
        }
        persistTabs()
    }

    /** Closes every tab and replaces them with a single fresh new-tab page. */
    fun closeAllTabs() {
        tabs.forEach { recordClosed(it) }
        tabs.clear()
        newTab()
    }

    /** Reopens a recently-closed tab and drops it from the list. */
    fun reopenRecentlyClosed(entry: PersistedTab) {
        recentlyClosed.remove(entry)
        newTab(url = entry.url)
    }

    private fun recordClosed(tab: BrowserTab) {
        if (tab.isIncognito || tab.url == NEW_TAB_URL) return
        recentlyClosed.add(0, PersistedTab(tab.url, tab.title, false))
        while (recentlyClosed.size > 10) recentlyClosed.removeAt(recentlyClosed.size - 1)
    }

    fun createGroup(members: List<BrowserTab>, name: String, colorIndex: Int) {
        if (members.isEmpty()) return
        val id = "group_${System.currentTimeMillis()}"
        members.forEach {
            it.groupId = id
            it.groupName = name
            it.groupColorIndex = colorIndex
        }
        persistTabs()
    }

    fun addToGroup(tab: BrowserTab, group: TabGroup) {
        tab.groupId = group.id
        tab.groupName = group.name
        tab.groupColorIndex = group.colorIndex
        persistTabs()
    }

    fun removeFromGroup(tab: BrowserTab) {
        tab.groupId = null
        tab.groupName = null
        persistTabs()
    }

    /** Call after a tab's url/title change (e.g. on page load) so the restore point stays current. */
    fun persistTabs() {
        database.saveOpenTabs(
            tabs.filterNot { it.isIncognito }.map {
                PersistedTab(it.url, it.title, it.id == activeTabId, it.groupId, it.groupName, it.groupColorIndex)
            },
        )
    }
}
