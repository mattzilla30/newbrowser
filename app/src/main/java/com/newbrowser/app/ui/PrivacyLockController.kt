package com.newbrowser.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Tracks whether private tabs should be locked behind biometric/PIN auth, and whether the
 * app is currently showing that lock screen. A plain object rather than prop-drilling:
 * [MainActivity]'s onStop() and [App.kt]'s composition are otherwise unrelated call sites,
 * mirroring the [PipController] pattern.
 */
object PrivacyLockController {
    /** Mirrors the "Lock private tabs" setting so onStop() can decide whether to lock. */
    var lockEnabled: Boolean = false

    /** Mirrors whether any incognito tab is currently open; locking is a no-op without one. */
    var hasIncognitoTabs: Boolean = false

    var isLocked by mutableStateOf(false)

    fun onAppBackgrounded() {
        if (lockEnabled && hasIncognitoTabs) {
            isLocked = true
        }
    }

    fun unlock() {
        isLocked = false
    }
}
