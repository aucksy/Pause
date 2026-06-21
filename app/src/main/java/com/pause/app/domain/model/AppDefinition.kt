package com.pause.app.domain.model

/**
 * A monitorable app the user can pick. The accent colour drives the monogram tile shown in
 * the UI so we never depend on the app actually being installed to render something premium.
 */
data class AppDefinition(
    val packageName: String,
    val label: String,
    /** Brand-ish accent as 0xAARRGGBB. */
    val accent: Long,
)
