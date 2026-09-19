package com.newbrowser.app.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

private enum class FilterFormat { ADBLOCK, HOSTS }

private data class FilterSource(val url: String, val format: FilterFormat)

private val FILTER_SOURCES = listOf(
    FilterSource("https://easylist.to/easylist/easylist.txt", FilterFormat.ADBLOCK),
    FilterSource("https://easylist.to/easylist/easyprivacy.txt", FilterFormat.ADBLOCK),
    FilterSource(
        "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/BaseFilter/sections/adservers.txt",
        FilterFormat.ADBLOCK,
    ),
    FilterSource(
        "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/BaseFilter/sections/general_url.txt",
        FilterFormat.ADBLOCK,
    ),
    FilterSource(
        "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/MobileFilter/sections/adservers.txt",
        FilterFormat.ADBLOCK,
    ),
    FilterSource(
        "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/MobileFilter/sections/general_url.txt",
        FilterFormat.ADBLOCK,
    ),
    // Ads/trackers only cover part of what ad-block test pages check. This adds malware,
    // cryptomining, gambling, adult, fake-news, and social-tracker domains, in hosts-file
    // format ("0.0.0.0 domain.tld") rather than Adblock Plus syntax.
    FilterSource(
        "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews-gambling-porn-social/hosts",
        FilterFormat.HOSTS,
    ),
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

private val HOSTS_RULE = Regex("""^(?:0\.0\.0\.0|127\.0\.0\.1)\s+(\S+)""")
private val LOCAL_HOST_NAMES = setOf(
    "localhost", "localhost.localdomain", "local", "broadcasthost", "0.0.0.0",
    "ip6-localhost", "ip6-loopback", "ip6-localnet", "ip6-mcastprefix",
    "ip6-allnodes", "ip6-allrouters", "ip6-allhosts",
)

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
            for (source in FILTER_SOURCES) {
                try {
                    fetchRules(source, blocked, allowed)
                } catch (e: Exception) {
                    failureCount++
                }
            }
            if (failureCount > 0) {
                return UpdateResult.Failure(
                    "$failureCount of ${FILTER_SOURCES.size} filter sources failed to download. " +
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

    private fun fetchRules(source: FilterSource, blocked: MutableSet<String>, allowed: MutableSet<String>) {
        val connection = URL(source.url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        try {
            connection.inputStream.bufferedReader().useLines { lines ->
                for (rawLine in lines) {
                    val line = rawLine.trim().lowercase()
                    if (line.isEmpty()) continue
                    when (source.format) {
                        FilterFormat.ADBLOCK -> parseAdblockLine(line, blocked, allowed)
                        FilterFormat.HOSTS -> parseHostsLine(line, blocked)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAdblockLine(line: String, blocked: MutableSet<String>, allowed: MutableSet<String>) {
        if (line.startsWith("!") || line.startsWith("[") || line.contains("*")) return
        val allowMatch = ALLOW_RULE.find(line)
        if (allowMatch != null) {
            allowed.add(allowMatch.groupValues[1])
            return
        }
        val blockMatch = BLOCK_RULE.find(line)
        if (blockMatch != null) {
            blocked.add(blockMatch.groupValues[1])
        }
    }

    private fun parseHostsLine(line: String, blocked: MutableSet<String>) {
        if (line.startsWith("#")) return
        val domain = HOSTS_RULE.find(line)?.groupValues?.get(1) ?: return
        if ('.' in domain && domain !in LOCAL_HOST_NAMES) {
            blocked.add(domain)
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
