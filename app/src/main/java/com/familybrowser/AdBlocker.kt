package com.familybrowser

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.ByteArrayInputStream

/**
 * AdBlocker.kt
 * Handles ad blocking, tracker blocking, and adult content filtering.
 * All blocking is done locally — no external calls, no data leaks.
 *
 * Features:
 * - Ad network domain blocking
 * - Tracker blocking with per-session count
 * - Adult content filtering with PIN protection
 * - Kids mode whitelist enforcement
 * - Custom "Site Blocked" page responses
 */

class AdBlocker(private val context: Context) {

    companion object {
        // ─── Ad Network Domains ───────────────────────────────────────────────
        private val AD_DOMAINS = setOf(
            "doubleclick.net", "googlesyndication.com", "adservice.google.com",
            "googleadservices.com", "youtube.com/pagead", "ads.youtube.com",
            "amazon-adsystem.com", "facebook.com/tr", "scorecardresearch.com",
            "quantserve.com", "taboola.com", "outbrain.com", "moatads.com",
            "adsrvr.org", "advertising.com", "ads.twitter.com",
            "static.ads-twitter.com", "adsystem.amazon.com", "adnxs.com",
            "rubiconproject.com", "pubmatic.com", "openx.net", "criteo.com",
            "yieldmo.com", "appnexus.com", "media.net", "revcontent.com",
            "zergnet.com", "mgid.com", "adblade.com", "bing.com/aclk",
            "cdn.taboola.com", "tpc.googlesyndication.com", "pagead2.googlesyndication.com",
            "stats.g.doubleclick.net", "cm.g.doubleclick.net", "securepubads.g.doubleclick.net",
            "ads.pubmatic.com", "simage2.pubmatic.com", "image2.pubmatic.com",
            "banner.siteimprove.com", "pixel.moatads.com", "ad.doubleclick.net",
            "ib.adnxs.com", "secure.adnxs.com", "acdn.adnxs.com",
            "pixel.rubiconproject.com", "pixel.mathtag.com", "mathtag.com",
            "bidswitch.net", "adsafeprotected.com", "eyeota.net",
            "lijit.com", "sovrn.com", "indexexchange.com",
            "casalemedia.com", "semasio.net", "exelate.com", "bluekai.com",
            "demdex.net", "turn.com", "agkn.com", "segment.io",
            "hotjar.com", "mouseflow.com", "fullstory.com", "logrocket.com"
        )

        // ─── Tracker Domains ──────────────────────────────────────────────────
        private val TRACKER_DOMAINS = setOf(
            "google-analytics.com", "googletagmanager.com", "googletagservices.com",
            "analytics.google.com", "ssl.google-analytics.com", "www.google-analytics.com",
            "stats.wp.com", "pixel.wp.com", "bat.bing.com", "analytics.twitter.com",
            "t.co", "connect.facebook.net", "graph.facebook.com",
            "analytics.yahoo.com", "beacon.yahoo.com", "clicks.beap.bc.yahoo.com",
            "piwik.org", "matomo.org", "statcounter.com", "clicktale.net",
            "clicktale.com", "crazyegg.com", "trackjs.com", "raygun.io",
            "bugsnag.com", "newrelic.com", "nr-data.net", "amplitude.com",
            "api.amplitude.com", "cdn.amplitude.com", "mixpanel.com",
            "cdn4.mxpnl.com", "segment.com", "cdn.segment.com",
            "api.segment.io", "cdn.heapanalytics.com", "heapanalytics.com",
            "rollbar.com", "sentry.io", "ingest.sentry.io", "browser.sentry-cdn.com",
            "intercom.io", "widget.intercom.io", "nexus.ensighten.com"
        )

        // ─── Adult Content Domains ────────────────────────────────────────────
        private val ADULT_DOMAINS = setOf(
            "pornhub.com", "xvideos.com", "xnxx.com", "onlyfans.com",
            "brazzers.com", "redtube.com", "youporn.com", "spankbang.com",
            "xhamster.com", "livejasmin.com", "chaturbate.com", "stripchat.com",
            "camsoda.com", "cam4.com", "bongacams.com", "myfreecams.com",
            "streamate.com", "ifsa.tv", "hclips.com", "hdzog.com",
            "tnaflix.com", "tube8.com", "extremetube.com", "keezmovies.com",
            "slutload.com", "beeg.com", "drtuber.com", "hardsextube.com",
            "fuq.com", "vjav.com", "porntrex.com", "empflix.com",
            "4tube.com", "porntube.com", "ah-me.com", "txxx.com",
            "xbabe.com", "xcafe.com", "wetplace.com", "sunporno.com",
            "fapster.com", "ok.xxx", "xxxbunker.com", "desixnxx.net",
            "desitvforum.net", "indianpornvideos.com"
        )

        // ─── Blocked Page HTML Template ───────────────────────────────────────
        fun buildBlockedPage(url: String, reason: BlockReason): String {
            val (icon, title, subtitle, color) = when (reason) {
                BlockReason.ADULT -> Quadruple("🔒", "Site Blocked", "This site contains adult content and has been blocked for safe browsing.", "#E53E3E")
                BlockReason.AD -> Quadruple("🛡️", "Ad Blocked", "An advertisement or tracker was blocked.", "#38A169")
                BlockReason.TRACKER -> Quadruple("👁️", "Tracker Blocked", "A tracking script was prevented from loading.", "#3182CE")
                BlockReason.KIDS_MODE -> Quadruple("👶", "Not Allowed", "This site is not on the approved list for Kids Mode.", "#805AD5")
            }
            return """
                <!DOCTYPE html><html><head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                  * { margin:0; padding:0; box-sizing:border-box; }
                  body { font-family: -apple-system, sans-serif; background: #F7FAFC;
                         display:flex; align-items:center; justify-content:center;
                         min-height:100vh; padding:24px; }
                  .card { background:white; border-radius:20px; padding:40px 32px;
                          text-align:center; max-width:400px; width:100%;
                          box-shadow: 0 4px 24px rgba(0,0,0,0.08); }
                  .icon { font-size:56px; margin-bottom:20px; }
                  h1 { font-size:24px; font-weight:700; color:#1A202C; margin-bottom:12px; }
                  p { color:#718096; font-size:15px; line-height:1.6; margin-bottom:24px; }
                  .url { background:#F7FAFC; border-radius:8px; padding:10px 14px;
                         font-size:12px; color:#A0AEC0; word-break:break-all; margin-bottom:24px; }
                  .badge { display:inline-block; background:$color; color:white;
                           border-radius:20px; padding:6px 16px; font-size:13px; font-weight:600; }
                  .back-btn { display:block; margin-top:20px; padding:14px;
                              background:#EDF2F7; border-radius:12px; color:#4A5568;
                              font-size:15px; font-weight:600; text-decoration:none; }
                </style></head><body>
                <div class="card">
                  <div class="icon">$icon</div>
                  <h1>$title</h1>
                  <p>$subtitle</p>
                  <div class="url">$url</div>
                  <span class="badge">Family Browser Protection</span>
                  <a href="javascript:history.back()" class="back-btn">← Go Back</a>
                </div></body></html>
            """.trimIndent()
        }
    }

