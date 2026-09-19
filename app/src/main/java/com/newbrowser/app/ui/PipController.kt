package com.newbrowser.app.ui

/**
 * Tracks whether a fullscreen HTML5 video is currently showing, so [MainActivity] knows
 * whether to enter picture-in-picture when the user leaves the app (e.g. taps Home).
 * A plain object rather than prop-drilling: BrowserScreen's WebChromeClient callbacks and
 * the Activity's lifecycle callback are otherwise unrelated call sites.
 */
object PipController {
    var isFullscreenVideoActive: Boolean = false
}
