package com.newbrowser.app.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.newbrowser.app.MainActivity
import com.newbrowser.app.R
import com.newbrowser.app.ui.tabs.BrowserTab

private const val CHANNEL_ID = "web_heads"

/**
 * Posts a "Web Head" for a background tab: a floating bubble on Android 11+ (via the
 * documented non-conversation [NotificationCompat.BubbleMetadata] path), or a plain
 * tap-to-return notification everywhere else - tapping either one brings the app forward
 * with that tab selected, without needing the old draw-over-other-apps overlay permission
 * that floating-bubble browsers used before Android's own Bubbles API existed.
 */
object WebHeadNotifier {
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Web Heads",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Bubbles for links sent to a Web Head while you keep browsing"
        }
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun postWebHead(context: Context, tab: BrowserTab) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)

        val requestCode = tab.url.hashCode()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(tab.url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(tab.title.ifBlank { tab.url })
            .setContentText(tab.url)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        // Bubbles without a conversation shortcut only work from Android 11 onward; older
        // versions fall back to the plain notification built above, which still returns the
        // user to the right tab, just without the floating circle.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(pendingIntent, icon)
                .setDesiredHeight(600)
                .setAutoExpandBubble(false)
                .setSuppressNotification(false)
                .build()
            builder.setBubbleMetadata(bubbleMetadata)
        }

        NotificationManagerCompat.from(context).notify(requestCode, builder.build())
    }

    /** Whether the system's per-app "Allow bubbles" setting can be opened on this device. */
    fun canOpenBubbleSettings(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun openBubbleSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ContextCompat.startActivity(context, intent, null)
        } catch (e: Exception) {
            // No such settings screen on this OEM's build; nothing to recover into.
        }
    }
}
