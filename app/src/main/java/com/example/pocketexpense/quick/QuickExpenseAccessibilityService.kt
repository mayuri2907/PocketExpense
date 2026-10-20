package com.example.pocketexpense.quick

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.pocketexpense.MainActivity
import com.example.pocketexpense.PocketExpenseApplication
import com.example.pocketexpense.floating.OverlayPermissionHelper

/**
 * Opt-in system accessibility service dedicated solely to filtering hardware key events
 * for the global double-Volume-Up Quick Expense trigger.
 *
 * Privacy & Security guarantees:
 * 1. Strictly declares canRetrieveWindowContent="false". It cannot read, observe,
 *    or access screen content, window text, passwords, or user interactions of other apps.
 * 2. accessibilityEventTypes is empty (""). No UI events are captured or dispatched.
 * 3. Never consumes unrelated key events. Volume Up increases volume normally on single press
 *    and on key-repeat/long-press.
 * 4. Checks KeyguardManager to ensure secure lockscreens and device authentication are NEVER bypassed.
 * 5. Uses monotonic clock (SystemClock.uptimeMillis) to detect double-press within 100-450ms.
 */
class QuickExpenseAccessibilityService : AccessibilityService() {

    private var lastVolumeUpTimestamp = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isRunning = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty. No window contents or UI events are observed or collected.
    }

    override fun onInterrupt() {
        // Required callback when accessibility framework interrupts service
    }

    private fun getMonotonicTime(): Long {
        return try {
            SystemClock.uptimeMillis()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.action != KeyEvent.ACTION_DOWN) {
                return false
            }
            if (event.repeatCount > 0) {
                // Ignore long-press auto-repeats to prevent false triggers
                return false
            }

            val now = getMonotonicTime()
            val delta = now - lastVolumeUpTimestamp

            if (delta in 100L..450L) {
                // Double-press detected! Reset timestamp immediately
                lastVolumeUpTimestamp = 0L

                // 1. Respect device lock state: Never display overlay or bypass secure lockscreen
                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                if (km?.isKeyguardLocked == true) {
                    return false
                }

                // 2. Debounce and coordinate with foreground Activity listener
                if (!QuickExpenseTriggerCoordinator.tryTrigger()) {
                    return false
                }

                // 3. Dispatch trigger appropriately
                if (QuickExpenseTriggerCoordinator.isForegroundActive) {
                    // PocketExpense is in the foreground: notify Activity
                    QuickExpenseTriggerCoordinator.notifyForegroundTrigger()
                } else {
                    // PocketExpense is in the background:
                    if (OverlayPermissionHelper.canDrawOverlays(this)) {
                        val app = applicationContext as? PocketExpenseApplication
                        if (app != null) {
                            QuickExpenseOverlayManager.show(this, app.repository)
                        }
                    } else {
                        // Fallback: Launch MainActivity with Quick Expense intent
                        val intent = Intent(this, MainActivity::class.java).apply {
                            action = QuickExpenseTileService.ACTION_QUICK_EXPENSE
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                    }
                }

                // Consume the second Volume-Up press so it triggers the popup without double-incrementing volume
                return true
            } else {
                lastVolumeUpTimestamp = now
                // Pass single press to system volume controls
                return false
            }
        }

        // Preserve all other keys untouched
        return false
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        /**
         * Checks whether QuickExpenseAccessibilityService is currently enabled in Android Settings.
         */
        fun isServiceEnabled(context: Context): Boolean {
            val expectedComponent = ComponentName(context, QuickExpenseAccessibilityService::class.java)
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledComponent = ComponentName.unflattenFromString(componentNameString)
                if (enabledComponent != null && enabledComponent == expectedComponent) {
                    return true
                }
            }
            return false
        }

        /**
         * Returns an Intent navigating directly to Android Accessibility Settings.
         */
        fun createSettingsIntent(): Intent {
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}
