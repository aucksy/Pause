package com.pause.app.domain.model

import com.pause.app.core.Constants

/** The complete, persisted user configuration — the single source of truth for the service. */
data class PauseSettings(
    val selectedPackages: Set<String>,
    val intervalMinutes: Int,
    val isEnabled: Boolean,
    val onboardingComplete: Boolean,
    /** Absolute path to the user's resized custom overlay image, or null to use the default character. */
    val customImagePath: String? = null,
    /** The message shown on the overlay (a preset or the user's own). May be blank. */
    val overlayMessage: String = MessagePresets.default,
    /** Whether the overlay shows the character / custom image. */
    val showImage: Boolean = true,
    /** Whether the overlay shows the text (the message + session caption). */
    val showText: Boolean = true,
    /** How Pause detects the foreground app. */
    val detectionMode: DetectionMode = DetectionMode.DEFAULT,
) {
    /** Monitoring can only actually do anything when it's on AND at least one app is chosen. */
    val isActivelyMonitoring: Boolean get() = isEnabled && selectedPackages.isNotEmpty()

    companion object {
        val DEFAULT = PauseSettings(
            selectedPackages = emptySet(),
            intervalMinutes = Constants.DEFAULT_INTERVAL_MINUTES,
            isEnabled = false,
            onboardingComplete = false,
            customImagePath = null,
            overlayMessage = MessagePresets.default,
            showImage = true,
            showText = true,
            detectionMode = DetectionMode.DEFAULT,
        )
    }
}
