package com.pause.app.domain.model

import com.pause.app.core.Constants

/** The interruption intervals offered in onboarding and on the main screen. */
object IntervalOptions {
    val minutes: List<Int> = listOf(5, 10, 15, 20, 30)
    const val DEFAULT: Int = Constants.DEFAULT_INTERVAL_MINUTES

    /** Snap an arbitrary stored value to the nearest supported option (defensive). */
    fun sanitize(value: Int): Int = minutes.minByOrNull { kotlin.math.abs(it - value) } ?: DEFAULT
}
