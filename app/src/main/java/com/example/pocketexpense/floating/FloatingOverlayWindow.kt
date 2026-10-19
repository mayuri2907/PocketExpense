package com.example.pocketexpense.floating

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class FloatingOverlayWindow(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var isShowing = false

    fun show(item: PendingExpenseItem) {
        mainHandler.post {
            try {
                if (!OverlayPermissionHelper.canDrawOverlays(context)) {
                    // Cannot display overlay without permission
                    return@post
                }

                if (isShowing) {
                    dismissInternal()
                }

                val overlayLifecycle = OverlayLifecycleOwner()
                lifecycleOwner = overlayLifecycle

                val view = ComposeView(context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setViewTreeLifecycleOwner(overlayLifecycle)
                    setViewTreeSavedStateRegistryOwner(overlayLifecycle)
                    setViewTreeViewModelStoreOwner(overlayLifecycle)

                    setContent {
                        FloatingExpensePopupContent(
                            item = item,
                            totalCountdownMillis = 3000L
                        )
                    }
                }

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    val density = context.resources.displayMetrics.density
                    y = (48 * density).toInt() // Positioned safely below status bar and camera punch-hole across all screen densities
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    }
                }

                composeView = view
                windowManager.addView(view, params)
                isShowing = true
            } catch (e: Exception) {
                // If overlay fails (e.g. permission revoked in background), safely dismiss
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
        if (!isShowing) return
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
            // View may already have detached
        } finally {
            composeView = null
            lifecycleOwner?.destroy()
            lifecycleOwner = null
            isShowing = false
        }
    }
}