    // ─── State ────────────────────────────────────────────────────────────────
    var isAdBlockEnabled: Boolean = true
    var isTrackerBlockEnabled: Boolean = true
    var isAdultBlockEnabled: Boolean = true
    var adultBlockPin: String = ""
    var trackerBlockCount: Int = 0
        private set
    var adBlockCount: Int = 0
        private set

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                context, "adblocker_prefs", masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("adblocker_prefs", Context.MODE_PRIVATE)
        }
    }

    init { loadSettings() }

    // ─── Main Intercept ───────────────────────────────────────────────────────
    /**
     * Called from WebViewClient.shouldInterceptRequest.
     * Returns a blocked page response, an empty response for ads/trackers,
     * or null to allow the request through.
     */
    fun shouldBlock(
        request: WebResourceRequest,
        isKidsMode: Boolean = false,
        kidsWhitelist: Set<String> = emptySet()
    ): WebResourceResponse? {
        val url = request.url?.toString() ?: return null
        val host = request.url?.host?.lowercase() ?: return null
        val isMainFrame = request.isForMainFrame

        // Kids mode: only allow whitelisted domains
        if (isKidsMode && isMainFrame) {
            val allowed = kidsWhitelist.any { host.endsWith(it) || host == it }
            if (!allowed) {
                return blockedPageResponse(url, BlockReason.KIDS_MODE)
            }
        }

        // Adult content block
        if (isAdultBlockEnabled && isMainFrame) {
            val blocked = ADULT_DOMAINS.any { host.contains(it) }
            if (blocked) return blockedPageResponse(url, BlockReason.ADULT)
        }

        // Ad block
        if (isAdBlockEnabled) {
            val blocked = AD_DOMAINS.any { url.contains(it) }
            if (blocked) {
                adBlockCount++
                return emptyResponse()
            }
        }

        // Tracker block
        if (isTrackerBlockEnabled) {
            val blocked = TRACKER_DOMAINS.any { host.contains(it) }
            if (blocked) {
                trackerBlockCount++
                return emptyResponse()
            }
        }

        return null // Allow request
    }

    // ─── PIN Management ───────────────────────────────────────────────────────
    fun setAdultBlockPin(pin: String) {
        adultBlockPin = pin
        prefs.edit().putString("adult_pin", pin).apply()
    }

    fun verifyPin(pin: String): Boolean = pin == adultBlockPin

    fun disableAdultBlockWithPin(pin: String): Boolean {
        return if (verifyPin(pin)) {
            isAdultBlockEnabled = false
            saveSettings()
            true
        } else false
    }

    // ─── Persistence ──────────────────────────────────────────────────────────
    fun saveSettings() {
        prefs.edit()
            .putBoolean("ad_block", isAdBlockEnabled)
            .putBoolean("tracker_block", isTrackerBlockEnabled)
            .putBoolean("adult_block", isAdultBlockEnabled)
            .putString("adult_pin", adultBlockPin)
            .apply()
    }

    private fun loadSettings() {
        isAdBlockEnabled = prefs.getBoolean("ad_block", true)
        isTrackerBlockEnabled = prefs.getBoolean("tracker_block", true)
        isAdultBlockEnabled = prefs.getBoolean("adult_block", true)
        adultBlockPin = prefs.getString("adult_pin", "") ?: ""
    }

    fun resetCounts() { adBlockCount = 0; trackerBlockCount = 0 }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private fun blockedPageResponse(url: String, reason: BlockReason): WebResourceResponse {
        val html = buildBlockedPage(url, reason)
        return WebResourceResponse(
            "text/html", "UTF-8", 200, "OK",
            mapOf("Content-Type" to "text/html"),
            ByteArrayInputStream(html.toByteArray())
        )
    }

    private fun emptyResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain", "UTF-8", 200, "OK",
        emptyMap(),
        ByteArrayInputStream(ByteArray(0))
    )
}

enum class BlockReason { ADULT, AD, TRACKER, KIDS_MODE }

// Simple data class replacement for destructuring
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
