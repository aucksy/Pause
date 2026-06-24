package com.pause.app.domain.model

/**
 * How Pause detects which app is in the foreground.
 *
 * - [USAGE_ACCESS] is the default and the Play-friendly path: it uses Android's documented
 *   "Usage access" (UsageStatsManager) — the API intended for screen-time apps — via a small
 *   always-on foreground service. The permission users grant is the mild "Usage access" toggle.
 * - [ACCESSIBILITY] uses the Accessibility Service for push-based detection (no persistent
 *   notification) but shows a stronger system warning and is subject to Google's accessibility
 *   policy review.
 */
enum class DetectionMode {
    USAGE_ACCESS,
    ACCESSIBILITY,
    ;

    companion object {
        val DEFAULT = USAGE_ACCESS

        fun fromName(name: String?): DetectionMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
