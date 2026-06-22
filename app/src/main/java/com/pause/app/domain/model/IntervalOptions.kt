package com.pause.app.domain.model

import com.pause.app.core.Constants

/** The interruption intervals offered in onboarding and on the main screen. */
object IntervalOptions {
    /** The quick-pick presets. The user can also enter any custom value in [MIN_MINUTES]..[MAX_MINUTES]. */
    val minutes: List<Int> = listOf(5, 10, 15, 20, 30)

    const val DEFAULT: Int = Constants.DEFAULT_INTERVAL_MINUTES
    const val MIN_MINUTES: Int = 1
    const val MAX_MINUTES: Int = 180

    /** Keep a stored/entered value within the supported range (custom values are preserved, not snapped). */
    fun sanitize(value: Int): Int = value.coerceIn(MIN_MINUTES, MAX_MINUTES)

    /** Whether a value is one of the quick-pick presets (vs a custom entry). */
    fun isPreset(value: Int): Boolean = value in minutes
}
