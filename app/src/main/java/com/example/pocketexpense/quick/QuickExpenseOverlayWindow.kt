package com.example.pocketexpense.quick

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.data.repository.ExpenseRepository
import com.example.pocketexpense.floating.OverlayLifecycleOwner
import com.example.pocketexpense.floating.OverlayPermissionHelper
import com.example.pocketexpense.ui.components.QuickExpenseFormCard
import com.example.pocketexpense.ui.theme.PocketExpenseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * System-wide floating overlay window for the Quick Expense flow.
 *
 * Implements:
 * 1. WindowManager TYPE_APPLICATION_OVERLAY with soft-keyboard focus support.
 * 2. Renders the exact same QuickExpenseFormCard component as the in-app dialog.
 * 3. 5-second countdown timer, paused-on-interaction, and draft preservation via QuickExpenseDraftManager.
 * 4. Dismisses cleanly on backdrop click or Back key press.
 * 5. Saves expense directly to Room using ExpenseRepository with source "QUICK_ENTRY".
 */
class QuickExpenseOverlayWindow(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var showing = false

    fun isShowing(): Boolean = showing

    fun show(repository: ExpenseRepository) {
        mainHandler.post {
            try {
                if (!OverlayPermissionHelper.canDrawOverlays(context)) {
                    return@post
                }

                if (showing) {
                    dismissInternal()
                }

                val overlayLifecycle = OverlayLifecycleOwner()
                lifecycleOwner = overlayLifecycle

                // Pre-fetch categories asynchronously while presenting default categories immediately
                var activeCategories = Category.defaultCategories()
                overlayScope.launch {
                    val dbCategories = repository.getAllCategoriesList()
                    if (dbCategories.isNotEmpty()) {
                        activeCategories = dbCategories
                    }
                }

                val view = ComposeView(context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setViewTreeLifecycleOwner(overlayLifecycle)
                    setViewTreeSavedStateRegistryOwner(overlayLifecycle)
                    setViewTreeViewModelStoreOwner(overlayLifecycle)

                    // Intercept system Back key to dismiss overlay and preserve draft
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                            dismissInternal()
                            true
                        } else {
                            false
                        }
                    }

                    setContent {
                        PocketExpenseTheme {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        // Tap outside modal card dismisses overlay safely
                                        dismissInternal()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                QuickExpenseFormCard(
                                    categories = activeCategories,
                                    onSaveExpense = { amountPaise, description, categoryId ->
                                        overlayScope.launch {
                                            val expense = Expense(
                                                title = description.trim().ifBlank { "Quick Expense" },
                                                amountInPaise = amountPaise,
                                                categoryId = categoryId,
                                                timestamp = System.currentTimeMillis(),
                                                note = "Recorded via Quick Expense Overlay",
                                                source = "QUICK_ENTRY"
                                            )
                                            repository.insertExpense(expense)
                                        }
                                        dismissInternal()
                                    },
                                    onDismiss = {
                                        dismissInternal()
                                    },
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        // Stop click propagation inside the card
                                    }
                                )
                            }
                        }
                    }
                }

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                // WindowManager params allowing soft keyboard focus and screen-wide scrim
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                    softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    }
                }

                composeView = view
                windowManager.addView(view, params)
                showing = true
            } catch (e: Exception) {
                dismissInternal()
            }
        }
    }

    fun dismiss() {
        mainHandler.post {
            dismissInternal()
        }
    }

    private fun dismissInternal() {
        if (!showing) return
        try {
            composeView?.let { view ->
                if (view.isAttachedToWindow) {
                    try {
                        windowManager.removeView(view)
                    } catch (_: Exception) {}
                }
                view.disposeComposition()
            }
        } catch (_: Exception) {
            // Ignored if already detached
        } finally {
            composeView = null
            lifecycleOwner?.destroy()
            lifecycleOwner = null
            showing = false
        }
    }
}
