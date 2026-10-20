package com.example.pocketexpense.quick

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe coordinator that synchronizes Quick Expense triggers between
 * the foreground MainActivity and the background QuickExpenseAccessibilityService.
 *
 * Guarantees:
 * 1. Exactly-once trigger firing even if both listeners receive the same key event.
 * 2. Debouncing of rapid accidental triggers (600ms cooldown).
 * 3. Accurate tracking of whether PocketExpense is currently holding foreground focus.
 */
object QuickExpenseTriggerCoordinator {

    private val lastTriggerTimestamp = AtomicLong(0L)
    private val isForeground = AtomicBoolean(false)

    var timeProvider: () -> Long = {
        try {
            SystemClock.uptimeMillis()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }

    @Volatile
    private var foregroundTriggerListener: (() -> Unit)? = null

    var isForegroundActive: Boolean
        get() = isForeground.get()
        set(value) {
            isForeground.set(value)
        }

    fun setForegroundListener(listener: (() -> Unit)?) {
        foregroundTriggerListener = listener
    }

    /**
     * Attempts to acquire the trigger lock. Returns true if and only if
     * no trigger has occurred within the cooldown window (600ms).
     */
    fun tryTrigger(): Boolean {
        val now = timeProvider()
        val last = lastTriggerTimestamp.get()
        if (now - last < 600L) {
            return false // Debounced: trigger already executed recently
        }
        return lastTriggerTimestamp.compareAndSet(last, now)
    }

    /**
     * Resets cooldown timer. Useful for test isolation.
     */
    fun resetCooldown() {
        lastTriggerTimestamp.set(0L)
    }

    /**
     * Signals the foreground Activity to display the in-app Quick Expense dialog.
     */
    fun notifyForegroundTrigger() {
        foregroundTriggerListener?.invoke()
    }
}
