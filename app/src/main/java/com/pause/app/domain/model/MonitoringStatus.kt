package com.pause.app.domain.model

/** What the main screen shows as the live service state. */
enum class MonitoringStatus {
    /** Enabled, has apps, and all permissions granted — actively watching. */
    RUNNING,

    /** Permissions granted but the user has toggled Pause off (or picked no apps). */
    PAUSED,

    /** A required permission (Accessibility / Display over other apps) is missing. */
    NEEDS_PERMISSION,
}
