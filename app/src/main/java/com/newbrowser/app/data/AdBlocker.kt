package com.newbrowser.app.data

import android.content.Context

object AdBlocker {
    private lateinit var appContext: Context

    private val blockedDomains: Set<String> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadDomains()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun loadDomains(): Set<String> {
        return try {
            appContext.assets.open("adblock_domains.txt").bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.toHashSet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun isBlockedHost(host: String?): Boolean {
        if (host.isNullOrEmpty()) return false
        val lower = host.lowercase()
        if (lower in blockedDomains) return true
        var idx = lower.indexOf('.')
        while (idx != -1) {
            val suffix = lower.substring(idx + 1)
            if (suffix in blockedDomains) return true
            idx = lower.indexOf('.', idx + 1)
        }
        return false
    }
}
