package com.example.pocketexpense.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.pocketexpense.PocketExpenseApplication
import com.example.pocketexpense.data.repository.ProcessNotificationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import com.example.pocketexpense.BuildConfig
import android.content.ComponentName
import android.content.Context
import android.os.Build

class ExpenseNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ExpenseNotifService"

        fun tryRebind(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val component = ComponentName(context, ExpenseNotificationListenerService::class.java)
                    requestRebind(component)
                } catch (_: Exception) {
                    // Safe no-op if already bound or permission not granted
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // 1. Never inspect our own notifications
        if (packageName == this.packageName) return

        val app = application as? PocketExpenseApplication ?: return
        val repository = app.repository

        // 2. Reject immediately if package is not a supported UPI app
        if (!repository.compositeParser.isPackageSupported(packageName)) {
            return
        }

        // 3. Extract notification text safely
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: ""

        if (title.isBlank() && text.isBlank()) return

        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        // 4. Process asynchronously in background coroutine
        serviceScope.launch {
            try {
                val result = repository.processIncomingNotification(
                    packageName = packageName,
                    title = title,
                    text = text,
                    timestamp = postTime
                )

                when (result) {
                    is ProcessNotificationResult.Created -> {
                        if (BuildConfig.DEBUG) {
                            Log.i(TAG, "Expense created successfully: ${result.expense.title} - ₹${result.expense.amountInPaise / 100.0}")
                        }
                        // Trigger the 3-second floating categorization popup (non-blocking)
                        app.floatingExpenseManager.enqueueAndShow(
                            expense = result.expense,
                            appName = result.appName,
                            suggestedCategoryId = result.suggestedCategoryId,
                            isConfidentCategory = result.isConfidentCategory
                        )
                    }
                    is ProcessNotificationResult.Duplicate -> {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Duplicate notification ignored: ${result.reason}")
                        }
                    }
                    is ProcessNotificationResult.Ignored -> {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Notification ignored: ${result.reason}")
                        }
                    }
                    is ProcessNotificationResult.Ambiguous -> {
                        if (BuildConfig.DEBUG) {
                            Log.w(TAG, "Notification ambiguous: ${result.reason}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error processing payment notification", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
