package com.pause.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.pause.app.domain.model.AppCatalog
import com.pause.app.domain.model.PauseSettings
import com.pause.app.domain.repository.SettingsRepository
import com.pause.app.overlay.OverlayController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The engine. The framework delivers a "window state changed" event whenever the foreground app
 * changes; we read only its package name. A session is tracked as an absolute deadline
 * (elapsedRealtime when to interrupt), so a brief excursion — a share sheet, a permission dialog,
 * picture-in-picture — within a short grace window does NOT throw away the elapsed time. Truly
 * leaving the app (longer than the grace window, or switching to another real app) resets it.
 *
 * Everything is event-driven — no polling, no foreground service — so the battery cost is nil.
 */
@AndroidEntryPoint
class PauseAccessibilityService : AccessibilityService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var overlayController: OverlayController

    // Main thread so timer callbacks and overlay/window operations are serialized and UI-safe.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile private var settings: PauseSettings = PauseSettings.DEFAULT

    /** The package actually in the foreground now (excluding our own app + transient surfaces). */
    private var currentPackage: String? = null

    /** The monitored app the active session belongs to (null = no session running). */
    private var sessionPackage: String? = null

    /** elapsedRealtime() at which the overlay should fire for the active session. */
    private var sessionDeadline: Long = 0L

    private var countdownJob: Job? = null
    private var leaveJob: Job? = null
    private var overlayShowing = false

    /** Transient windows (keyboard, status-bar shade) that should NOT count as leaving the app. */
    private val ignoredPackages: Set<String> by lazy { buildIgnoredPackages() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsRepository.settings
            .onEach { updated ->
                val previous = settings
                settings = updated
                reconcileWithSettings(previous, updated)
            }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in ignoredPackages) return
        onForegroundPackageChanged(pkg)
    }

    private fun onForegroundPackageChanged(pkg: String) {
        if (pkg == currentPackage) return
        currentPackage = pkg

        // Any genuine foreground change tears down a visible overlay.
        dismissOverlay()

        when {
            // Returned to the app the current session is for.
            pkg == sessionPackage -> {
                cancelLeave()
                resumeOrFireSession()
            }
            // Entered a (different) monitored app — start a fresh session.
            isMonitored(pkg) -> startSession(pkg)
            // A transient/other app while a session is alive — start the grace timer before
            // treating it as "left the app". A quick share-sheet / dialog won't reset the timer.
            sessionPackage != null -> scheduleLeave()
        }
    }

    /** Re-evaluate the running session whenever the persisted settings change. */
    private fun reconcileWithSettings(previous: PauseSettings, updated: PauseSettings) {
        if (!updated.isActivelyMonitoring) {
            clearSession()
            dismissOverlay()
            return
        }
        val pkg = currentPackage ?: return
        val intervalChanged = previous.intervalMinutes != updated.intervalMinutes
        val membershipChanged = (pkg in previous.selectedPackages) != (pkg in updated.selectedPackages)
        when {
            // Current app is monitored but has no session yet (just enabled, or just added).
            isMonitored(pkg) && sessionPackage != pkg -> startSession(pkg)
            // Interval changed for the running session — restart it with the new interval.
            sessionPackage == pkg && intervalChanged -> startSession(pkg)
            // Current app was just deselected — stop monitoring it immediately.
            sessionPackage == pkg && membershipChanged && !isMonitored(pkg) -> {
                clearSession()
                dismissOverlay()
            }
        }
    }

    private fun isMonitored(pkg: String): Boolean =
        settings.isActivelyMonitoring && pkg in settings.selectedPackages

    private fun startSession(pkg: String) {
        cancelLeave()
        cancelCountdown()
        sessionPackage = pkg
        sessionDeadline = SystemClock.elapsedRealtime() + settings.intervalMinutes * 60_000L
        armCountdown()
    }

    /** After returning to the session's app: fire now if overdue, otherwise keep counting. */
    private fun resumeOrFireSession() {
        val pkg = sessionPackage ?: return
        if (!isMonitored(pkg)) {
            clearSession()
            return
        }
        if (SystemClock.elapsedRealtime() >= sessionDeadline) {
            showOverlay(pkg)
        } else if (countdownJob?.isActive != true) {
            armCountdown()
        }
    }

    private fun armCountdown() {
        cancelCountdown()
        val pkg = sessionPackage ?: return
        countdownJob = scope.launch {
            var remaining = sessionDeadline - SystemClock.elapsedRealtime()
            while (remaining > 0) {
                delay(remaining)
                remaining = sessionDeadline - SystemClock.elapsedRealtime()
            }
            // Only interrupt if the user is still actually in this app and not already interrupted.
            if (currentPackage == pkg && isMonitored(pkg) && !overlayShowing) {
                showOverlay(pkg)
            }
            // Otherwise the deadline passed while away: resumeOrFireSession() fires it on return.
        }
    }

    private fun showOverlay(pkg: String) {
        // Don't latch the flag if we can't actually draw (e.g. overlay permission was revoked).
        if (!Settings.canDrawOverlays(this)) return
        overlayShowing = true
        overlayController.show(
            appLabel = AppCatalog.labelFor(pkg),
            intervalMinutes = settings.intervalMinutes,
            onContinue = {
                overlayShowing = false
                // Keep gently re-interrupting for as long as the user stays in the app.
                if (currentPackage == pkg && isMonitored(pkg)) {
                    startSession(pkg)
                } else {
                    clearSession()
                }
            },
            onShowFailed = { overlayShowing = false },
        )
    }

    private fun scheduleLeave() {
        cancelLeave()
        leaveJob = scope.launch {
            delay(LEAVE_GRACE_MS)
            // Still away from the session app after the grace window — count it as truly left.
            if (currentPackage != sessionPackage) clearSession()
        }
    }

    private fun clearSession() {
        cancelCountdown()
        cancelLeave()
        sessionPackage = null
        sessionDeadline = 0L
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun cancelLeave() {
        leaveJob?.cancel()
        leaveJob = null
    }

    private fun dismissOverlay() {
        if (overlayShowing) {
            overlayController.dismiss()
            overlayShowing = false
        }
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
        clearSession()
        dismissOverlay()
        scope.cancel()
    }

    private companion object {
        // How long a transient excursion (share sheet, permission dialog, PiP) is tolerated
        // before we treat it as the user actually leaving the monitored app.
        const val LEAVE_GRACE_MS = 1500L
    }
}
