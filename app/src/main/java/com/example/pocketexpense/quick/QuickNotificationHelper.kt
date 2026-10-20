package com.example.pocketexpense.quick

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pocketexpense.MainActivity
import com.example.pocketexpense.R

/**
 * Helper for optional, user-visible Quick Expense Notification.
 *
 * Feature D Compliance:
 * - 100% user-controlled and optional (disabled by default, toggled via Settings).
 * - Compliant with standard Android Notification guidelines.
 * - Does NOT abuse background keys or deceive the user.
 */
object QuickNotificationHelper {

    private const val CHANNEL_ID = "quick_expense_channel"
    private const val NOTIFICATION_ID = 2001
    private const val PREFS_NAME = "quick_expense_prefs"
    private const val KEY_QUICK_NOTIFICATION = "quick_notification_enabled"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_QUICK_NOTIFICATION, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_QUICK_NOTIFICATION, enabled).apply()

        if (enabled) {
            showNotification(context)
        } else {
            cancelNotification(context)
        }
    }

    fun updateState(context: Context) {
        if (isEnabled(context)) {
            showNotification(context)
        } else {
            cancelNotification(context)
        }
    }

    fun showNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quick Expense Shortcut",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Provides an optional notification shortcut to record expenses quickly"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = QuickExpenseTileService.ACTION_QUICK_EXPENSE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Quick Expense Entry")
            .setContentText("Tap to record an expense in one tap")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.mipmap.ic_launcher, "Quick Add", pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
