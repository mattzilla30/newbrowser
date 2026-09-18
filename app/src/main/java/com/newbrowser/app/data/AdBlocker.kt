package com.newbrowser.app.data

object AdBlocker {
    private val BLOCKED_DOMAINS: Set<String> = setOf(
        // Ad networks / exchanges
        "doubleclick.net",
        "2mdn.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "amazon-adsystem.com",
        "adnxs.com",
        "adsrvr.org",
        "moatads.com",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "casalemedia.com",
        "bidswitch.net",
        "media.net",
        "adroll.com",
        "adform.net",
        "adsafeprotected.com",
        "serving-sys.com",
        "mathtag.com",
        "bluekai.com",
        "exelator.com",
        "yieldmo.com",
        "sharethrough.com",
        "33across.com",
        "smartadserver.com",
        "flashtalking.com",
        "innovid.com",
        "spotxchange.com",
        "contextweb.com",
        "sovrn.com",
        "gumgum.com",
        "advertising.com",
        "yieldlab.net",
        "adition.com",
        "smaato.com",
        "inmobi.com",
        "chartboost.com",
        "vungle.com",
        "applovin.com",

        // Analytics / trackers
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "scorecardresearch.com",
        "quantserve.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "hotjar.com",
        "mouseflow.com",
        "crazyegg.com",
        "fullstory.com",
        "amplitude.com",
        "connect.facebook.net",
        "facebook.net",
    )

    fun isBlockedHost(host: String?): Boolean {
        if (host.isNullOrEmpty()) return false
        val lower = host.lowercase()
        return BLOCKED_DOMAINS.any { domain -> lower == domain || lower.endsWith(".$domain") }
    }
}
