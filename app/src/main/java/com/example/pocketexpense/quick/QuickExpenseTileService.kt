package com.example.pocketexpense.quick

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.example.pocketexpense.MainActivity

/**
 * Android Quick Settings Tile for global, system-sanctioned Quick Expense triggering.
 *
 * Allows users to trigger Quick Expense entry from anywhere (notification shade,
 * over any open app, or lock screen if configured) without abusing background services.
 */
class QuickExpenseTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_QUICK_EXPENSE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        const val ACTION_QUICK_EXPENSE = "com.example.pocketexpense.action.QUICK_EXPENSE"
    }
}
