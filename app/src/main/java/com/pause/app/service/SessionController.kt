package com.pause.app.service

import android.content.Context
import android.os.PowerManager
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
 * The interruption engine. It is fed foreground-app changes (from the Usage-Access monitor service)
 * and the current settings, and owns the timing state machine + overlay.
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
    private var sessionStart: Long = 0L
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
            // App de-selected mid-session — stop watching it.
            sessionPackage == pkg && membershipChanged && !isMonitored(pkg) -> {
                clearSession()
                dismissOverlay()
            }
            // Newly selected (or first seen) monitored app — start its session.
            isMonitored(pkg) && sessionPackage != pkg -> startSession(pkg)
            // Interval changed mid-session — move the deadline relative to the ORIGINAL start so the
            // time already spent still counts (lengthening keeps elapsed time; shortening below it
            // lets armCountdown fire at once). Re-confirming the same interval no longer postpones it.
            sessionPackage == pkg && intervalChanged && isMonitored(pkg) -> {
                sessionDeadline = sessionStart + updated.intervalMinutes * 60_000L
                armCountdown()
            }
        }
    }

    /**
     * A new foreground app was observed. [sinceElapsed] is the [SystemClock.elapsedRealtime] at
     * which it actually came to the front (defaults to now): the poll-based detector passes the
     * real entry time so an app that was already open when monitoring started counts the time
     * already spent in it, instead of restarting the clock from zero.
     */
    fun onForegroundChanged(pkg: String, sinceElapsed: Long = SystemClock.elapsedRealtime()) {
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
            isMonitored(pkg) -> startSession(pkg, sinceElapsed)
            sessionPackage != null -> scheduleLeave()
        }
    }

    /**
     * The foreground app went away with no replacement (the screen turned off or the device
     * locked). In Usage-Access mode the platform emits no new foreground event for this, so without
     * this signal the armed countdown would keep running and fire the overlay over a blank/locked
     * screen. Ending the session here also means returning to the app starts a fresh count.
     */
    fun onForegroundLost() {
        if (currentPackage == null && sessionPackage == null) return
        currentPackage = null
        dismissOverlay()
        clearSession()
    }

    /** Tear everything down (the feeding service is stopping or the mode changed away). */
    fun stop() {
        clearSession()
        dismissOverlay()
        currentPackage = null
    }

    private fun isMonitored(pkg: String): Boolean =
        settings.isActivelyMonitoring && pkg in settings.selectedPackages

    private fun startSession(pkg: String, since: Long = SystemClock.elapsedRealtime()) {
        cancelLeave()
        cancelCountdown()
        sessionPackage = pkg
        // Anchor to when the app actually came to the foreground (so time already spent counts),
        // but never in the future — a skewed/late timestamp must not push the deadline out.
        sessionStart = since.coerceAtMost(SystemClock.elapsedRealtime())
        sessionDeadline = sessionStart + settings.intervalMinutes * 60_000L
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
        // Last line of defence: never raise the overlay while the screen is off or locked. In
        // Usage-Access mode a screen-off "leave" emits no foreground event, so if onForegroundLost
        // was missed the countdown could otherwise fire onto a blank screen. Drop the session so the
        // user gets a fresh count when they come back.
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (power?.isInteractive == false) {
            clearSession()
            return
        }
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
        sessionStart = 0L
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
