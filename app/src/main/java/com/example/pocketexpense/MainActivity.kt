package com.example.pocketexpense

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.pocketexpense.quick.QuickExpenseTileService
import com.example.pocketexpense.quick.QuickExpenseTriggerCoordinator
import com.example.pocketexpense.quick.QuickNotificationHelper
import com.example.pocketexpense.ui.screens.MainScreen
import com.example.pocketexpense.ui.theme.PocketExpenseTheme
import com.example.pocketexpense.ui.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        val app = application as PocketExpenseApplication
        ExpenseViewModel.provideFactory(app.repository)
    }

    private var showQuickExpenseTrigger by mutableStateOf(false)
    private var lastVolumeUpTimestamp = 0L
    private var currentThemeMode by mutableStateOf(com.example.pocketexpense.util.ThemeMode.SYSTEM)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        currentThemeMode = com.example.pocketexpense.util.ThemePreferences.getThemeMode(this)

        // Sync optional persistent notification state with user preference
        QuickNotificationHelper.updateState(this)

        handleIntent(intent)

        setContent {
            PocketExpenseTheme(themeMode = currentThemeMode) {
                MainScreen(
                    viewModel = viewModel,
                    isQuickExpenseTriggered = showQuickExpenseTrigger,
                    onQuickExpenseHandled = { showQuickExpenseTrigger = false },
                    currentThemeMode = currentThemeMode,
                    onThemeChanged = { mode ->
                        com.example.pocketexpense.util.ThemePreferences.setThemeMode(this@MainActivity, mode)
                        currentThemeMode = mode
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        QuickExpenseTriggerCoordinator.isForegroundActive = true
        QuickExpenseTriggerCoordinator.setForegroundListener {
            runOnUiThread {
                showQuickExpenseTrigger = true
            }
        }
    }

    override fun onPause() {
        super.onPause()
        QuickExpenseTriggerCoordinator.isForegroundActive = false
        QuickExpenseTriggerCoordinator.setForegroundListener(null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == QuickExpenseTileService.ACTION_QUICK_EXPENSE) {
            showQuickExpenseTrigger = true
        }
    }

    /**
     * Foreground Volume-Up Double Press.
     *
     * Detects two volume-up presses within 100-450ms while the activity has input focus.
     * Coordinates with QuickExpenseTriggerCoordinator to prevent duplicate triggers
     * with QuickExpenseAccessibilityService.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val now = SystemClock.uptimeMillis()
            if (now - lastVolumeUpTimestamp in 40..600) {
                lastVolumeUpTimestamp = 0L
                if (QuickExpenseTriggerCoordinator.tryTrigger()) {
                    showQuickExpenseTrigger = true
                    return true
                }
            } else {
                lastVolumeUpTimestamp = now
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}