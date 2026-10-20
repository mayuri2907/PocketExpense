package com.example.pocketexpense.quick

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.pocketexpense.data.repository.ExpenseRepository

/**
 * Singleton manager providing a clean, thread-safe entry point to display
 * and dismiss the system-wide Quick Expense floating overlay window.
 */
object QuickExpenseOverlayManager {

    private var overlayWindow: QuickExpenseOverlayWindow? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun show(context: Context, repository: ExpenseRepository) {
        mainHandler.post {
            if (overlayWindow == null) {
                overlayWindow = QuickExpenseOverlayWindow(context.applicationContext)
            }
            overlayWindow?.show(repository)
        }
    }

    fun dismiss() {
        mainHandler.post {
            overlayWindow?.dismiss()
        }
    }

    fun isShowing(): Boolean = overlayWindow?.isShowing() == true
}
