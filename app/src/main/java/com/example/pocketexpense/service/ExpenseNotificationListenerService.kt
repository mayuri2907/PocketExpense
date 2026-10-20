package com.example.pocketexpense.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.pocketexpense.BuildConfig
import com.example.pocketexpense.PocketExpenseApplication
import com.example.pocketexpense.data.repository.ProcessNotificationResult
import com.example.pocketexpense.util.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ExpenseNotifService"

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        fun isListenerConnected(): Boolean = _isConnected.value

        fun tryRebind(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val component = ComponentName(context, ExpenseNotificationListenerService::class.java)
                    requestRebind(component)
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "requestRebind() invoked for $component")
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "requestRebind() failed: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        com.example.pocketexpense.util.NotificationDiagnosticTracker.logEvent(
            packageName = "android.system",
            appDisplayName = "Notification Listener",
            status = com.example.pocketexpense.util.DiagnosticStatus.PARSED_SUCCESS,
            summary = "Listener service connected to Android notification subsystem"
        )
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "NotificationListenerService CONNECTED successfully")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
        com.example.pocketexpense.util.NotificationDiagnosticTracker.logEvent(
            packageName = "android.system",
            appDisplayName = "Notification Listener",
            status = com.example.pocketexpense.util.DiagnosticStatus.SERVICE_DISCONNECTED,
            summary = "Listener disconnected by Android OS"
        )
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "NotificationListenerService DISCONNECTED")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Notification received from package: $packageName [Service connected: ${_isConnected.value}]")
        }

        // 1. Never inspect our own notifications
        if (packageName == this.packageName) return

        val app = application as? PocketExpenseApplication ?: return
        val repository = app.repository

        // 2. Reject immediately if package is not a supported UPI app
        if (!repository.compositeParser.isPackageSupported(packageName)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Package '$packageName' is not a supported payment application. Discarding.")
            }
            return
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Supported UPI package matched: '$packageName'. Extracting notification fields.")
        }

        // 3. Extract notification text safely across standard and expanded text fields
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: "").trim()

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
        val normalText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""

        // Preserve all distinct informational text without duplicating lines
        val text = when {
            bigText.isNotBlank() && normalText.isNotBlank() && !bigText.contains(normalText) -> "$normalText. $bigText"
            bigText.isNotBlank() -> bigText
            normalText.isNotBlank() -> normalText
            else -> subText
        }

        if (title.isBlank() && text.isBlank()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Empty title and body in notification from '$packageName'. Discarding.")
            }
            return
        }

        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        // 4. Process asynchronously in background coroutine
        serviceScope.launch {
            try {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Dispatching notification to parser pipeline for package: '$packageName'")
                }

                val result = repository.processIncomingNotification(
                    packageName = packageName,
                    title = title,
                    text = text,
                    timestamp = postTime
                )

                when (result) {
                    is ProcessNotificationResult.Created -> {
                        if (BuildConfig.DEBUG) {
                            Log.i(
                                TAG,
                                "Transaction CREATED in Room: id=${result.expense.id}, " +
                                "merchant='${result.expense.title}', amountPaise=${result.expense.amountInPaise}, " +
                                "category=${result.suggestedCategoryId}"
                            )
                        }
                        com.example.pocketexpense.util.NotificationDiagnosticTracker.logEvent(
                            packageName = packageName,
                            appDisplayName = result.appName,
                            status = com.example.pocketexpense.util.DiagnosticStatus.PARSED_SUCCESS,
                            summary = "Recorded ${CurrencyUtils.paiseToRupeesString(result.expense.amountInPaise)} to '${result.expense.title}'"
                        )
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
                            Log.d(TAG, "Notification DUPLICATE detected: ${result.reason}")
                        }
                        com.example.pocketexpense.util.NotificationDiagnosticTracker.logEvent(
                            packageName = packageName,
                            appDisplayName = packageName,
                            status = com.example.pocketexpense.util.DiagnosticStatus.DUPLICATE_IGNORED,
                            summary = result.reason
                        )
                    }
                    is ProcessNotificationResult.Ignored -> {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Notification IGNORED by parser: ${result.reason}")
                        }
                        com.example.pocketexpense.util.NotificationDiagnosticTracker.logEvent(
                            packageName = packageName,
                            appDisplayName = packageName,
                            status = com.example.pocketexpense.util.DiagnosticStatus.NON_EXPENSE_IGNORED,
                            summary = result.reason
                        )
                    }
                    is ProcessNotificationResult.Ambiguous -> {
                        if (BuildConfig.DEBUG) {
                            Log.w(TAG, "Notification AMBIGUOUS: ${result.reason}")
                        }
                        com.example.pocketexpense.util.NotificationDiagnosticTracker.logEvent(
                            packageName = packageName,
                            appDisplayName = packageName,
                            status = com.example.pocketexpense.util.DiagnosticStatus.AMBIGUOUS_UNPARSED,
                            summary = result.reason
                        )
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Database insert or processing error for package: '$packageName'", e)
                }
                com.example.pocketexpense.util.NotificationDiagnosticTracker.logEvent(
                    packageName = packageName,
                    appDisplayName = packageName,
                    status = com.example.pocketexpense.util.DiagnosticStatus.DATABASE_ERROR,
                    summary = "Database or processing error"
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isConnected.value = false
        serviceScope.cancel()
    }
}
