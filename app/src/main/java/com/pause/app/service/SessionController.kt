package com.pause.app.service

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.pause.app.domain.model.AppCatalog
import com.pause.app.domain.model.PauseSettings
import com.pause.app.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The detection-agnostic interruption engine. It is fed foreground-app changes (from either the
 * Accessibility service or the Usage-Access monitor service) and the current settings, and owns the
 * timing state machine + overlay.
 *
 * A session is tracked as an absolute deadline (elapsedRealtime when to interrupt), so a brief
 * excursion — a share sheet, a dialog, PiP — within a short grace window does NOT throw away the
 * elapsed time. Truly leaving the app (longer than the grace window, or switching to another real
 * app) resets it. All calls must come from [scope]'s (main) thread.
 */
class SessionController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val overlayController: OverlayController,
    private val ownPackage: String,
    private val ignoredPackages: Set<String>,
) {
    @Volatile private var settings: PauseSettings = PauseSettings.DEFAULT
    private var currentPackage: String? = null
    private var sessionPackage: String? = null
    private var sessionDeadline: Long = 0L
    private var countdownJob: Job? = null
    private var leaveJob: Job? = null
    private var overlayShowing = false

    /** Re-evaluate the running session whenever the persisted settings change. */
    fun onSettings(updated: PauseSettings) {
        val previous = settings
        settings = updated
        if (!updated.isActivelyMonitoring) {
            clearSession()
            dismissOverlay()
            return
        }
        val pkg = currentPackage ?: return
        val intervalChanged = previous.intervalMinutes != updated.intervalMinutes
        val membershipChanged = (pkg in previous.selectedPackages) != (pkg in updated.selectedPackages)
        when {
            isMonitored(pkg) && sessionPackage != pkg -> startSession(pkg)
            sessionPackage == pkg && intervalChanged -> startSession(pkg)
            sessionPackage == pkg && membershipChanged && !isMonitored(pkg) -> {
                clearSession()
                dismissOverlay()
            }
        }
    }

    fun onForegroundChanged(pkg: String) {
        // Ignore our own windows (the overlay) and transient surfaces (keyboard, status-bar shade).
        if (pkg == ownPackage || pkg in ignoredPackages) return
        if (pkg == currentPackage) return
        currentPackage = pkg

        dismissOverlay()

        when {
            pkg == sessionPackage -> {
                cancelLeave()
                resumeOrFireSession()
            }
            isMonitored(pkg) -> startSession(pkg)
            sessionPackage != null -> scheduleLeave()
        }
    }

    /** Tear everything down (the feeding service is stopping or the mode changed away). */
    fun stop() {
        clearSession()
        dismissOverlay()
        currentPackage = null
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
            if (currentPackage == pkg && isMonitored(pkg) && !overlayShowing) {
                showOverlay(pkg)
            }
        }
    }

    private fun showOverlay(pkg: String) {
        if (!Settings.canDrawOverlays(context)) return
        overlayShowing = true
        val current = settings
        overlayController.show(
            appLabel = AppCatalog.labelFor(pkg),
            intervalMinutes = current.intervalMinutes,
            message = current.overlayMessage,
            showImage = current.showImage,
            showText = current.showText,
            customImagePath = current.customImagePath,
            onContinue = {
                overlayShowing = false
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

    private companion object {
        const val LEAVE_GRACE_MS = 1500L
    }
}
