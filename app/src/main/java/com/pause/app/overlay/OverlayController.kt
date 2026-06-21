package com.pause.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pause.app.ui.theme.PauseTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the single full-screen interruption window. It adds a Compose-hosting view to the
 * WindowManager as a TYPE_APPLICATION_OVERLAY (above the app the user is scrolling), wires it
 * up with a lifecycle/savedstate/viewmodel owner, and tears it down again.
 *
 * All window mutations are marshalled onto the main thread, so it is safe to call from the
 * accessibility service's coroutines.
 */
@Singleton
class OverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentView: ComposeView? = null
    private var currentOwner: OverlayLifecycleOwner? = null

    /** Whether an overlay is currently on screen. Only meaningful on the main thread. */
    val isShowing: Boolean get() = currentView != null

    fun show(
        appLabel: String,
        intervalMinutes: Int,
        message: String,
        showImage: Boolean,
        showText: Boolean,
        customImagePath: String?,
        onContinue: () -> Unit,
        onShowFailed: () -> Unit = {},
    ) {
        mainHandler.post {
            if (currentView != null) {
                onShowFailed()
                return@post
            }
            if (!Settings.canDrawOverlays(context)) {
                onShowFailed()
                return@post
            }

            val owner = OverlayLifecycleOwner().also { it.onCreate() }
            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                // Hold the deliberate pause: swallow the Back button while the overlay is up.
                setOnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }
                setContent {
                    PauseTheme(darkTheme = true, dynamicColor = false) {
                        PauseOverlay(
                            appLabel = appLabel,
                            intervalMinutes = intervalMinutes,
                            message = message,
                            showImage = showImage,
                            showText = showText,
                            customImagePath = customImagePath,
                            onFinished = {
                                removeCurrent()
                                onContinue()
                            },
                        )
                    }
                }
            }

            currentOwner = owner
            currentView = view
            runCatching { windowManager.addView(view, buildLayoutParams()) }
                .onFailure {
                    removeCurrent()
                    onShowFailed()
                }
        }
    }

    /** Remove the overlay immediately (used when the user leaves the app or disables Pause). */
    fun dismiss() {
        mainHandler.post { removeCurrent() }
    }

    private fun removeCurrent() {
        currentView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        currentOwner?.onDestroy()
        currentView = null
        currentOwner = null
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            dimAmount = 0.32f
        }

        // Real cross-window blur for the glassmorphism on Android 12+ (silently ignored by
        // devices/users that have window blurs disabled — the Compose scrim still looks right).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            params.blurBehindRadius = (BLUR_RADIUS_DP * context.resources.displayMetrics.density).toInt()
        }
        return params
    }

    private companion object {
        const val BLUR_RADIUS_DP = 36f
    }
}
