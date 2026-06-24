package com.pause.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.pause.app.domain.model.DetectionMode
import com.pause.app.domain.model.PauseSettings
import com.pause.app.domain.repository.SettingsRepository
import com.pause.app.overlay.OverlayController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Accessibility-based foreground detection: the framework pushes a "window state changed" event
 * whenever the foreground app changes; we read only its package name and forward it to the shared
 * [SessionController]. This path produces no persistent notification.
 *
 * It only drives the engine when the user's chosen [DetectionMode] is [DetectionMode.ACCESSIBILITY]
 * — if the user picked Usage Access, this service stays bound (if enabled in system settings) but
 * does nothing.
 */
@AndroidEntryPoint
class PauseAccessibilityService : AccessibilityService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var overlayController: OverlayController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: SessionController

    @Volatile private var active = false

    private val ignoredPackages: Set<String> by lazy { buildIgnoredPackages() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        controller = SessionController(
            context = applicationContext,
            scope = scope,
            overlayController = overlayController,
            ownPackage = packageName,
            ignoredPackages = ignoredPackages,
        )
        settingsRepository.settings
            .onEach { updated ->
                val isAccessibilityMode = updated.detectionMode == DetectionMode.ACCESSIBILITY
                if (isAccessibilityMode) {
                    active = true
                    controller.onSettings(updated)
                } else if (active) {
                    // User switched to Usage Access — stand down.
                    active = false
                    controller.stop()
                }
            }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!active) return
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        controller.onForegroundChanged(pkg)
    }

    private fun buildIgnoredPackages(): Set<String> {
        val ignored = mutableSetOf("com.android.systemui")
        runCatching {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.enabledInputMethodList.forEach { info -> info.packageName?.let(ignored::add) }
        }
        return ignored
    }

    override fun onInterrupt() {
        // No spoken/audible feedback to interrupt — required override, intentionally empty.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        if (::controller.isInitialized) controller.stop()
        scope.cancel()
    }
}
