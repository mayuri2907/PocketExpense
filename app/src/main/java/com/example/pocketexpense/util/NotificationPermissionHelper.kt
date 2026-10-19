package com.example.pocketexpense.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.pocketexpense.service.ExpenseNotificationListenerService

object NotificationPermissionHelper {

    /**
     * Checks if the user has granted NotificationListenerService permission to this app.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        // Preferred modern check using NotificationManagerCompat
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        if (enabledPackages.contains(context.packageName)) {
            return true
        }

        // Secondary fallback checking Secure Settings string
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val componentName = ComponentName(context, ExpenseNotificationListenerService::class.java).flattenToString()
        return flat.contains(componentName) || flat.contains(context.packageName)
    }

    /**
     * Creates an intent to navigate the user directly to Android's Notification Listener Settings screen.
     */
    fun createSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
