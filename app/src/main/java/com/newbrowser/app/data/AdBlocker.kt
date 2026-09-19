package com.newbrowser.app.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

private val FILTER_LIST_URLS = listOf(
    "https://easylist.to/easylist/easylist.txt",
    "https://easylist.to/easylist/easyprivacy.txt",
    "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/BaseFilter/sections/adservers.txt",
    "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/BaseFilter/sections/general_url.txt",
    "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/MobileFilter/sections/adservers.txt",
    "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/MobileFilter/sections/general_url.txt",
)

/**
 * Well-known ad/tracker domains that the filter lists above only cover with path- or
 * option-qualified rules (which this parser doesn't attempt to match), not a bare `||domain^`
 * rule. Always unioned into the parsed result so a filter-list quirk upstream can't silently
 * drop a domain this app used to block.
 */
private val SUPPLEMENTAL_DOMAINS = setOf(
    "doubleclick.net", "2mdn.net", "googlesyndication.com", "googleadservices.com",
    "adservice.google.com", "amazon-adsystem.com", "adnxs.com", "adsrvr.org", "moatads.com",
    "criteo.com", "criteo.net", "taboola.com", "outbrain.com", "pubmatic.com", "rubiconproject.com",
    "openx.net", "casalemedia.com", "bidswitch.net", "media.net", "adroll.com", "adform.net",
    "adsafeprotected.com", "serving-sys.com", "mathtag.com", "bluekai.com", "exelator.com",
    "yieldmo.com", "sharethrough.com", "33across.com", "smartadserver.com", "flashtalking.com",
    "innovid.com", "spotxchange.com", "contextweb.com", "sovrn.com", "gumgum.com", "advertising.com",
    "yieldlab.net", "adition.com", "smaato.com", "inmobi.com", "chartboost.com", "vungle.com",
    "applovin.com", "google-analytics.com", "googletagmanager.com", "googletagservices.com",
    "scorecardresearch.com", "quantserve.com", "mixpanel.com", "segment.io", "segment.com",
    "hotjar.com", "mouseflow.com", "crazyegg.com", "fullstory.com", "amplitude.com",
    "connect.facebook.net", "facebook.net",
)

private val BLOCK_RULE = Regex("""^\|\|([a-z0-9][a-z0-9.*_-]*?)\^(?:$|\$)""")

/**
 * Unlike [BLOCK_RULE], this requires the line to end right after `^`: an exception with
 * options after it (`@@||domain^$domain=example.com`) is scoped to specific sites and would be
 * wrong to treat as "never block this domain anywhere", which is what a match here means.
 */
private val ALLOW_RULE = Regex("""^@@\|\|([a-z0-9][a-z0-9.*_-]*?)\^$""")

object AdBlocker {
    private lateinit var appContext: Context
    private val domainsRef = AtomicReference<Set<String>?>(null)
    private val mainHandler = Handler(Looper.getMainLooper())

    sealed class UpdateResult {
        data class Success(val domainCount: Int) : UpdateResult()
        data class Failure(val message: String) : UpdateResult()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        Thread { blockedDomains() }.start()
    }

    fun isBlockedHost(host: String?): Boolean {
        if (host.isNullOrEmpty()) return false
        val domains = blockedDomains()
        val lower = host.lowercase()
        if (lower in domains) return true
        var idx = lower.indexOf('.')
        while (idx != -1) {
            val suffix = lower.substring(idx + 1)
            if (suffix in domains) return true
            idx = lower.indexOf('.', idx + 1)
        }
        return false
    }

    fun currentDomainCount(): Int = blockedDomains().size

    fun lastUpdatedMillis(): Long = updatedListFile().let { if (it.exists()) it.lastModified() else 0L }

    /** Downloads fresh filter lists on a background thread and posts [onResult] on the main thread. */
    fun updateFilterListsAsync(onResult: (UpdateResult) -> Unit) {
        Thread {
            val result = updateFilterLists()
            mainHandler.post { onResult(result) }
        }.start()
    }

    private fun updateFilterLists(): UpdateResult {
        return try {
            val blocked = HashSet<String>()
            val allowed = HashSet<String>()
            var failureCount = 0
            for (urlString in FILTER_LIST_URLS) {
                try {
                    fetchRules(urlString, blocked, allowed)
                } catch (e: Exception) {
                    failureCount++
                }
            }
            if (failureCount > 0) {
                return UpdateResult.Failure(
                    "$failureCount of ${FILTER_LIST_URLS.size} filter sources failed to download. " +
                        "Nothing was changed; check your connection and try again.",
                )
            }
            blocked.addAll(SUPPLEMENTAL_DOMAINS)
            blocked.removeAll(allowed)
            val pruned = pruneRedundantSubdomains(blocked)

            val tempFile = File(appContext.filesDir, "adblock_domains_updated.tmp")
            tempFile.bufferedWriter().use { writer ->
                pruned.sorted().forEach { domain ->
                    writer.write(domain)
                    writer.newLine()
                }
            }
            tempFile.renameTo(updatedListFile())

            domainsRef.set(pruned)
            UpdateResult.Success(pruned.size)
        } catch (e: Exception) {
            UpdateResult.Failure(e.message ?: "Update failed")
        }
    }

    private fun fetchRules(urlString: String, blocked: MutableSet<String>, allowed: MutableSet<String>) {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        try {
            connection.inputStream.bufferedReader().useLines { lines ->
                for (rawLine in lines) {
                    val line = rawLine.trim()
                    if (line.isEmpty() || line.startsWith("!") || line.startsWith("[") || line.contains("*")) {
                        continue
                    }
                    val allowMatch = ALLOW_RULE.find(line)
                    if (allowMatch != null) {
                        allowed.add(allowMatch.groupValues[1].lowercase())
                        continue
                    }
                    val blockMatch = BLOCK_RULE.find(line)
                    if (blockMatch != null) {
                        blocked.add(blockMatch.groupValues[1].lowercase())
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun pruneRedundantSubdomains(domains: Set<String>): Set<String> {
        val result = HashSet<String>(domains.size)
        for (domain in domains) {
            var isRedundant = false
            var idx = domain.indexOf('.')
            while (idx != -1) {
                if (domains.contains(domain.substring(idx + 1))) {
                    isRedundant = true
                    break
                }
                idx = domain.indexOf('.', idx + 1)
            }
            if (!isRedundant) result.add(domain)
        }
        return result
    }

    private fun updatedListFile(): File = File(appContext.filesDir, "adblock_domains_updated.txt")

    private fun blockedDomains(): Set<String> {
        domainsRef.get()?.let { return it }
        synchronized(this) {
            domainsRef.get()?.let { return it }
            val loaded = loadDomains()
            domainsRef.set(loaded)
            return loaded
        }
    }

    private fun loadDomains(): Set<String> {
        val updated = updatedListFile()
        if (updated.exists()) {
            try {
                return updated.bufferedReader().useLines { lines ->
                    lines.filter { it.isNotBlank() }.toHashSet()
                }
            } catch (e: Exception) {
                // Fall through to the bundled asset.
            }
        }
        return try {
            appContext.assets.open("adblock_domains.txt").bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.toHashSet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }
}
