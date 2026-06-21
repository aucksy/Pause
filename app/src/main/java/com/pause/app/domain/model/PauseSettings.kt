package com.pause.app.domain.model

import com.pause.app.core.Constants

/** The complete, persisted user configuration — the single source of truth for the service. */
data class PauseSettings(
    val selectedPackages: Set<String>,
    val intervalMinutes: Int,
    val isEnabled: Boolean,
    val onboardingComplete: Boolean,
) {
    /** Monitoring can only actually do anything when it's on AND at least one app is chosen. */
    val isActivelyMonitoring: Boolean get() = isEnabled && selectedPackages.isNotEmpty()

    companion object {
        val DEFAULT = PauseSettings(
            selectedPackages = emptySet(),
            intervalMinutes = Constants.DEFAULT_INTERVAL_MINUTES,
            isEnabled = false,
            onboardingComplete = false,
        )
    }
}
